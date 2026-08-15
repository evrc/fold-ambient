package com.example.foldambient.weather

data class WeatherLocationSearchResult(
  val name: String,
  val country: String?,
  val adminArea: String?,
  val latitude: Double,
  val longitude: Double,
) {
  val displayName: String =
    listOf(name, adminArea, country)
      .filter { !it.isNullOrBlank() }
      .distinct()
      .joinToString(", ")
}

interface WeatherGeocodingRepository {
  suspend fun searchLocations(query: String): List<WeatherLocationSearchResult>
}
