package com.example.foldambient.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

class LrcLibLyricsRepository : AmbientLyricsRepository {
  private val successfulMatches = mutableMapOf<String, AmbientLyricsLookupResult.Found>()

  override suspend fun lyricsFor(query: LyricsTrackQuery): AmbientLyricsLookupResult =
    withContext(Dispatchers.IO) {
      successfulMatches[query.cacheKey]?.let { return@withContext it }

      val result =
        runCatching {
          lookupExact(query) ?: lookupBySearch(query) ?: AmbientLyricsLookupResult.NotFound
        }.getOrElse { error ->
          if (error is TrackNotFoundException) {
            AmbientLyricsLookupResult.NotFound
          } else {
            AmbientLyricsLookupResult.Unavailable(error.message)
          }
        }

      if (result is AmbientLyricsLookupResult.Found) {
        successfulMatches[query.cacheKey] = result
      }
      result
    }

  private fun lookupExact(query: LyricsTrackQuery): AmbientLyricsLookupResult.Found? {
    val album = query.album?.takeIf { it.isNotBlank() } ?: return null
    val durationSeconds = query.durationMillis?.toSeconds() ?: return null
    return requestJsonObject(
      endpoint = "/api/get",
      parameters =
        mapOf(
          "track_name" to query.title,
          "artist_name" to query.artist,
          "album_name" to album,
          "duration" to durationSeconds.toString(),
        ),
    )?.toLyricsResult()
  }

  private fun lookupBySearch(query: LyricsTrackQuery): AmbientLyricsLookupResult.Found? {
    val records =
      requestJsonArray(
        endpoint = "/api/search",
        parameters =
          buildMap {
            put("track_name", query.title)
            put("artist_name", query.artist)
            query.album?.takeIf { it.isNotBlank() }?.let { put("album_name", it) }
          },
      )
    return records
      ?.objects()
      ?.maxByOrNull { record -> record.matchScore(query) }
      ?.takeIf { record -> record.matchScore(query) > 0 }
      ?.toLyricsResult()
  }

  private fun requestJsonObject(
    endpoint: String,
    parameters: Map<String, String>,
  ): JSONObject? {
    val body = request(endpoint, parameters) ?: return null
    return JSONObject(body)
  }

  private fun requestJsonArray(
    endpoint: String,
    parameters: Map<String, String>,
  ): JSONArray? {
    val body = request(endpoint, parameters) ?: return null
    return JSONArray(body)
  }

  private fun request(
    endpoint: String,
    parameters: Map<String, String>,
  ): String? {
    val url = URL("$BaseUrl$endpoint?${parameters.toQueryString()}")
    val connection = (url.openConnection() as HttpURLConnection).apply {
      connectTimeout = 7_000
      readTimeout = 10_000
      requestMethod = "GET"
      setRequestProperty("Accept", "application/json")
      setRequestProperty("User-Agent", LrcLibUserAgent)
    }

    return try {
      when (val responseCode = connection.responseCode) {
        HttpURLConnection.HTTP_OK -> connection.inputStream.bufferedReader().use { it.readText() }
        HttpURLConnection.HTTP_NOT_FOUND -> throw TrackNotFoundException()
        HttpTooManyRequests -> throw IOException("LRCLIB rate limit reached")
        else -> throw IOException("LRCLIB request failed: HTTP $responseCode")
      }
    } finally {
      connection.disconnect()
    }
  }
}

private fun JSONObject.toLyricsResult(): AmbientLyricsLookupResult.Found? {
  val syncedLyrics = optStringOrNull("syncedLyrics")
  val plainLyrics = optStringOrNull("plainLyrics")
  val lines =
    when {
      !syncedLyrics.isNullOrBlank() -> syncedLyrics.toSyncedLyricLines()
      !plainLyrics.isNullOrBlank() -> plainLyrics.toPlainLyricLines()
      optBoolean("instrumental", false) ->
        listOf(AmbientLyricLine(startMillis = null, text = "Instrumental"))
      else -> emptyList()
    }
  if (lines.isEmpty()) return null

  return AmbientLyricsLookupResult.Found(
    AmbientLyrics(
      trackName = optStringOrNull("trackName") ?: optStringOrNull("name") ?: "",
      artistName = optStringOrNull("artistName") ?: "",
      albumName = optStringOrNull("albumName"),
      durationMillis = optLongOrNull("duration")?.times(1_000L),
      lines = lines,
      source = if (!syncedLyrics.isNullOrBlank()) AmbientLyricsSource.Synced else AmbientLyricsSource.Plain,
    ),
  )
}

