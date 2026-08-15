package com.example.foldambient.ambient.widgets

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.foldambient.ambient.AmbientWidget
import com.example.foldambient.ambient.WidgetConfigurationField
import com.example.foldambient.ambient.WidgetConfigurationFieldType
import com.example.foldambient.ambient.WidgetConfigurationSpec
import com.example.foldambient.ambient.WidgetInstance
import kotlinx.coroutines.delay

class BatteryWidget : AmbientWidget {
  override val type = "battery.status"
  override val displayName = "Battery"
  override val configurationSpec =
    WidgetConfigurationSpec(
      fields =
        listOf(
          WidgetConfigurationField(
            key = "label",
            label = "Label",
            type = WidgetConfigurationFieldType.Text,
            defaultValue = "Battery",
          ),
        ),
    )

  @Composable
  override fun Content(instance: WidgetInstance, modifier: Modifier) {
    val context = LocalContext.current
    var batteryState by remember { mutableStateOf(context.readBatteryState()) }

    StartedWidgetEffect(context) {
      while (true) {
        batteryState = context.readBatteryState()
        delay(30_000L)
      }
    }

    BatteryWidgetContent(
      label = instance.configuration.text("label", displayName),
      batteryState = batteryState,
      modifier = modifier,
    )
  }

  @Composable
  override fun PreviewContent(instance: WidgetInstance, modifier: Modifier) {
    BatteryWidgetContent(
      label = instance.configuration.text("label", displayName),
      batteryState = BatteryState(levelText = "82%", statusText = "Charging"),
      modifier = modifier,
    )
  }
}

@Composable
private fun BatteryWidgetContent(
  label: String,
  batteryState: BatteryState,
  modifier: Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
  ) {
    WidgetLabel(label)
    WidgetValue(batteryState.levelText)
    WidgetLabel(batteryState.statusText)
  }
}

private data class BatteryState(
  val levelText: String,
  val statusText: String,
)

private fun Context.readBatteryState(): BatteryState {
  val batteryStatus: Intent? =
    registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
  val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
  val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
  val percentage =
    if (level >= 0 && scale > 0) "${(level * 100 / scale.toFloat()).toInt()}%" else "--%"
  val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
  val isCharging =
    status == BatteryManager.BATTERY_STATUS_CHARGING ||
      status == BatteryManager.BATTERY_STATUS_FULL

  return BatteryState(
    levelText = percentage,
    statusText = if (isCharging) "Charging" else "Battery",
  )
}
