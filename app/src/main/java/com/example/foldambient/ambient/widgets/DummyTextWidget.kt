package com.example.foldambient.ambient.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.foldambient.ambient.AmbientWidget
import com.example.foldambient.ambient.AmbientWidgetRegistry
import com.example.foldambient.ambient.AmbientWidgetTemplate
import com.example.foldambient.ambient.WidgetConfiguration
import com.example.foldambient.ambient.WidgetConfigurationField
import com.example.foldambient.ambient.WidgetConfigurationFieldType
import com.example.foldambient.ambient.WidgetConfigurationSpec
import com.example.foldambient.ambient.WidgetInstance

class DummyTextWidget : AmbientWidget {
  override val type = "dummy.text"
  override val displayName = "Dummy Text"
  override val configurationSpec =
    WidgetConfigurationSpec(
      fields =
        listOf(
          WidgetConfigurationField(
            key = "label",
            label = "Label",
            type = WidgetConfigurationFieldType.Text,
            defaultValue = "Text",
          ),
          WidgetConfigurationField(
            key = "value",
            label = "Value",
            type = WidgetConfigurationFieldType.Text,
            defaultValue = "Ready",
          ),
        ),
    )

  @Composable
  override fun Content(instance: WidgetInstance, modifier: Modifier) {
    Column(
      modifier = modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
    ) {
      WidgetLabel(instance.configuration.text("label", displayName))
      WidgetValue(instance.configuration.text("value", "Ready"))
    }
  }
}

fun defaultAmbientWidgetRegistry() =
  AmbientWidgetRegistry(
    widgets =
      listOf(
        DigitalClockWidget(),
        AnalogClockWidget(),
        DateWidget(),
        BatteryWidget(),
        DummyTextWidget(),
      ),
    templates =
      listOf(
        widgetTemplate(
          id = "clock.digital",
          displayName = "Digital Clock",
          widgetType = "clock.digital",
          values =
            mapOf(
              "label" to "Clock",
              "use24Hour" to "true",
              "showSeconds" to "false",
            ),
        ),
        widgetTemplate(
          id = "clock.analog",
          displayName = "Analog Clock",
          widgetType = "clock.analog",
          values = mapOf("label" to "Clock"),
        ),
        widgetTemplate(
          id = "date",
          displayName = "Date",
          widgetType = "date.today",
        ),
        widgetTemplate(
          id = "battery",
          displayName = "Battery",
          widgetType = "battery.status",
          values = mapOf("label" to "Battery"),
        ),
        widgetTemplate(
          id = "text.simple",
          displayName = "Simple Text",
          widgetType = "dummy.text",
          values =
            mapOf(
              "label" to "Text",
              "value" to "Ready",
            ),
        ),
      ),
  )

private fun widgetTemplate(
  id: String,
  displayName: String,
  widgetType: String,
  values: Map<String, String> = emptyMap(),
) =
  AmbientWidgetTemplate(
    id = id,
    displayName = displayName,
    widgetType = widgetType,
    configuration = WidgetConfiguration(values = values),
  )
