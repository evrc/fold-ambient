package com.example.foldambient.weather

import java.util.Locale

data class WeatherLocation(
  val name: String,
  val latitude: Double,
  val longitude: Double,
  val temperatureUnit: WeatherTemperatureUnit,
)

enum class WeatherTemperatureUnit(
  val apiValue: String,
  val symbol: String,
) {
  Celsius(apiValue = "celsius", symbol = "C"),
  Fahrenheit(apiValue = "fahrenheit", symbol = "F");

  companion object {
    fun fromValue(value: String): WeatherTemperatureUnit =
      entries.firstOrNull { it.apiValue == value } ?: Celsius
  }
}

enum class WeatherLocationMode(
  val value: String,
) {
  Manual(value = "manual"),
  Phone(value = "phone");

  companion object {
    fun fromValue(value: String): WeatherLocationMode =
      entries.firstOrNull { it.value == value } ?: Manual
  }
}

data class AmbientWeather(
  val locationName: String,
  val temperature: Double,
  val apparentTemperature: Double?,
  val temperatureUnit: WeatherTemperatureUnit,
  val condition: WeatherCondition,
  val windSpeed: Double?,
  val windSpeedUnit: String?,
  val observedAt: String?,
) {
  val temperatureText: String = "${temperature.roundTemperature()}\u00B0${temperatureUnit.symbol}"
  val apparentTemperatureText: String? =
    apparentTemperature?.let { "${it.roundTemperature()}\u00B0${temperatureUnit.symbol}" }
  val windText: String? =
    if (windSpeed != null && !windSpeedUnit.isNullOrBlank()) {
      "${windSpeed.roundWind()} $windSpeedUnit"
    } else {
      null
    }
}

data class WeatherCondition(
  val code: Int,
  val isDay: Boolean,
  val label: String,
)

sealed interface AmbientWeatherResult {
  data class Available(val weather: AmbientWeather) : AmbientWeatherResult
  data class Unavailable(val reason: String? = null) : AmbientWeatherResult
}

interface AmbientWeatherRepository {
  suspend fun currentWeather(location: WeatherLocation): AmbientWeatherResult
}

private fun Double.roundTemperature(): String = String.format(Locale.US, "%.0f", this)

private fun Double.roundWind(): String = String.format(Locale.US, "%.0f", this)
