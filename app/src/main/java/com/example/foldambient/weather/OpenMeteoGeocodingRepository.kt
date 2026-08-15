package com.example.foldambient.weather

import com.example.foldambient.cache.BoundedLruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class OpenMeteoGeocodingRepository : WeatherGeocodingRepository {
  private val cachedResults =
    BoundedLruCache<String, List<WeatherLocationSearchResult>>(GeocodingSearchCacheCapacity)

  override suspend fun searchLocations(query: String): List<WeatherLocationSearchResult> =
    withContext(Dispatchers.IO) {
      val normalizedQuery = query.trim()
      if (normalizedQuery.length < MinimumSearchLength) return@withContext emptyList()

      cachedResults[normalizedQuery]?.let { return@withContext it }
      val results =
        runCatching { requestSearch(normalizedQuery) }
          .getOrDefault(emptyList())
      cachedResults[normalizedQuery] = results
      results
    }

  private fun requestSearch(query: String): List<WeatherLocationSearchResult> {
    val url =
      URL(
        "$BaseUrl/v1/search?${searchParameters(query).toQueryString()}",
      )
    val connection = (url.openConnection() as HttpURLConnection).apply {
      connectTimeout = 7_000
      readTimeout = 10_000
      requestMethod = "GET"
      setRequestProperty("Accept", "application/json")
      setRequestProperty("User-Agent", UserAgent)
    }

    return try {
      if (connection.responseCode != HttpURLConnection.HTTP_OK) {
        return emptyList()
      }
      val body = connection.inputStream.bufferedReader().use { it.readText() }
      JSONObject(body)
        .optJSONArray("results")
        ?.toSearchResults()
        .orEmpty()
    } finally {
      connection.disconnect()
    }
  }
}

private fun searchParameters(query: String): Map<String, String> =
  mapOf(
    "name" to query,
    "count" to "6",
    "format" to "json",
    "language" to "en",
  )

private fun JSONArray.toSearchResults(): List<WeatherLocationSearchResult> =
  List(length()) { index -> getJSONObject(index) }
    .mapNotNull(JSONObject::toSearchResult)

private fun JSONObject.toSearchResult(): WeatherLocationSearchResult? {
  if (!has("latitude") || !has("longitude")) return null
  return WeatherLocationSearchResult(
    name = optString("name").takeIf { it.isNotBlank() } ?: return null,
    country = optStringOrNull("country"),
    adminArea = optStringOrNull("admin1"),
    latitude = getDouble("latitude"),
    longitude = getDouble("longitude"),
  )
}

private fun JSONObject.optStringOrNull(key: String): String? =
  if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null

private fun Map<String, String>.toQueryString(): String =
  entries.joinToString("&") { (key, value) ->
    "${key.urlEncode()}=${value.urlEncode()}"
  }

private fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())

private const val BaseUrl = "https://geocoding-api.open-meteo.com"
private const val UserAgent = "FoldAmbient/1.0 (https://example.com/foldambient)"
private const val MinimumSearchLength = 2
private const val GeocodingSearchCacheCapacity = 24
