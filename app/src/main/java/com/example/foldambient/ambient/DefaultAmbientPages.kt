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
              widget(
                id = "clock",
                widgetType = "clock.digital",
                values =
                  mapOf(
                    "label" to "Clock",
                    "use24Hour" to "true",
                  ),
              ),
              widget(
                id = "analog",
                widgetType = "clock.analog",
                values = mapOf("label" to "Clock"),
              ),
            ),
        ),
        AmbientPage(
          id = "daily",
          title = "Daily",
          layout = AmbientLayoutKind.Duo,
          widgets =
            listOf(
              widget(
                id = "date",
                widgetType = "date.today",
              ),
              widget(
                id = "battery",
                widgetType = "battery.status",
                values = mapOf("label" to "Battery"),
              ),
            ),
        ),
        AmbientPage(
          id = "focus",
          title = "Focus",
          layout = AmbientLayoutKind.Duo,
          widgets =
            listOf(
              widget(
                id = "focus",
                widgetType = "dummy.text",
                values =
                  mapOf(
                    "label" to "Focus",
                    "value" to "Calm",
                  ),
              ),
              widget(
                id = "space",
                widgetType = "dummy.text",
                values =
                  mapOf(
                    "label" to "Text",
                    "value" to "Ready",
                  ),
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

private fun widget(
  id: String,
  widgetType: String,
  values: Map<String, String> = emptyMap(),
): WidgetInstance =
  WidgetInstance(
    id = id,
    widgetType = widgetType,
    configuration = WidgetConfiguration(values = values),
  )
