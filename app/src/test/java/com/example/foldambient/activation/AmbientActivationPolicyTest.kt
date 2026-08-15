package com.example.foldambient.activation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientActivationPolicyTest {
  @Test
  fun coverLandscapeWithPartialHinge_isAmbientCandidate() {
    val evaluation =
      AmbientActivationPolicy.evaluate(
        settings = AmbientActivationSettings(isEnabled = true),
        signals =
          AmbientActivationSignals(
            windowWidthPx = 2376,
            windowHeightPx = 968,
            hingeSensorAvailable = true,
            hingeAngleDegrees = 90f,
          ),
      )

    assertTrue(evaluation.isCoverLikeLandscape)
    assertTrue(evaluation.isHingeInRange)
    assertTrue(evaluation.shouldActivate)
  }

  @Test
  fun coverPortraitWithSameHinge_isNotAmbientCandidate() {
    val evaluation =
      AmbientActivationPolicy.evaluate(
        settings = AmbientActivationSettings(isEnabled = true),
        signals =
          AmbientActivationSignals(
            windowWidthPx = 968,
            windowHeightPx = 2376,
            hingeSensorAvailable = true,
            hingeAngleDegrees = 90f,
          ),
      )

    assertFalse(evaluation.isCoverLikeLandscape)
    assertFalse(evaluation.shouldActivate)
  }

  @Test
  fun innerDisplayWithSameHinge_isNotCoverAmbientCandidate() {
    val evaluation =
      AmbientActivationPolicy.evaluate(
        settings = AmbientActivationSettings(isEnabled = true),
        signals =
          AmbientActivationSignals(
            windowWidthPx = 2160,
            windowHeightPx = 1856,
            hingeSensorAvailable = true,
            hingeAngleDegrees = 90f,
          ),
      )

    assertFalse(evaluation.isCoverLikeLandscape)
    assertFalse(evaluation.shouldActivate)
  }

  @Test
  fun aspectRatioAtBoundary_isCoverLikeLandscape() {
    val evaluation =
      AmbientActivationPolicy.evaluate(
        settings =
          AmbientActivationSettings(
            isEnabled = true,
            minCoverLandscapeAspectRatio = 1.8f,
          ),
        signals =
          AmbientActivationSignals(
            windowWidthPx = 1800,
            windowHeightPx = 1000,
            hingeSensorAvailable = true,
            hingeAngleDegrees = 90f,
          ),
      )

    assertTrue(evaluation.isCoverLikeLandscape)
    assertTrue(evaluation.shouldActivate)
  }

  @Test
  fun hingeAngleOutsideConfiguredRange_blocksActivation() {
    val evaluation =
      AmbientActivationPolicy.evaluate(
        settings =
          AmbientActivationSettings(
            isEnabled = true,
            minHingeAngle = 65f,
            maxHingeAngle = 125f,
          ),
        signals =
          AmbientActivationSignals(
            windowWidthPx = 2376,
            windowHeightPx = 968,
            hingeSensorAvailable = true,
            hingeAngleDegrees = 20f,
          ),
      )

    assertFalse(evaluation.isHingeInRange)
    assertFalse(evaluation.shouldActivate)
  }

  @Test
  fun missingHingeSensor_blocksActivation() {
    val evaluation =
      AmbientActivationPolicy.evaluate(
        settings = AmbientActivationSettings(isEnabled = true),
        signals =
          AmbientActivationSignals(
            windowWidthPx = 2376,
            windowHeightPx = 968,
            hingeSensorAvailable = false,
            hingeAngleDegrees = 90f,
          ),
      )

    assertFalse(evaluation.isHingeInRange)
    assertFalse(evaluation.shouldActivate)
  }

  @Test
  fun requiredCharging_blocksActivationWhenNotCharging() {
    val evaluation =
      AmbientActivationPolicy.evaluate(
        settings = AmbientActivationSettings(isEnabled = true, requireCharging = true),
        signals =
          AmbientActivationSignals(
            windowWidthPx = 2376,
            windowHeightPx = 968,
            hingeSensorAvailable = true,
            hingeAngleDegrees = 90f,
            isCharging = false,
          ),
      )

    assertFalse(evaluation.isChargingSatisfied)
    assertFalse(evaluation.shouldActivate)
  }

  @Test
  fun activationDisabled_blocksOtherwiseMatchingSignals() {
    val evaluation =
      AmbientActivationPolicy.evaluate(
        settings = AmbientActivationSettings(isEnabled = false),
        signals =
          AmbientActivationSignals(
            windowWidthPx = 2376,
            windowHeightPx = 968,
            hingeSensorAvailable = true,
            hingeAngleDegrees = 90f,
          ),
      )

    assertTrue(evaluation.isCoverLikeLandscape)
    assertTrue(evaluation.isHingeInRange)
    assertFalse(evaluation.shouldActivate)
  }
}
