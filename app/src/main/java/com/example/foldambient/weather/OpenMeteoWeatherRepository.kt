package com.example.foldambient.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

class OpenMeteoWeatherRepository : AmbientWeatherRepository {
  private var cachedLocationKey: String? = null
  private var cachedResult: AmbientWeatherResult.Available? = null
  private var cachedAtMillis: Long = 0L

  override suspend fun currentWeather(location: WeatherLocation): AmbientWeatherResult =
    withContext(Dispatchers.IO) {
      val now = System.currentTimeMillis()
      val locationKey = location.cacheKey
      if (
        cachedLocationKey == locationKey &&
        cachedResult != null &&
        now - cachedAtMillis < CacheDurationMillis
      ) {
        return@withContext cachedResult ?: AmbientWeatherResult.Unavailable()
      }

      val result =
        runCatching {
          requestWeather(location)
        }.fold(
          onSuccess = { AmbientWeatherResult.Available(it) },
          onFailure = { AmbientWeatherResult.Unavailable(it.message) },
        )

      if (result is AmbientWeatherResult.Available) {
        cachedLocationKey = locationKey
        cachedResult = result
        cachedAtMillis = now
      }
      result
    }

  private fun requestWeather(location: WeatherLocation): AmbientWeather {
    val url =
      URL(
        "$BaseUrl/v1/forecast?${weatherParameters(location).toQueryString()}",
      )
    val connection = (url.openConnection() as HttpURLConnection).apply {
      connectTimeout = 7_000
      readTimeout = 10_000
      requestMethod = "GET"
      setRequestProperty("Accept", "application/json")
      setRequestProperty("User-Agent", UserAgent)
    }

    return try {
      when (val responseCode = connection.responseCode) {
        HttpURLConnection.HTTP_OK ->
          JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            .toAmbientWeather(location)
        else -> throw IOException("Open-Meteo request failed: HTTP $responseCode")
      }
    } finally {
      connection.disconnect()
    }
  }
}

private fun weatherParameters(location: WeatherLocation): Map<String, String> =
  mapOf(
    "latitude" to location.latitude.toString(),
    "longitude" to location.longitude.toString(),
    "current" to "temperature_2m,apparent_temperature,is_day,weather_code,wind_speed_10m",
    "temperature_unit" to location.temperatureUnit.apiValue,
    "wind_speed_unit" to if (location.temperatureUnit == WeatherTemperatureUnit.Fahrenheit) "mph" else "kmh",
    "timezone" to "auto",
  )

private fun JSONObject.toAmbientWeather(location: WeatherLocation): AmbientWeather {
  val current = getJSONObject("current")
  val currentUnits = optJSONObject("current_units")
  val weatherCode = current.optInt("weather_code", UnknownWeatherCode)
  val isDay = current.optInt("is_day", 1) == 1
  return AmbientWeather(
    locationName = location.name,
    temperature = current.getDouble("temperature_2m"),
    apparentTemperature = current.optDoubleOrNull("apparent_temperature"),
    temperatureUnit = location.temperatureUnit,
    condition =
      WeatherCondition(
        code = weatherCode,
        isDay = isDay,
        label = weatherCode.weatherDescription(),
      ),
    windSpeed = current.optDoubleOrNull("wind_speed_10m"),
    windSpeedUnit = currentUnits?.optStringOrNull("wind_speed_10m"),
    observedAt = current.optStringOrNull("time"),
  )
}

private fun Int.weatherDescription(): String =
  when (this) {
    0 -> "Clear"
    1 -> "Mostly clear"
    2 -> "Partly cloudy"
    3 -> "Cloudy"
    45, 48 -> "Fog"
    51, 53, 55 -> "Drizzle"
    56, 57 -> "Freezing drizzle"
    61, 63, 65 -> "Rain"
    66, 67 -> "Freezing rain"
    71, 73, 75 -> "Snow"
    77 -> "Snow grains"
    80, 81, 82 -> "Rain showers"
    85, 86 -> "Snow showers"
    95 -> "Thunderstorm"
    96, 99 -> "Thunderstorm hail"
    else -> "Weather"
  }

private val WeatherLocation.cacheKey: String
  get() =
    listOf(
      latitude.toRoundedKey(),
      longitude.toRoundedKey(),
      temperatureUnit.apiValue,
    ).joinToString("|")

private fun Double.toRoundedKey(): String = String.format(Locale.US, "%.4f", this)

private fun JSONObject.optStringOrNull(key: String): String? =
  if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null

private fun JSONObject.optDoubleOrNull(key: String): Double? =
  if (has(key) && !isNull(key)) optDouble(key) else null

private fun Map<String, String>.toQueryString(): String =
  entries.joinToString("&") { (key, value) ->
    "${key.urlEncode()}=${value.urlEncode()}"
  }

private fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())

private const val BaseUrl = "https://api.open-meteo.com"
private const val UserAgent = "FoldAmbient/1.0 (https://example.com/foldambient)"
private const val CacheDurationMillis = 10 * 60 * 1_000L
private const val UnknownWeatherCode = -1
