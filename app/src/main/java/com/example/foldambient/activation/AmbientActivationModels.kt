package com.example.foldambient.activation

data class AmbientActivationSettings(
  val isEnabled: Boolean = false,
  val requireCharging: Boolean = false,
  val minHingeAngle: Float = 65f,
  val maxHingeAngle: Float = 125f,
  val minCoverLandscapeAspectRatio: Float = 1.8f,
)

data class AmbientActivationSignals(
  val windowWidthPx: Int = 0,
  val windowHeightPx: Int = 0,
  val displayRotation: Int = 0,
  val hingeSensorAvailable: Boolean = false,
  val hingeAngleDegrees: Float? = null,
  val isCharging: Boolean = false,
) {
  val isLandscape: Boolean
    get() = windowWidthPx > windowHeightPx && windowHeightPx > 0

  val aspectRatio: Float
    get() {
      val shortSide = minOf(windowWidthPx, windowHeightPx)
      val longSide = maxOf(windowWidthPx, windowHeightPx)
      return if (shortSide > 0) longSide / shortSide.toFloat() else 0f
    }
}

data class AmbientActivationEvaluation(
  val shouldActivate: Boolean,
  val isCoverLikeLandscape: Boolean,
  val isHingeInRange: Boolean,
  val isChargingSatisfied: Boolean,
)

object AmbientActivationPolicy {
  fun evaluate(
    settings: AmbientActivationSettings,
    signals: AmbientActivationSignals,
  ): AmbientActivationEvaluation {
    val isCoverLikeLandscape =
      signals.isLandscape &&
        signals.aspectRatio >= settings.minCoverLandscapeAspectRatio
    val hingeAngle = signals.hingeAngleDegrees
    val isHingeInRange =
      signals.hingeSensorAvailable &&
        hingeAngle != null &&
        hingeAngle in settings.minHingeAngle..settings.maxHingeAngle
    val isChargingSatisfied = !settings.requireCharging || signals.isCharging

    return AmbientActivationEvaluation(
      shouldActivate =
        settings.isEnabled &&
          isCoverLikeLandscape &&
          isHingeInRange &&
          isChargingSatisfied,
      isCoverLikeLandscape = isCoverLikeLandscape,
      isHingeInRange = isHingeInRange,
      isChargingSatisfied = isChargingSatisfied,
    )
  }
}
