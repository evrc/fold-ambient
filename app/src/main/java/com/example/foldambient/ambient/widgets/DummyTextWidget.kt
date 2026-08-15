package com.example.foldambient.ambient.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.foldambient.ambient.AmbientWidgetRegistry
import com.example.foldambient.ambient.AmbientWidgetTemplate
import com.example.foldambient.ambient.AmbientWidget
import com.example.foldambient.ambient.WidgetConfiguration
import com.example.foldambient.ambient.WidgetInstance

class DummyTextWidget : AmbientWidget {
  override val type = "dummy.text"
  override val displayName = "Dummy Text"

  @Composable
  override fun Content(instance: WidgetInstance, modifier: Modifier) {
    Column(
      modifier = modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        text = instance.configuration.text("label", displayName),
        color = Color(0xFF9CA3AF),
        style = MaterialTheme.typography.titleMedium,
      )
      Text(
        text = instance.configuration.text("value", "Ready"),
        color = Color.White,
        style = MaterialTheme.typography.displaySmall,
      )
    }
  }
}

fun defaultAmbientWidgetRegistry() =
  AmbientWidgetRegistry(
    widgets = listOf(DummyTextWidget()),
    templates =
      listOf(
        dummyTextTemplate(
          id = "status",
          displayName = "Status",
          label = "Fold Ambient",
          value = "Ready",
        ),
        dummyTextTemplate(
          id = "clock",
          displayName = "Clock",
          label = "Clock",
          value = "12:00",
        ),
        dummyTextTemplate(
          id = "date",
          displayName = "Date",
          label = "Today",
          value = "Monday",
        ),
        dummyTextTemplate(
          id = "battery",
          displayName = "Battery",
          label = "Battery",
          value = "100%",
        ),
      ),
  )

private fun dummyTextTemplate(
  id: String,
  displayName: String,
  label: String,
  value: String,
) =
  AmbientWidgetTemplate(
    id = "dummy.$id",
    displayName = displayName,
    widgetType = "dummy.text",
    configuration =
      WidgetConfiguration(
        values =
          mapOf(
            "label" to label,
            "value" to value,
          ),
      ),
  )
