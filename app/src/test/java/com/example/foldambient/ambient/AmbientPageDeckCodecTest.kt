package com.example.foldambient.ambient

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientPageDeckCodecTest {
  @Test
  fun currentPageDeckRoundTripsLayoutWidgetConfigurationAppearanceAndSelectedPage() {
    val deck =
      AmbientPageDeck(
        selectedPageId = "second",
        pages =
          listOf(
            AmbientPage(
              id = "first",
              title = "First",
              layout = AmbientLayoutKind.Full,
              widgets = listOf(widget(id = "clock", widgetType = "clock.digital")),
            ),
            AmbientPage(
              id = "second",
              title = "Configured",
              layout = AmbientLayoutKind.Quad,
              widgets =
                listOf(
                  widget(
                    id = "weather",
                    widgetType = "weather.current",
                    values =
                      mapOf(
                        "label" to "Weather",
                        "locationName" to "Paris",
                        "temperatureUnit" to "celsius",
                      ),
                    accentColor = 0xFF7DD3FC,
                  ),
                  widget(id = "media", widgetType = "media.playback"),
                  widget(id = "lyrics", widgetType = "lyrics.current"),
                  widget(id = "empty", widgetType = "empty"),
                ),
            ),
          ),
      )

    val encoded = AmbientPageDeckCodec.encode(deck)
    val decoded = AmbientPageDeckCodec.decode(encoded)

    assertTrue(encoded.contains(""""schemaVersion":${AmbientPageDeckCodec.SCHEMA_VERSION}"""))
    assertEquals(deck.selectedPageId, decoded.selectedPageId)
    assertEquals(AmbientLayoutKind.Quad, decoded.pages[1].layout)
    assertEquals("weather.current", decoded.pages[1].widgets.first().widgetType)
    assertEquals("Paris", decoded.pages[1].widgets.first().configuration.text("locationName", ""))
    assertEquals(0xFF7DD3FC, decoded.pages[1].widgets.first().appearance.accentColor)
  }

  @Test
  fun unversionedLegacyDeckDecodesAndPreservesConfiguration() {
    val decoded =
      AmbientPageDeckCodec.decode(
        """
        {
          "selectedPageId": "legacy",
          "pages": [
            {
              "id": "legacy",
              "title": "Legacy",
              "layout": "Duo",
              "widgets": [
                {
                  "id": "clock",
                  "widgetType": "clock.digital",
                  "configuration": {
                    "label": "Bedside",
                    "style": "standby",
                    "use24Hour": "false"
                  },
                  "appearance": {}
                },
                {
                  "id": "battery",
                  "widgetType": "battery.status",
                  "configuration": { "label": "Power" },
                  "appearance": {}
                }
              ]
            }
          ]
        }
        """.trimIndent(),
      )

    assertEquals("legacy", decoded.selectedPageId)
    assertEquals("Bedside", decoded.pages.first().widgets.first().configuration.text("label", ""))
    assertEquals("standby", decoded.pages.first().widgets.first().configuration.text("style", ""))
    assertEquals("Power", decoded.pages.first().widgets[1].configuration.text("label", ""))
  }

  @Test
  fun malformedWidgetDoesNotDiscardItsPageOrOtherPages() {
    val decoded =
      AmbientPageDeckCodec.decode(
        """
        {
          "schemaVersion": 1,
          "selectedPageId": "page-3",
          "pages": [
            {
              "id": "page-1",
              "title": "One",
              "layout": "Duo",
              "widgets": [
                { "id": "clock", "widgetType": "clock.digital", "configuration": { "label": "Clock" } },
                "not a widget"
              ]
            },
            {
              "id": "page-3",
              "title": "Three",
              "layout": "Full",
              "widgets": [
                { "id": "date", "widgetType": "date.today", "configuration": { "label": "Today" } }
              ]
            }
          ]
        }
        """.trimIndent(),
      )

    assertEquals("page-3", decoded.selectedPageId)
    assertEquals("page-1", decoded.pages.first().id)
    assertEquals("clock.digital", decoded.pages.first().widgets.first().widgetType)
    assertEquals("dummy.text", decoded.pages.first().widgets[1].widgetType)
    assertEquals("page-3", decoded.pages[1].id)
    assertEquals("date.today", decoded.pages[1].widgets.single().widgetType)
  }

  @Test
  fun malformedPageDoesNotDiscardValidPages() {
    val decoded =
      AmbientPageDeckCodec.decode(
        """
        {
          "schemaVersion": 1,
          "selectedPageId": "valid-2",
          "pages": [
            {
              "id": "valid-1",
              "title": "Valid One",
              "layout": "Full",
              "widgets": [
                { "id": "clock", "widgetType": "clock.digital", "configuration": {} }
              ]
            },
            "broken page",
            {
              "id": "valid-2",
              "title": "Valid Two",
              "layout": "Duo",
              "widgets": [
                { "id": "date", "widgetType": "date.today", "configuration": {} },
                { "id": "battery", "widgetType": "battery.status", "configuration": {} }
              ]
            }
          ]
        }
        """.trimIndent(),
      )

    assertEquals("valid-2", decoded.selectedPageId)
    assertEquals("valid-1", decoded.pages.first().id)
    assertEquals(DefaultAmbientPages.createDeck().pages[1], decoded.pages[1])
    assertEquals("valid-2", decoded.pages[2].id)
  }

  @Test
  fun malformedWidgetConfigurationUsesLocalDefaultsOnlyForThatWidget() {
    val decoded =
      AmbientPageDeckCodec.decode(
        """
        {
          "schemaVersion": 1,
          "selectedPageId": "page",
          "pages": [
            {
              "id": "page",
              "title": "Page",
              "layout": "Duo",
              "widgets": [
                { "id": "weather", "widgetType": "weather.current", "configuration": "broken" },
                { "id": "text", "widgetType": "dummy.text", "configuration": { "label": "Text", "value": "Kept" } }
              ]
            }
          ]
        }
        """.trimIndent(),
      )

    assertEquals("weather.current", decoded.pages.first().widgets.first().widgetType)
    assertEquals(emptyMap<String, String>(), decoded.pages.first().widgets.first().configuration.values)
    assertEquals("Kept", decoded.pages.first().widgets[1].configuration.text("value", ""))
  }

  @Test
  fun unknownWidgetTypeFallsBackSafely() {
    val decoded =
      AmbientPageDeckCodec.decode(
        """
        {
          "schemaVersion": 1,
          "selectedPageId": "page",
          "pages": [
            {
              "id": "page",
              "title": "Page",
              "layout": "Full",
              "widgets": [
                { "id": "future", "widgetType": "future.widget", "configuration": { "secret": "kept?" } }
              ]
            }
          ]
        }
        """.trimIndent(),
      )

    val widget = decoded.pages.first().widgets.single()
    assertEquals("future", widget.id)
    assertEquals("dummy.text", widget.widgetType)
    assertEquals("Ready", widget.configuration.text("value", ""))
  }

  @Test
  fun unknownLayoutTypeFallsBackSafelyAndPreservesValidWidgets() {
    val decoded =
      AmbientPageDeckCodec.decode(
        """
        {
          "schemaVersion": 1,
          "selectedPageId": "page",
          "pages": [
            {
              "id": "page",
              "title": "Page",
              "layout": "Panorama",
              "widgets": [
                { "id": "clock", "widgetType": "clock.digital", "configuration": {} }
              ]
            }
          ]
        }
        """.trimIndent(),
      )

    assertEquals(AmbientLayoutKind.Duo, decoded.pages.first().layout)
    assertEquals("clock.digital", decoded.pages.first().widgets.first().widgetType)
    assertEquals(2, decoded.pages.first().widgets.size)
  }

  @Test
  fun selectedPageIndexBelowRangeFallsBackToFirstPage() {
    val decoded =
      AmbientPageDeckCodec.decode(deckWithSelectedIndex(selectedPageIndex = -10))

    assertEquals("one", decoded.selectedPageId)
  }

  @Test
  fun selectedPageIndexAboveRangeFallsBackToLastPage() {
    val decoded =
      AmbientPageDeckCodec.decode(deckWithSelectedIndex(selectedPageIndex = 50))

    assertEquals("two", decoded.selectedPageId)
  }

  @Test
  fun emptyPageCollectionUsesDefaultDeck() {
    val decoded =
      AmbientPageDeckCodec.decode("""{"schemaVersion":1,"selectedPageId":"missing","pages":[]}""")

    assertEquals(DefaultAmbientPages.createDeck(), decoded)
  }

  @Test
  fun incorrectWidgetCountForLayoutIsNormalized() {
    val decoded =
      AmbientPageDeckCodec.decode(
        """
        {
          "schemaVersion": 1,
          "selectedPageId": "full",
          "pages": [
            {
              "id": "full",
              "title": "Full",
              "layout": "Full",
              "widgets": [
                { "id": "clock", "widgetType": "clock.digital", "configuration": {} },
                { "id": "extra", "widgetType": "date.today", "configuration": {} }
              ]
            },
            {
              "id": "quad",
              "title": "Quad",
              "layout": "Quad",
              "widgets": [
                { "id": "battery", "widgetType": "battery.status", "configuration": {} }
              ]
            }
          ]
        }
        """.trimIndent(),
      )

    assertEquals(1, decoded.pages.first().widgets.size)
    assertEquals("clock", decoded.pages.first().widgets.single().id)
    assertEquals(4, decoded.pages[1].widgets.size)
    assertEquals("battery", decoded.pages[1].widgets.first().id)
    assertEquals("dummy.text", decoded.pages[1].widgets.last().widgetType)
  }

  @Test
  fun partiallyMissingFieldsAreRecoveredLocally() {
    val decoded =
      AmbientPageDeckCodec.decode(
        """
        {
          "schemaVersion": 1,
          "pages": [
            {
              "layout": "Full",
              "widgets": [
                { "widgetType": "date.today" }
              ]
            }
          ]
        }
        """.trimIndent(),
      )

    val page = decoded.pages.first()
    assertEquals("recovered-page-1", page.id)
    assertEquals("Page 1", page.title)
    assertEquals("recovered-widget-1-1", page.widgets.single().id)
    assertEquals("date.today", page.widgets.single().widgetType)
  }

  @Test
  fun currentSchemaVersionDecodes() {
    val decoded =
      AmbientPageDeckCodec.decode(
        """
        {
          "schemaVersion": ${AmbientPageDeckCodec.SCHEMA_VERSION},
          "selectedPageId": "page",
          "pages": [
            {
              "id": "page",
              "title": "Page",
              "layout": "Full",
              "widgets": [
                { "id": "empty", "widgetType": "empty", "configuration": {} }
              ]
            }
          ]
        }
        """.trimIndent(),
    )

    assertEquals("page", decoded.selectedPageId)
    assertEquals("empty", decoded.pages.first().widgets.single().widgetType)
  }

  @Test
  fun futureSchemaVersionAttemptsBestEffortRecoveryWithoutCrashing() {
    val logs = mutableListOf<String>()

    val decoded =
      AmbientPageDeckCodec.decode(
        """
        {
          "schemaVersion": 999,
          "selectedPageId": "page",
          "pages": [
            {
              "id": "page",
              "title": "Page",
              "layout": "Full",
              "widgets": [
                { "id": "clock", "widgetType": "clock.digital", "configuration": {} }
              ]
            }
          ]
        }
        """.trimIndent(),
        recoveryLogger = AmbientPageDeckRecoveryLogger(logs::add),
      )

    assertEquals("page", decoded.selectedPageId)
    assertEquals("clock.digital", decoded.pages.first().widgets.single().widgetType)
    assertTrue(logs.any { it.contains("unsupported page deck schema version") })
  }

  @Test
  fun completelyInvalidJsonStillFallsBackToDefaultAtLoadBoundary() {
    val fallback =
      runCatching { AmbientPageDeckCodec.decode("""not json""") }
        .getOrElse { DefaultAmbientPages.createDeck() }

    assertEquals(DefaultAmbientPages.createDeck(), fallback)
  }

  @Test
  fun duplicatePageIdsAreRecovered() {
    val decoded =
      AmbientPageDeckCodec.decode(
        """
        {
          "schemaVersion": 1,
          "selectedPageId": "same",
          "pages": [
            {
              "id": "same",
              "title": "One",
              "layout": "Full",
              "widgets": [
                { "id": "clock", "widgetType": "clock.digital", "configuration": {} }
              ]
            },
            {
              "id": "same",
              "title": "Two",
              "layout": "Full",
              "widgets": [
                { "id": "date", "widgetType": "date.today", "configuration": {} }
              ]
            }
          ]
        }
        """.trimIndent(),
      )

    assertEquals(listOf("same", "recovered-page-2"), decoded.pages.map { it.id })
    assertEquals("same", decoded.selectedPageId)
  }

  private fun deckWithSelectedIndex(selectedPageIndex: Int): String =
    """
    {
      "schemaVersion": 1,
      "selectedPageIndex": $selectedPageIndex,
      "pages": [
        {
          "id": "one",
          "title": "One",
          "layout": "Full",
          "widgets": [
            { "id": "clock", "widgetType": "clock.digital", "configuration": {} }
          ]
        },
        {
          "id": "two",
          "title": "Two",
          "layout": "Full",
          "widgets": [
            { "id": "date", "widgetType": "date.today", "configuration": {} }
          ]
        }
      ]
    }
    """.trimIndent()

  private fun widget(
    id: String,
    widgetType: String,
    values: Map<String, String> = emptyMap(),
    accentColor: Long? = null,
  ): WidgetInstance =
    WidgetInstance(
      id = id,
      widgetType = widgetType,
      configuration = WidgetConfiguration(values = values),
      appearance = WidgetAppearance(accentColor = accentColor),
    )
}
