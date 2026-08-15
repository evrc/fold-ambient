package com.example.foldambient.ambient

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientPageDeckCodecTest {
  @Test
  fun pageDeckCodecRoundTripsLayoutWidgetConfigurationAndAppearance() {
    val deck =
      AmbientPageDeck(
        selectedPageId = "page",
        pages =
          listOf(
            AmbientPage(
              id = "page",
              title = "Configured",
              layout = AmbientLayoutKind.Quad,
              widgets =
                listOf(
                  WidgetInstance(
                    id = "weather",
                    widgetType = "weather.current",
                    configuration =
                      WidgetConfiguration(
                        mapOf(
                          "label" to "Weather",
                          "locationName" to "Paris",
                          "temperatureUnit" to "celsius",
                        ),
                      ),
                    appearance = WidgetAppearance(accentColor = 0xFF7DD3FC),
                  ),
                ),
            ),
          ),
      )

    val decoded = AmbientPageDeckCodec.decode(AmbientPageDeckCodec.encode(deck))

    assertEquals(deck.selectedPageId, decoded.selectedPageId)
    assertEquals(AmbientLayoutKind.Quad, decoded.pages.single().layout)
    assertEquals("weather.current", decoded.pages.single().widgets.single().widgetType)
    assertEquals("Paris", decoded.pages.single().widgets.single().configuration.text("locationName", ""))
    assertEquals(0xFF7DD3FC, decoded.pages.single().widgets.single().appearance.accentColor)
  }

  @Test
  fun normalizingSinglePageDeckPadsWithDefaultPages() {
    val deck =
      AmbientPageDeck(
        selectedPageId = "single",
        pages =
          listOf(
            AmbientPage(
              id = "single",
              title = "Single",
              layout = AmbientLayoutKind.Full,
              widgets = emptyList(),
            ),
          ),
      )

    val normalized = AmbientPageDeckCodec.normalize(deck)

    assertEquals("single", normalized.selectedPageId)
    assertEquals(3, normalized.pages.size)
    assertEquals("single", normalized.pages.first().id)
  }

  @Test
  fun malformedDeckDecodeCurrentlyFallsBackToDefaultWhenLoadPathCatchesFailure() {
    val fallback =
      runCatching {
        AmbientPageDeckCodec.normalize(AmbientPageDeckCodec.decode("""{"pages":[{"layout":"Unknown"}]}"""))
      }.getOrElse { DefaultAmbientPages.createDeck() }

    assertEquals(DefaultAmbientPages.createDeck(), fallback)
  }

  @Test
  fun selectedPageMissingCurrentlyNormalizesToFirstPage() {
    val deck = DefaultAmbientPages.createDeck().copy(selectedPageId = "missing")

    val normalized = AmbientPageDeckCodec.normalize(deck)

    assertEquals(normalized.pages.first().id, normalized.selectedPageId)
    assertTrue(normalized.pages.none { it.id == "missing" })
  }
}
