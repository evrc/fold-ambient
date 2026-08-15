package com.example.foldambient.ambient

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientPageDeckOperationsTest {
  @Test
  fun defaultDeckCreatesThreePagesAndSelectsHome() {
    val deck = DefaultAmbientPages.createDeck()

    assertEquals(3, deck.pages.size)
    assertEquals("home", deck.selectedPageId)
    assertEquals(AmbientLayoutKind.Duo, deck.selectedPage?.layout)
  }

  @Test
  fun addPageAfterSelectedInsertsAfterCurrentPageAndSelectsNewPage() {
    val deck = DefaultAmbientPages.createDeck().copy(selectedPageId = "daily")

    val updated = deck.addPageAfterSelected()

    assertEquals(4, updated.pages.size)
    assertEquals("daily", updated.pages[1].id)
    assertEquals(updated.selectedPageId, updated.pages[2].id)
    assertEquals("Page 4", updated.pages[2].title)
  }

  @Test
  fun deleteSelectedPageSelectsNextAvailablePage() {
    val deck = DefaultAmbientPages.createDeck().copy(selectedPageId = "daily")

    val updated = deck.deleteSelectedPage()

    assertEquals(2, updated.pages.size)
    assertEquals("focus", updated.selectedPageId)
    assertTrue(updated.pages.none { it.id == "daily" })
  }

  @Test
  fun deletingOnlyPageReturnsSameDeck() {
    val deck =
      AmbientPageDeck(
        pages =
          listOf(
            AmbientPage(
              id = "only",
              title = "Only",
              layout = AmbientLayoutKind.Full,
              widgets = emptyList(),
            ),
          ),
        selectedPageId = "only",
      )

    assertSame(deck, deck.deleteSelectedPage())
  }

  @Test
  fun moveSelectedPageByReordersPagesAndKeepsSelection() {
    val deck = DefaultAmbientPages.createDeck().copy(selectedPageId = "focus")

    val updated = deck.moveSelectedPageBy(-2)

    assertEquals(listOf("focus", "home", "daily"), updated.pages.map { it.id })
    assertEquals("focus", updated.selectedPageId)
  }

  @Test
  fun changingLayoutPadsMissingSlotsWithEmptyWidgets() {
    val deck = DefaultAmbientPages.createDeck()

    val updated = deck.updateSelectedPageLayout(AmbientLayoutKind.Quad)

    val selectedPage = updated.selectedPage!!
    assertEquals(AmbientLayoutKind.Quad, selectedPage.layout)
    assertEquals(4, selectedPage.widgets.size)
    assertEquals("clock", selectedPage.widgets[0].id)
    assertEquals("analog", selectedPage.widgets[1].id)
    assertEquals("dummy.text", selectedPage.widgets[2].widgetType)
    assertEquals("Slot 3", selectedPage.widgets[2].configuration.text("label", ""))
  }

  @Test
  fun replacingWidgetPreservesTemplateConfigurationAndAppearance() {
    val deck = DefaultAmbientPages.createDeck()
    val template =
      AmbientWidgetTemplate(
        id = "text",
        displayName = "Text",
        widgetType = "dummy.text",
        configuration = WidgetConfiguration(mapOf("label" to "Custom", "value" to "Ready")),
        appearance = WidgetAppearance(accentColor = 0xFF00FF00),
      )

    val updated = deck.replaceSelectedPageWidget(slotIndex = 1, template = template)
    val replaced = updated.selectedPage!!.widgets[1]

    assertEquals("dummy.text", replaced.widgetType)
    assertEquals("Custom", replaced.configuration.text("label", ""))
    assertEquals("Ready", replaced.configuration.text("value", ""))
    assertEquals(0xFF00FF00, replaced.appearance.accentColor)
  }

  @Test
  fun updatingWidgetConfigurationMergesValues() {
    val deck = DefaultAmbientPages.createDeck()

    val updated =
      deck.updateSelectedWidgetConfiguration(
        slotIndex = 0,
        values = mapOf("label" to "Bedside", "showSeconds" to "true"),
      )

    val widget = updated.selectedPage!!.widgets[0]
    assertEquals("Bedside", widget.configuration.text("label", ""))
    assertEquals("true", widget.configuration.text("showSeconds", "false"))
    assertEquals("classic", widget.configuration.text("style", ""))
  }

  @Test
  fun swappingDuoSlotsReordersWidgets() {
    val deck = DefaultAmbientPages.createDeck()

    val updated = deck.swapSelectedPageSlots(0)

    assertEquals("analog", updated.selectedPage!!.widgets[0].id)
    assertEquals("clock", updated.selectedPage!!.widgets[1].id)
  }
}
