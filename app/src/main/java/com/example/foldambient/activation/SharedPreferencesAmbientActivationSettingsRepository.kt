package com.example.foldambient.activation

import android.content.Context

class SharedPreferencesAmbientActivationSettingsRepository(
  context: Context,
) {
  private val preferences =
    context.getSharedPreferences("ambient_activation", Context.MODE_PRIVATE)

  fun load(): AmbientActivationSettings =
    AmbientActivationSettings(
      isEnabled = preferences.getBoolean(KEY_ENABLED, false),
      requireCharging = preferences.getBoolean(KEY_REQUIRE_CHARGING, false),
      minHingeAngle = preferences.getFloat(KEY_MIN_HINGE, 65f),
      maxHingeAngle = preferences.getFloat(KEY_MAX_HINGE, 125f),
      minCoverLandscapeAspectRatio = preferences.getFloat(KEY_MIN_COVER_ASPECT_RATIO, 1.8f),
    ).normalized()

  fun save(settings: AmbientActivationSettings) {
    val normalized = settings.normalized()
    preferences.edit()
      .putBoolean(KEY_ENABLED, normalized.isEnabled)
      .putBoolean(KEY_REQUIRE_CHARGING, normalized.requireCharging)
      .putFloat(KEY_MIN_HINGE, normalized.minHingeAngle)
      .putFloat(KEY_MAX_HINGE, normalized.maxHingeAngle)
      .putFloat(KEY_MIN_COVER_ASPECT_RATIO, normalized.minCoverLandscapeAspectRatio)
      .apply()
  }
}

fun AmbientActivationSettings.normalized(): AmbientActivationSettings {
  val minHinge = minHingeAngle.coerceIn(0f, 175f)
  val maxHinge = maxHingeAngle.coerceIn(minHinge + 5f, 180f)
  return copy(
    minHingeAngle = minHinge,
    maxHingeAngle = maxHinge,
    minCoverLandscapeAspectRatio = minCoverLandscapeAspectRatio.coerceIn(1.2f, 3.0f),
  )
}

private const val KEY_ENABLED = "enabled"
private const val KEY_REQUIRE_CHARGING = "require_charging"
private const val KEY_MIN_HINGE = "min_hinge"
private const val KEY_MAX_HINGE = "max_hinge"
private const val KEY_MIN_COVER_ASPECT_RATIO = "min_cover_aspect_ratio"
