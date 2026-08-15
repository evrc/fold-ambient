package com.example.foldambient.weather

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherMappingTest {
  @Test
  fun weatherDescriptionMapsRepresentativeKnownCodes() {
    assertEquals("Clear", weatherDescriptionForCode(0))
    assertEquals("Fog", weatherDescriptionForCode(48))
    assertEquals("Rain", weatherDescriptionForCode(63))
    assertEquals("Snow showers", weatherDescriptionForCode(86))
    assertEquals("Thunderstorm hail", weatherDescriptionForCode(99))
  }

  @Test
  fun weatherDescriptionFallsBackForUnknownCodes() {
    assertEquals("Weather", weatherDescriptionForCode(-1))
    assertEquals("Weather", weatherDescriptionForCode(1000))
  }

  @Test
  fun weatherConfigurationEnumsFallbackToSafeDefaults() {
    assertEquals(WeatherTemperatureUnit.Celsius, WeatherTemperatureUnit.fromValue("kelvin"))
    assertEquals(WeatherLocationMode.Manual, WeatherLocationMode.fromValue("gps"))
  }
}
