package com.example.foldambient.display

data class AmbientDisplaySettings(
  val keepScreenOn: Boolean = true,
  val activeBrightness: Float = 0.18f,
  val idleBrightness: Float = 0.07f,
  val idleDelayMillis: Long = 90_000L,
  val pixelShiftEnabled: Boolean = true,
  val pixelShiftActiveIntervalMillis: Long = 45_000L,
  val pixelShiftIdleIntervalMillis: Long = 25_000L,
)

fun AmbientDisplaySettings.normalized(): AmbientDisplaySettings {
  val active = activeBrightness.coerceIn(0.02f, 1f)
  val idle = idleBrightness.coerceIn(0.02f, active)
  return copy(
    activeBrightness = active,
    idleBrightness = idle,
    idleDelayMillis = idleDelayMillis.coerceIn(30_000L, 300_000L),
    pixelShiftActiveIntervalMillis = pixelShiftActiveIntervalMillis.coerceIn(15_000L, 180_000L),
    pixelShiftIdleIntervalMillis = pixelShiftIdleIntervalMillis.coerceIn(15_000L, 180_000L),
  )
}
