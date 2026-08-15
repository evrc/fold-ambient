package com.example.foldambient.display

import android.content.Context

class SharedPreferencesAmbientDisplaySettingsRepository(
  context: Context,
) {
  private val preferences =
    context.getSharedPreferences("ambient_display", Context.MODE_PRIVATE)

  fun load(): AmbientDisplaySettings =
    AmbientDisplaySettings(
      keepScreenOn = preferences.getBoolean(KEY_KEEP_SCREEN_ON, true),
      activeBrightness = preferences.getFloat(KEY_ACTIVE_BRIGHTNESS, 0.18f),
      idleBrightness = preferences.getFloat(KEY_IDLE_BRIGHTNESS, 0.07f),
      idleDelayMillis = preferences.getLong(KEY_IDLE_DELAY_MILLIS, 90_000L),
      pixelShiftEnabled = preferences.getBoolean(KEY_PIXEL_SHIFT_ENABLED, true),
      pixelShiftActiveIntervalMillis =
        preferences.getLong(KEY_PIXEL_SHIFT_ACTIVE_INTERVAL_MILLIS, 45_000L),
      pixelShiftIdleIntervalMillis =
        preferences.getLong(KEY_PIXEL_SHIFT_IDLE_INTERVAL_MILLIS, 25_000L),
    ).normalized()

  fun save(settings: AmbientDisplaySettings) {
    val normalized = settings.normalized()
    preferences.edit()
      .putBoolean(KEY_KEEP_SCREEN_ON, normalized.keepScreenOn)
      .putFloat(KEY_ACTIVE_BRIGHTNESS, normalized.activeBrightness)
      .putFloat(KEY_IDLE_BRIGHTNESS, normalized.idleBrightness)
      .putLong(KEY_IDLE_DELAY_MILLIS, normalized.idleDelayMillis)
      .putBoolean(KEY_PIXEL_SHIFT_ENABLED, normalized.pixelShiftEnabled)
      .putLong(KEY_PIXEL_SHIFT_ACTIVE_INTERVAL_MILLIS, normalized.pixelShiftActiveIntervalMillis)
      .putLong(KEY_PIXEL_SHIFT_IDLE_INTERVAL_MILLIS, normalized.pixelShiftIdleIntervalMillis)
      .apply()
  }
}

private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
private const val KEY_ACTIVE_BRIGHTNESS = "active_brightness"
private const val KEY_IDLE_BRIGHTNESS = "idle_brightness"
private const val KEY_IDLE_DELAY_MILLIS = "idle_delay_millis"
private const val KEY_PIXEL_SHIFT_ENABLED = "pixel_shift_enabled"
private const val KEY_PIXEL_SHIFT_ACTIVE_INTERVAL_MILLIS = "pixel_shift_active_interval_millis"
private const val KEY_PIXEL_SHIFT_IDLE_INTERVAL_MILLIS = "pixel_shift_idle_interval_millis"
