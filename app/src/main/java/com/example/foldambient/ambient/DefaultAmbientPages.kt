package com.example.foldambient.ambient

object DefaultAmbientPages {
  fun createDeck(): AmbientPageDeck {
    val pages =
      listOf(
        AmbientPage(
          id = "home",
          title = "Home",
          layout = AmbientLayoutKind.Duo,
          widgets =
            listOf(
              dummyTextWidget(
                id = "status",
                label = "Fold Ambient",
                value = "Ready",
              ),
              dummyTextWidget(
                id = "clock",
                label = "Clock",
                value = "12:00",
              ),
            ),
        ),
        AmbientPage(
          id = "daily",
          title = "Daily",
          layout = AmbientLayoutKind.Duo,
          widgets =
            listOf(
              dummyTextWidget(
                id = "date",
                label = "Today",
                value = "Monday",
              ),
              dummyTextWidget(
                id = "battery",
                label = "Battery",
                value = "100%",
              ),
            ),
        ),
        AmbientPage(
          id = "focus",
          title = "Focus",
          layout = AmbientLayoutKind.Duo,
          widgets =
            listOf(
              dummyTextWidget(
                id = "focus",
                label = "Focus",
                value = "Calm",
              ),
              dummyTextWidget(
                id = "space",
                label = "Widget",
                value = "Empty",
              ),
            ),
        ),
      )

    return AmbientPageDeck(
      pages = pages,
      selectedPageId = pages.first().id,
    )
  }
}

private fun dummyTextWidget(
  id: String,
  label: String,
  value: String,
): WidgetInstance =
  WidgetInstance(
    id = id,
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
