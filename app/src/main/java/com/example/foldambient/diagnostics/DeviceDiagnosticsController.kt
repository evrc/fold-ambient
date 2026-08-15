package com.example.foldambient.diagnostics

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetricsCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val DIAGNOSTICS_TAG = "FoldAmbientDiagnostics"

data class DeviceDiagnostics(
  val displayId: String = "Unknown",
  val displayRotation: String = "Unknown",
  val windowWidthPx: Int = 0,
  val windowHeightPx: Int = 0,
  val orientation: String = "Unknown",
  val foldingFeatures: List<FoldingFeatureDiagnostics> = emptyList(),
  val isHingeAngleSensorAvailable: Boolean = false,
  val hingeAngleDegrees: Float? = null,
)

data class FoldingFeatureDiagnostics(
  val state: String,
  val orientation: String,
  val bounds: String,
  val occlusionType: String,
  val isSeparating: Boolean,
)

class DeviceDiagnosticsController(
  private val activity: ComponentActivity,
) : DefaultLifecycleObserver, SensorEventListener {
  private val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
  private val hingeAngleSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)
  private val mutableDiagnostics = MutableStateFlow(
    DeviceDiagnostics(isHingeAngleSensorAvailable = hingeAngleSensor != null),
  )

  val diagnostics: StateFlow<DeviceDiagnostics> = mutableDiagnostics.asStateFlow()

  private var windowLayoutJob: Job? = null

  init {
    activity.lifecycle.addObserver(this)
    refreshDisplayAndWindowMetrics("init")
    logDiagnostics("init", mutableDiagnostics.value)
  }

  override fun onStart(owner: LifecycleOwner) {
    refreshDisplayAndWindowMetrics("onStart")
    startWindowLayoutCollection()
    startHingeAngleSensor()
  }

  override fun onStop(owner: LifecycleOwner) {
    windowLayoutJob?.cancel()
    windowLayoutJob = null
    sensorManager.unregisterListener(this)
    Log.d(DIAGNOSTICS_TAG, "Stopped WindowLayoutInfo and hinge angle observation")
  }

  override fun onDestroy(owner: LifecycleOwner) {
    activity.lifecycle.removeObserver(this)
  }

  override fun onSensorChanged(event: SensorEvent) {
    if (event.sensor.type != Sensor.TYPE_HINGE_ANGLE) return

    val angle = event.values.firstOrNull() ?: return
    mutableDiagnostics.value = mutableDiagnostics.value.copy(hingeAngleDegrees = angle)
    Log.d(DIAGNOSTICS_TAG, "TYPE_HINGE_ANGLE angle=$angle")
  }

  override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    if (sensor.type == Sensor.TYPE_HINGE_ANGLE) {
      Log.d(DIAGNOSTICS_TAG, "TYPE_HINGE_ANGLE accuracy=$accuracy")
    }
  }

  private fun startWindowLayoutCollection() {
    windowLayoutJob?.cancel()
    windowLayoutJob = activity.lifecycleScope.launch {
      WindowInfoTracker.getOrCreate(activity)
        .windowLayoutInfo(activity)
        .collect { layoutInfo ->
          refreshDisplayAndWindowMetrics("WindowLayoutInfo")
          val foldingFeatures = layoutInfo.toFoldingFeatureDiagnostics()
          mutableDiagnostics.value = mutableDiagnostics.value.copy(foldingFeatures = foldingFeatures)
          Log.d(DIAGNOSTICS_TAG, "WindowLayoutInfo foldingFeatures=$foldingFeatures")
        }
    }
  }

  private fun startHingeAngleSensor() {
    val sensor = hingeAngleSensor
    if (sensor == null) {
      Log.d(DIAGNOSTICS_TAG, "TYPE_HINGE_ANGLE unavailable")
      return
    }

    sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    Log.d(DIAGNOSTICS_TAG, "TYPE_HINGE_ANGLE listener registered")
  }

  private fun refreshDisplayAndWindowMetrics(reason: String) {
    val bounds = WindowMetricsCalculator.getOrCreate()
      .computeCurrentWindowMetrics(activity)
      .bounds
    val display = activity.display

    mutableDiagnostics.value = mutableDiagnostics.value.copy(
      displayId = display?.displayId?.toString() ?: "Unavailable",
      displayRotation = display?.rotation?.toDisplayRotation() ?: "Unavailable",
      windowWidthPx = bounds.width(),
      windowHeightPx = bounds.height(),
      orientation = activity.resources.configuration.orientation.toOrientationName(),
    )
    logDiagnostics(reason, mutableDiagnostics.value)
  }
}

private fun WindowLayoutInfo.toFoldingFeatureDiagnostics(): List<FoldingFeatureDiagnostics> =
  displayFeatures
    .filterIsInstance<FoldingFeature>()
    .map { feature ->
      FoldingFeatureDiagnostics(
        state = feature.state.toString(),
        orientation = feature.orientation.toString(),
        bounds = feature.bounds.toDiagnosticString(),
        occlusionType = feature.occlusionType.toString(),
        isSeparating = feature.isSeparating,
      )
    }

private fun Rect.toDiagnosticString(): String =
  "left=$left, top=$top, right=$right, bottom=$bottom, width=${width()}, height=${height()}"

private fun Int.toDisplayRotation(): String =
  when (this) {
    Surface.ROTATION_0 -> "ROTATION_0"
    Surface.ROTATION_90 -> "ROTATION_90"
    Surface.ROTATION_180 -> "ROTATION_180"
    Surface.ROTATION_270 -> "ROTATION_270"
    else -> "Unknown($this)"
  }

private fun Int.toOrientationName(): String =
  when (this) {
    Configuration.ORIENTATION_LANDSCAPE -> "Landscape"
    Configuration.ORIENTATION_PORTRAIT -> "Portrait"
    Configuration.ORIENTATION_UNDEFINED -> "Undefined"
    else -> "Unknown($this)"
  }

private fun logDiagnostics(reason: String, diagnostics: DeviceDiagnostics) {
  Log.d(
    DIAGNOSTICS_TAG,
    "$reason displayId=${diagnostics.displayId}, rotation=${diagnostics.displayRotation}, " +
      "window=${diagnostics.windowWidthPx}x${diagnostics.windowHeightPx}, " +
      "orientation=${diagnostics.orientation}, " +
      "hingeAvailable=${diagnostics.isHingeAngleSensorAvailable}, " +
      "hingeAngle=${diagnostics.hingeAngleDegrees}",
  )
}
