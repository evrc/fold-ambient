package com.example.foldambient.ambient

import java.util.UUID

fun AmbientPageDeck.replaceSelectedPageWidget(
  slotIndex: Int,
  template: AmbientWidgetTemplate,
): AmbientPageDeck =
  updateSelectedPage { page ->
    page.copy(
      widgets =
        page.slotWidgets().apply {
          if (slotIndex in indices) {
            set(slotIndex, template.toWidgetInstance())
          }
        }.filterNotNull(),
    )
  }

fun AmbientPageDeck.updateSelectedWidgetConfiguration(
  slotIndex: Int,
  field: WidgetConfigurationField,
  value: String,
): AmbientPageDeck =
  updateSelectedWidgetConfiguration(
    slotIndex = slotIndex,
    values = mapOf(field.key to value),
  )

fun AmbientPageDeck.updateSelectedWidgetConfiguration(
  slotIndex: Int,
  values: Map<String, String>,
): AmbientPageDeck =
  updateSelectedPage { page ->
    page.copy(
      widgets =
        page.slotWidgets().apply {
          val widget = getOrNull(slotIndex)
          if (widget != null && slotIndex in indices) {
            set(
              slotIndex,
              widget.copy(
                configuration =
                  WidgetConfiguration(
                    values = widget.configuration.values + values,
                  ),
              ),
            )
          }
        }.filterNotNull(),
    )
  }

fun AmbientPageDeck.updateSelectedPageLayout(layout: AmbientLayoutKind): AmbientPageDeck =
  updateSelectedPage { page ->
    if (page.layout == layout) {
      page
    } else {
      page.copy(
        layout = layout,
        widgets = page.resizedWidgets(layout.slotCount),
      )
    }
  }

fun AmbientPageDeck.swapSelectedPageSlots(slotIndex: Int): AmbientPageDeck =
  updateSelectedPage { page ->
    val otherSlotIndex = if (slotIndex == 0) 1 else 0
    page.copy(
      widgets =
        page.slotWidgets().apply {
          val selected = getOrNull(slotIndex)
          set(slotIndex, getOrNull(otherSlotIndex))
          set(otherSlotIndex, selected)
        }.filterNotNull(),
    )
  }

fun AmbientPageDeck.addPageAfterSelected(): AmbientPageDeck {
  val page = createBlankPage(pageNumber = pages.size + 1)
  val insertIndex = (selectedPageIndex() + 1).coerceIn(0, pages.size)
  return copy(
    pages = pages.toMutableList().apply { add(insertIndex, page) },
    selectedPageId = page.id,
  )
}

fun AmbientPageDeck.deleteSelectedPage(): AmbientPageDeck {
  if (pages.size <= 1) return this
  val selectedIndex = selectedPageIndex()
  val nextPages = pages.toMutableList().apply { removeAt(selectedIndex) }
  val nextSelectedIndex = selectedIndex.coerceAtMost(nextPages.lastIndex)
  return copy(
    pages = nextPages,
    selectedPageId = nextPages[nextSelectedIndex].id,
  )
}

fun AmbientPageDeck.moveSelectedPageBy(offset: Int): AmbientPageDeck {
  val fromIndex = selectedPageIndex()
  val toIndex = (fromIndex + offset).coerceIn(0, pages.lastIndex)
  if (fromIndex == toIndex) return this

  val nextPages =
    pages.toMutableList().apply {
      add(toIndex, removeAt(fromIndex))
    }
  return copy(pages = nextPages)
}

fun AmbientPageDeck.selectedPageIndex(): Int =
  pages.indexOfFirst { it.id == selectedPageId }.takeIf { it >= 0 } ?: 0

fun AmbientPage.slotWidgets(): MutableList<WidgetInstance?> =
  MutableList(layout.slotCount) { index -> widgets.getOrNull(index) }

private fun AmbientPageDeck.updateSelectedPage(transform: (AmbientPage) -> AmbientPage): AmbientPageDeck =
  copy(
    pages =
      pages.map { page ->
        if (page.id == selectedPage?.id) transform(page) else page
      },
  )

private fun AmbientPage.resizedWidgets(slotCount: Int): List<WidgetInstance> =
  List(slotCount) { index ->
    widgets.getOrNull(index) ?: createEmptyWidget(label = "Slot ${index + 1}")
  }

private fun AmbientWidgetTemplate.toWidgetInstance(): WidgetInstance =
  WidgetInstance(
    id = UUID.randomUUID().toString(),
    widgetType = widgetType,
    configuration = configuration,
    appearance = appearance,
  )

private fun createBlankPage(pageNumber: Int): AmbientPage =
  AmbientPage(
    id = UUID.randomUUID().toString(),
    title = "Page $pageNumber",
    layout = AmbientLayoutKind.Duo,
    widgets =
      listOf(
        createEmptyWidget(label = "Page $pageNumber", value = "Ready"),
        createEmptyWidget(label = "Widget"),
      ),
  )

private fun createEmptyWidget(
  label: String,
  value: String = "Empty",
): WidgetInstance =
  WidgetInstance(
    id = UUID.randomUUID().toString(),
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
