package com.example.foldambient.ambient

object DefaultAmbientPages {
  fun createDeck(): AmbientPageDeck {
    val page =
      AmbientPage(
        id = "home",
        title = "Home",
        layout = AmbientLayoutKind.Duo,
        widgets =
          listOf(
            WidgetInstance(
              id = "status",
              widgetType = "dummy.text",
              configuration =
                WidgetConfiguration(
                  values =
                    mapOf(
                      "label" to "Fold Ambient",
                      "value" to "Ready",
                    ),
                ),
            ),
            WidgetInstance(
              id = "placeholder",
              widgetType = "dummy.text",
              configuration =
                WidgetConfiguration(
                  values =
                    mapOf(
                      "label" to "Widget",
                      "value" to "Empty",
                    ),
                ),
            ),
          ),
      )

    return AmbientPageDeck(
      pages = listOf(page),
      selectedPageId = page.id,
    )
  }
}
