package com.example.foldambient.activation

import org.junit.Assert.assertEquals
import org.junit.Test

class AmbientActivationSettingsTest {
  @Test
  fun normalizedClampsHingeAndAspectRatioRanges() {
    val normalized =
      AmbientActivationSettings(
        minHingeAngle = -10f,
        maxHingeAngle = 300f,
        minCoverLandscapeAspectRatio = 9f,
      ).normalized()

    assertEquals(0f, normalized.minHingeAngle)
    assertEquals(180f, normalized.maxHingeAngle)
    assertEquals(3.0f, normalized.minCoverLandscapeAspectRatio)
  }

  @Test
  fun normalizedKeepsAtLeastFiveDegreesBetweenHingeBounds() {
    val normalized =
      AmbientActivationSettings(
        minHingeAngle = 120f,
        maxHingeAngle = 121f,
      ).normalized()

    assertEquals(120f, normalized.minHingeAngle)
    assertEquals(125f, normalized.maxHingeAngle)
  }
}
