package com.example.foldambient.display

import org.junit.Assert.assertEquals
import org.junit.Test

class AmbientDisplaySettingsTest {
  @Test
  fun normalizedClampsBrightnessAndDurations() {
    val normalized =
      AmbientDisplaySettings(
        activeBrightness = 2f,
        idleBrightness = 0.001f,
        idleDelayMillis = 5_000L,
        pixelShiftActiveIntervalMillis = 1_000L,
        pixelShiftIdleIntervalMillis = 999_000L,
      ).normalized()

    assertEquals(1f, normalized.activeBrightness)
    assertEquals(0.02f, normalized.idleBrightness)
    assertEquals(30_000L, normalized.idleDelayMillis)
    assertEquals(15_000L, normalized.pixelShiftActiveIntervalMillis)
    assertEquals(180_000L, normalized.pixelShiftIdleIntervalMillis)
  }

  @Test
  fun normalizedKeepsIdleBrightnessNoHigherThanActiveBrightness() {
    val normalized =
      AmbientDisplaySettings(
        activeBrightness = 0.18f,
        idleBrightness = 0.8f,
      ).normalized()

    assertEquals(0.18f, normalized.idleBrightness)
  }
}
