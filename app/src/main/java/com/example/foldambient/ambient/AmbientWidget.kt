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
  val configurationSpec: WidgetConfigurationSpec
    get() = WidgetConfigurationSpec.Empty

  @Composable
  fun Content(instance: WidgetInstance, modifier: Modifier)

  @Composable
  fun PreviewContent(instance: WidgetInstance, modifier: Modifier)
}

data class WidgetConfigurationSpec(
  val fields: List<WidgetConfigurationField>,
) {
  val isEmpty: Boolean
    get() = fields.isEmpty()

  companion object {
    val Empty = WidgetConfigurationSpec(emptyList())
  }
}

data class WidgetConfigurationField(
  val key: String,
  val label: String,
  val type: WidgetConfigurationFieldType,
  val defaultValue: String,
  val options: List<WidgetConfigurationOption> = emptyList(),
)

data class WidgetConfigurationOption(
  val value: String,
  val label: String,
)

enum class WidgetConfigurationFieldType {
  Boolean,
  Location,
  Option,
  Text,
}

class AmbientWidgetRegistry(
  widgets: List<AmbientWidget>,
  val templates: List<AmbientWidgetTemplate>,
) {
  private val widgetsByType = widgets.associateBy { it.type }

  fun widgetFor(instance: WidgetInstance): AmbientWidget? = widgetsByType[instance.widgetType]
}
