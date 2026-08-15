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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DateWidget : AmbientWidget {
  override val type = "date.today"
  override val displayName = "Date"
  override val configurationSpec =
    WidgetConfigurationSpec(
      fields =
        listOf(
          WidgetConfigurationField(
            key = "label",
            label = "Label",
            type = WidgetConfigurationFieldType.Text,
            defaultValue = "Today",
          ),
          WidgetConfigurationField(
            key = "showYear",
            label = "Year",
            type = WidgetConfigurationFieldType.Boolean,
            defaultValue = "false",
          ),
        ),
    )

  @Composable
  override fun Content(instance: WidgetInstance, modifier: Modifier) {
    val showYear = instance.configuration.text("showYear", "false").toBoolean()
    var today by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(Unit) {
      while (true) {
        today = LocalDate.now()
        delay(60_000L)
      }
    }

    Column(
      modifier = modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
    ) {
      WidgetLabel(instance.configuration.text("label", today.format(DateTimeFormatter.ofPattern("EEEE"))))
      WidgetValue(today.format(DateTimeFormatter.ofPattern(if (showYear) "MMM d, yyyy" else "MMM d")))
    }
  }
}