private fun String.toSyncedLyricLines(): List<AmbientLyricLine> =
  lineSequence()
    .flatMap { line ->
      val timestamps = LrcTimestamp.findAll(line).toList()
      val text =
        timestamps.lastOrNull()
          ?.let { match -> line.substring(match.range.last + 1).trim() }
          .orEmpty()
      timestamps.mapNotNull { timestamp ->
        timestamp.toMillis()?.let { millis ->
          AmbientLyricLine(startMillis = millis, text = text)
        }
      }
    }
    .filter { it.text.isNotBlank() }
    .sortedBy { it.startMillis }
    .toList()

private fun String.toPlainLyricLines(): List<AmbientLyricLine> =
  lineSequence()
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .map { AmbientLyricLine(startMillis = null, text = it) }
    .toList()

private fun MatchResult.toMillis(): Long? {
  val minutes = groupValues.getOrNull(1)?.toLongOrNull() ?: return null
  val seconds = groupValues.getOrNull(2)?.toLongOrNull() ?: return null
  val fraction =
    groupValues.getOrNull(3)
      ?.takeIf { it.isNotBlank() }
      ?.padEnd(3, '0')
      ?.take(3)
      ?.toLongOrNull()
      ?: 0L
  return minutes * 60_000L + seconds * 1_000L + fraction
}

private fun JSONObject.matchScore(query: LyricsTrackQuery): Int {
  val title = optStringOrNull("trackName") ?: optStringOrNull("name") ?: return 0
  val artist = optStringOrNull("artistName") ?: return 0
  val durationSeconds = optLongOrNull("duration")
  val queryDurationSeconds = query.durationMillis?.toSeconds()
  var score = 0

  if (title.normalizedMatchKey() == query.title.normalizedMatchKey()) score += 5
  if (artist.normalizedMatchKey() == query.artist.normalizedMatchKey()) score += 4
  if (
    queryDurationSeconds != null &&
    durationSeconds != null &&
    abs(durationSeconds - queryDurationSeconds) <= 3L
  ) {
    score += 4
  }
  if (hasLyrics()) score += 2
  return score
}

private fun JSONObject.hasLyrics(): Boolean =
  !optStringOrNull("syncedLyrics").isNullOrBlank() ||
    !optStringOrNull("plainLyrics").isNullOrBlank() ||
    optBoolean("instrumental", false)

private fun JSONArray.objects(): List<JSONObject> =
  List(length()) { index -> getJSONObject(index) }

private fun JSONObject.optStringOrNull(key: String): String? =
  if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null

private fun JSONObject.optLongOrNull(key: String): Long? =
  if (has(key) && !isNull(key)) optLong(key).takeIf { it > 0L } else null

private fun Long.toSeconds(): Long = this / 1_000L

private fun String.normalizedMatchKey(): String =
  cleanedTrackTitle()
    .cleanedArtistName()
    .lowercase()
    .replace(Regex("""[^\p{L}\p{Nd}]+"""), " ")
    .trim()

private fun Map<String, String>.toQueryString(): String =
  entries.joinToString("&") { (key, value) ->
    "${key.urlEncode()}=${value.urlEncode()}"
  }

private fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())

private class TrackNotFoundException : IOException("Track not found")

private val LrcTimestamp = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
private const val BaseUrl = "https://lrclib.net"
private const val LrcLibUserAgent = "FoldAmbient/1.0 (https://example.com/foldambient)"
private const val HttpTooManyRequests = 429
