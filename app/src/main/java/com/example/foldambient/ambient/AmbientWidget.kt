package com.example.foldambient.ambient

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface AmbientWidget {
  val type: String
  val displayName: String

  @Composable
  fun Content(instance: WidgetInstance, modifier: Modifier)
}

class AmbientWidgetRegistry(
  widgets: List<AmbientWidget>,
) {
  private val widgetsByType = widgets.associateBy { it.type }

  fun widgetFor(instance: WidgetInstance): AmbientWidget? = widgetsByType[instance.widgetType]
}
