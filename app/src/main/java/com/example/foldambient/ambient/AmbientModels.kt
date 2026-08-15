package com.example.foldambient.ambient

data class AmbientPageDeck(
  val pages: List<AmbientPage>,
  val selectedPageId: String,
) {
  val selectedPage: AmbientPage?
    get() = pages.firstOrNull { it.id == selectedPageId } ?: pages.firstOrNull()
}

data class AmbientPage(
  val id: String,
  val title: String,
  val layout: AmbientLayoutKind,
  val widgets: List<WidgetInstance>,
)

enum class AmbientLayoutKind(val slotCount: Int) {
  Full(slotCount = 1),
  Duo(slotCount = 2),
  Quad(slotCount = 4),
}

data class WidgetInstance(
  val id: String,
  val widgetType: String,
  val configuration: WidgetConfiguration = WidgetConfiguration(),
  val appearance: WidgetAppearance = WidgetAppearance(),
)

data class WidgetConfiguration(
  val values: Map<String, String> = emptyMap(),
) {
  fun text(key: String, fallback: String): String = values[key] ?: fallback
}

data class WidgetAppearance(
  val accentColor: Long? = null,
)
