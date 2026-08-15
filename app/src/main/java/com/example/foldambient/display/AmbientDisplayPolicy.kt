package com.example.foldambient.display

import android.view.WindowManager

object AmbientDisplayPolicy {
  fun brightnessFor(
    isAmbientActive: Boolean,
    isIdle: Boolean,
    settings: AmbientDisplaySettings,
  ): Float =
    when {
      !isAmbientActive -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
      isIdle -> settings.idleBrightness
      else -> settings.activeBrightness
    }
}
