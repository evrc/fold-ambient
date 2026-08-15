package com.example.foldambient.ambient.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.foldambient.ambient.AmbientWidget
import com.example.foldambient.ambient.WidgetConfigurationField
import com.example.foldambient.ambient.WidgetConfigurationFieldType
import com.example.foldambient.ambient.WidgetConfigurationSpec
import com.example.foldambient.ambient.WidgetInstance
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class DigitalClockWidget : AmbientWidget {
  override val type = "clock.digital"
  override val displayName = "Digital Clock"
  override val configurationSpec =
    WidgetConfigurationSpec(
      fields =
        listOf(
          WidgetConfigurationField(
            key = "label",
            label = "Label",
            type = WidgetConfigurationFieldType.Text,
            defaultValue = "Clock",
          ),
          WidgetConfigurationField(
            key = "use24Hour",
            label = "24-hour mode",
            type = WidgetConfigurationFieldType.Boolean,
            defaultValue = "true",
          ),
          WidgetConfigurationField(
            key = "showSeconds",
            label = "Seconds",
            type = WidgetConfigurationFieldType.Boolean,
            defaultValue = "false",
          ),
        ),
    )

  @Composable
  override fun Content(instance: WidgetInstance, modifier: Modifier) {
    val showSeconds = instance.configuration.text("showSeconds", "false").toBoolean()
    val use24Hour = instance.configuration.text("use24Hour", "true").toBoolean()
    var now by remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(showSeconds) {
      while (true) {
        now = LocalTime.now()
        delay(if (showSeconds) 1_000L else 30_000L)
      }
    }

    Column(
      modifier = modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
    ) {
      WidgetLabel(instance.configuration.text("label", displayName))
      WidgetValue(now.format(clockFormatter(use24Hour = use24Hour, showSeconds = showSeconds)))
    }
  }
}

private fun clockFormatter(use24Hour: Boolean, showSeconds: Boolean): DateTimeFormatter =
  DateTimeFormatter.ofPattern(
    when {
      use24Hour && showSeconds -> "HH:mm:ss"
      use24Hour -> "HH:mm"
      showSeconds -> "h:mm:ss a"
      else -> "h:mm a"
    },
  )
