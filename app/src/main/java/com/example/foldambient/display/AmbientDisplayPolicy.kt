package com.example.foldambient.display

import android.view.WindowManager

object AmbientDisplayPolicy {
  const val IdleDelayMillis = 90_000L

  fun brightnessFor(
    isAmbientActive: Boolean,
    isIdle: Boolean,
  ): Float =
    when {
      !isAmbientActive -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
      isIdle -> 0.07f
      else -> 0.18f
    }
}
