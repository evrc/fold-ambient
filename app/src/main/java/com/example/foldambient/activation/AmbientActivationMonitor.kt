package com.example.foldambient.activation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager

class AmbientActivationMonitor(
  context: Context,
  private val onSignalsChanged: (AmbientActivationSignals) -> Unit,
) : SensorEventListener {
  private val applicationContext = context.applicationContext
  private val sensorManager = applicationContext.getSystemService(SensorManager::class.java)
  private val hingeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)
  private val batteryReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
          updateSignals { copy(isCharging = intent.isCharging()) }
        }
      }
    }
  private var isStarted = false
  private var signals =
    AmbientActivationSignals(
      hingeSensorAvailable = hingeSensor != null,
      isCharging = applicationContext.readChargingState(),
    )

  fun start() {
    if (isStarted) return
    isStarted = true
    hingeSensor?.let { sensor ->
      sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }
    applicationContext.registerReceiver(
      batteryReceiver,
      IntentFilter(Intent.ACTION_BATTERY_CHANGED),
    )
    updateSignals {
      copy(
        hingeSensorAvailable = hingeSensor != null,
        isCharging = applicationContext.readChargingState(),
      )
    }
  }

  fun stop() {
    if (!isStarted) return
    sensorManager.unregisterListener(this)
    applicationContext.unregisterReceiver(batteryReceiver)
    isStarted = false
  }

  fun updateWindowGeometry(
    widthPx: Int,
    heightPx: Int,
    displayRotation: Int,
  ) {
    updateSignals {
      copy(
        windowWidthPx = widthPx,
        windowHeightPx = heightPx,
        displayRotation = displayRotation,
        isCharging = applicationContext.readChargingState(),
      )
    }
  }

  override fun onSensorChanged(event: SensorEvent) {
    if (event.sensor.type == Sensor.TYPE_HINGE_ANGLE) {
      updateSignals { copy(hingeAngleDegrees = event.values.firstOrNull()) }
    }
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

  private fun updateSignals(transform: AmbientActivationSignals.() -> AmbientActivationSignals) {
    val nextSignals = signals.transform()
    if (nextSignals != signals) {
      signals = nextSignals
      onSignalsChanged(nextSignals)
    }
  }
}

private fun Context.readChargingState(): Boolean =
  registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)).isCharging()

private fun Intent?.isCharging(): Boolean {
  val status = this?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
  return status == BatteryManager.BATTERY_STATUS_CHARGING ||
    status == BatteryManager.BATTERY_STATUS_FULL
}
