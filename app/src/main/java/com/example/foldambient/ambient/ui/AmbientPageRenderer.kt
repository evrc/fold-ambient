package com.example.foldambient.ambient.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.foldambient.ambient.AmbientLayoutKind
import com.example.foldambient.ambient.AmbientPage
import com.example.foldambient.ambient.AmbientWidgetRegistry
import com.example.foldambient.ambient.WidgetInstance

@Composable
fun AmbientPageRenderer(
  page: AmbientPage,
  widgetRegistry: AmbientWidgetRegistry,
  preferDuo: Boolean,
  modifier: Modifier = Modifier,
) {
  val layout = if (preferDuo) page.layout else AmbientLayoutKind.Full
  when (layout) {
    AmbientLayoutKind.Full ->
      FullLayout(
        widget = page.widgets.firstOrNull(),
        widgetRegistry = widgetRegistry,
        modifier = modifier,
      )
    AmbientLayoutKind.Duo ->
      DuoLayout(
        widgets = page.widgets.take(AmbientLayoutKind.Duo.slotCount),
        widgetRegistry = widgetRegistry,
        modifier = modifier,
      )
  }
}

@Composable
private fun FullLayout(
  widget: WidgetInstance?,
  widgetRegistry: AmbientWidgetRegistry,
  modifier: Modifier = Modifier,
) {
  WidgetSlot(
    widget = widget,
    widgetRegistry = widgetRegistry,
    modifier = modifier,
  )
}

@Composable
private fun DuoLayout(
  widgets: List<WidgetInstance>,
  widgetRegistry: AmbientWidgetRegistry,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    repeat(AmbientLayoutKind.Duo.slotCount) { index ->
      WidgetSlot(
        widget = widgets.getOrNull(index),
        widgetRegistry = widgetRegistry,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun WidgetSlot(
  widget: WidgetInstance?,
  widgetRegistry: AmbientWidgetRegistry,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .fillMaxSize()
        .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(8.dp))
        .padding(28.dp),
  ) {
    val renderer = widget?.let(widgetRegistry::widgetFor)
    if (widget != null && renderer != null) {
      renderer.Content(
        instance = widget,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      EmptySlot()
    }
  }
}

@Composable
private fun EmptySlot() {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = "Widget",
      color = Color(0xFF9CA3AF),
      style = MaterialTheme.typography.titleMedium,
    )
    Text(
      text = "Empty",
      color = Color.White,
      style = MaterialTheme.typography.displaySmall,
    )
  }
}
