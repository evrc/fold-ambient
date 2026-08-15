package com.example.foldambient.ambient

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class AmbientWidgetTemplate(
  val id: String,
  val displayName: String,
  val widgetType: String,
  val configuration: WidgetConfiguration = WidgetConfiguration(),
  val appearance: WidgetAppearance = WidgetAppearance(),
) {
  val previewInstance =
    WidgetInstance(
      id = "preview-$id",
      widgetType = widgetType,
      configuration = configuration,
      appearance = appearance,
    )
}

interface AmbientWidget {
  val type: String
  val displayName: String

  @Composable
  fun Content(instance: WidgetInstance, modifier: Modifier)
}

class AmbientWidgetRegistry(
  widgets: List<AmbientWidget>,
  val templates: List<AmbientWidgetTemplate>,
) {
  private val widgetsByType = widgets.associateBy { it.type }

  fun widgetFor(instance: WidgetInstance): AmbientWidget? = widgetsByType[instance.widgetType]
}
