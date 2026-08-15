package com.example.foldambient.lyrics

import com.example.foldambient.media.AmbientMediaSnapshot

data class LyricsTrackQuery(
  val title: String,
  val artist: String,
  val album: String?,
  val durationMillis: Long?,
) {
  val cacheKey: String =
    listOf(
      title.normalizedForLookup(),
      artist.normalizedForLookup(),
      album?.normalizedForLookup().orEmpty(),
      durationMillis?.div(1_000L)?.toString().orEmpty(),
    ).joinToString("|")

  companion object {
    fun from(snapshot: AmbientMediaSnapshot): LyricsTrackQuery? {
      val title = snapshot.title?.takeIf { it.isNotBlank() } ?: return null
      val artist = snapshot.artist?.takeIf { it.isNotBlank() } ?: return null
      return LyricsTrackQuery(
        title = title.cleanedTrackTitle(),
        artist = artist.cleanedArtistName(),
        album = snapshot.album?.takeIf { it.isNotBlank() },
        durationMillis = snapshot.durationMillis,
      )
    }
  }
}

data class AmbientLyrics(
  val trackName: String,
  val artistName: String,
  val albumName: String?,
  val durationMillis: Long?,
  val lines: List<AmbientLyricLine>,
  val source: AmbientLyricsSource,
) {
  val isSynced: Boolean
    get() = source == AmbientLyricsSource.Synced
}

data class AmbientLyricLine(
  val startMillis: Long?,
  val text: String,
)

enum class AmbientLyricsSource {
  Plain,
  Synced,
}

sealed interface AmbientLyricsLookupResult {
  data class Found(val lyrics: AmbientLyrics) : AmbientLyricsLookupResult
  data object NotFound : AmbientLyricsLookupResult
  data class Unavailable(val reason: String? = null) : AmbientLyricsLookupResult
}

interface AmbientLyricsRepository {
  suspend fun lyricsFor(query: LyricsTrackQuery): AmbientLyricsLookupResult
}

fun String.cleanedTrackTitle(): String =
  trim()
    .replace(NoiseInBrackets, "")
    .replace(NoiseAfterDash, "")
    .trim()

fun String.cleanedArtistName(): String =
  trim()
    .replace(FeaturedArtistSuffix, "")
    .trim()

private fun String.normalizedForLookup(): String =
  lowercase()
    .replace(NormalizedSeparators, " ")
    .replace(RepeatedWhitespace, " ")
    .trim()

private val NoiseInBrackets =
  Regex(
    pattern = """\s*[\[(](feat\.?|ft\.?|with|official|video|audio|lyrics|remaster(?:ed)?|live|mono|stereo|radio edit|clean|explicit).*?[\])]""",
    option = RegexOption.IGNORE_CASE,
  )

private val NoiseAfterDash =
  Regex(
    pattern = """\s+-\s+(official.*|remaster(?:ed)?.*|live.*|radio edit|single version|audio|lyrics)$""",
    option = RegexOption.IGNORE_CASE,
  )

private val FeaturedArtistSuffix =
  Regex(
    pattern = """\s+(feat\.?|ft\.?|with)\s+.*$""",
    option = RegexOption.IGNORE_CASE,
  )

private val NormalizedSeparators = Regex("""[^\p{L}\p{Nd}]+""")
private val RepeatedWhitespace = Regex("""\s+""")
