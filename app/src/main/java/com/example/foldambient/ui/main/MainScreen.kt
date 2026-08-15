package com.example.foldambient.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.foldambient.ambient.AmbientLayoutKind
import com.example.foldambient.ambient.AmbientPage
import com.example.foldambient.ambient.AmbientPageDeck
import com.example.foldambient.ambient.AmbientWidgetRegistry
import com.example.foldambient.ambient.AmbientWidgetTemplate
import com.example.foldambient.ambient.DefaultAmbientPages
import com.example.foldambient.ambient.WidgetConfiguration
import com.example.foldambient.ambient.WidgetInstance
import com.example.foldambient.ambient.ui.AmbientPageRenderer
import com.example.foldambient.ambient.widgets.defaultAmbientWidgetRegistry
import com.example.foldambient.theme.FoldAmbientTheme
import java.util.UUID

@Composable
fun MainScreen(
  isAmbientActive: Boolean,
  pageDeck: AmbientPageDeck,
  widgetRegistry: AmbientWidgetRegistry,
  onPageDeckChange: (AmbientPageDeck) -> Unit,
  onAmbientActiveChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (isAmbientActive) {
    AmbientDashboard(
      pageDeck = pageDeck,
      widgetRegistry = widgetRegistry,
      onPageDeckChange = onPageDeckChange,
      onExit = { onAmbientActiveChange(false) },
      modifier = modifier,
    )
  } else {
    EntryShell(
      onEnter = { onAmbientActiveChange(true) },
      modifier = modifier,
    )
  }
}

@Composable
private fun EntryShell(
  onEnter: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Night)
      .padding(32.dp),
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = "Fold Ambient",
      color = Color.White,
      style = MaterialTheme.typography.headlineMedium,
    )
    Text(
      text = "Ready",
      color = Muted,
      style = MaterialTheme.typography.titleMedium,
    )
    SectionSpacer()
    Button(
      onClick = onEnter,
      colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Night),
    ) {
      Text("Enter ambient")
    }
  }
}

@Composable
private fun AmbientDashboard(
  pageDeck: AmbientPageDeck,
  widgetRegistry: AmbientWidgetRegistry,
  onPageDeckChange: (AmbientPageDeck) -> Unit,
  onExit: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isEditing by remember { mutableStateOf(false) }
  var selectedSlotIndex by remember { mutableIntStateOf(0) }
  var isPickerOpen by remember { mutableStateOf(false) }
  val selectedPageIndex = pageDeck.selectedPageIndex()
  val pagerState =
    rememberPagerState(initialPage = selectedPageIndex) {
      pageDeck.pages.size
    }

  LaunchedEffect(pageDeck.selectedPageId, pageDeck.pages.size) {
    val pageIndex = pageDeck.selectedPageIndex()
    if (pageIndex != pagerState.currentPage) {
      pagerState.scrollToPage(pageIndex)
    }
  }

  LaunchedEffect(pagerState, pageDeck) {
    snapshotFlow { pagerState.settledPage }
      .collect { pageIndex ->
        val pageId = pageDeck.pages.getOrNull(pageIndex)?.id
        if (pageId != null && pageId != pageDeck.selectedPageId) {
          onPageDeckChange(pageDeck.copy(selectedPageId = pageId))
          isPickerOpen = false
          selectedSlotIndex = 0
        }
      }
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black)
      .padding(18.dp),
  ) {
    val windowClass = ambientWindowClass(maxWidth, maxHeight)
    val preferDuo = windowClass == AmbientWindowClass.WideCoverLandscape
    val selectedPage = pageDeck.selectedPage

    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      HorizontalPager(
        state = pagerState,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
      ) { pageIndex ->
        val page = pageDeck.pages[pageIndex]
        AmbientPageRenderer(
          page = page,
          widgetRegistry = widgetRegistry,
          preferDuo = preferDuo,
          isEditing = isEditing && page.id == selectedPage?.id,
          selectedSlotIndex = selectedSlotIndex,
          onSlotLongPress = { slotIndex ->
            selectedSlotIndex = slotIndex
            isEditing = true
            isPickerOpen = false
            if (page.id != pageDeck.selectedPageId) {
              onPageDeckChange(pageDeck.copy(selectedPageId = page.id))
            }
          },
          onSlotClick = { slotIndex ->
            selectedSlotIndex = slotIndex
            isPickerOpen = true
            if (page.id != pageDeck.selectedPageId) {
              onPageDeckChange(pageDeck.copy(selectedPageId = page.id))
            }
          },
          modifier = Modifier.fillMaxSize(),
        )
      }

      if (isEditing && selectedPage != null) {
        PageEditorBar(
          pageTitle = selectedPage.title,
          pageCount = pageDeck.pages.size,
          canSwapWidgets = preferDuo && selectedPage.layout == AmbientLayoutKind.Duo,
          canMovePageLeft = selectedPageIndex > 0,
          canMovePageRight = selectedPageIndex < pageDeck.pages.lastIndex,
          canDeletePage = pageDeck.pages.size > 1,
          onSwapWidgets = {
            onPageDeckChange(pageDeck.swapSelectedPageSlots(selectedSlotIndex))
          },
          onAddPage = {
            onPageDeckChange(pageDeck.addPageAfterSelected())
            selectedSlotIndex = 0
            isPickerOpen = false
          },
          onMovePageLeft = {
            onPageDeckChange(pageDeck.moveSelectedPageBy(-1))
          },
          onMovePageRight = {
            onPageDeckChange(pageDeck.moveSelectedPageBy(1))
          },
          onDeletePage = {
            onPageDeckChange(pageDeck.deleteSelectedPage())
            selectedSlotIndex = 0
            isPickerOpen = false
          },
          onDone = {
            isEditing = false
            isPickerOpen = false
          },
          onExit = onExit,
        )
      }

      if (isEditing && isPickerOpen && selectedPage != null) {
        WidgetPicker(
          widgetRegistry = widgetRegistry,
          onTemplateSelected = { template ->
            onPageDeckChange(pageDeck.replaceSelectedPageWidget(selectedSlotIndex, template))
            isPickerOpen = false
          },
        )
      }
    }
  }
}

@Composable
private fun PageEditorBar(
  pageTitle: String,
  pageCount: Int,
  canSwapWidgets: Boolean,
  canMovePageLeft: Boolean,
  canMovePageRight: Boolean,
  canDeletePage: Boolean,
  onSwapWidgets: () -> Unit,
  onAddPage: () -> Unit,
  onMovePageLeft: () -> Unit,
  onMovePageRight: () -> Unit,
  onDeletePage: () -> Unit,
  onDone: () -> Unit,
  onExit: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = "$pageTitle / $pageCount",
      color = Muted,
      style = MaterialTheme.typography.labelLarge,
    )
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      TextButton(onClick = onDone) {
        Text("Done", color = Color.White)
      }
      TextButton(
        onClick = onSwapWidgets,
        enabled = canSwapWidgets,
      ) {
        Text("Swap", color = if (canSwapWidgets) Color.White else Muted)
      }
      TextButton(onClick = onAddPage) {
        Text("Add", color = Color.White)
      }
      TextButton(
        onClick = onMovePageLeft,
        enabled = canMovePageLeft,
      ) {
        Text("Left", color = if (canMovePageLeft) Color.White else Muted)
      }
      TextButton(
        onClick = onMovePageRight,
        enabled = canMovePageRight,
      ) {
        Text("Right", color = if (canMovePageRight) Color.White else Muted)
      }
      TextButton(
        onClick = onDeletePage,
        enabled = canDeletePage,
      ) {
        Text("Delete", color = if (canDeletePage) Color.White else Muted)
      }
      TextButton(onClick = onExit) {
        Text("Exit", color = Muted)
      }
    }
  }
}

@Composable
private fun WidgetPicker(
  widgetRegistry: AmbientWidgetRegistry,
  onTemplateSelected: (AmbientWidgetTemplate) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    widgetRegistry.templates.forEach { template ->
      WidgetTemplatePreview(
        template = template,
        widgetRegistry = widgetRegistry,
        onClick = { onTemplateSelected(template) },
      )
    }
  }
}

@Composable
private fun WidgetTemplatePreview(
  template: AmbientWidgetTemplate,
  widgetRegistry: AmbientWidgetRegistry,
  onClick: () -> Unit,
) {
  val widget = widgetRegistry.widgetFor(template.previewInstance)
  Column(
    modifier = Modifier
      .size(width = 180.dp, height = 150.dp)
      .border(1.dp, Color(0xFF374151), RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
      .padding(14.dp),
  ) {
    Text(
      text = template.displayName,
      color = Color.White,
      style = MaterialTheme.typography.titleSmall,
    )
    Spacer(modifier = Modifier.height(8.dp))
    if (widget != null) {
      widget.Content(
        instance = template.previewInstance,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}

private fun AmbientPageDeck.replaceSelectedPageWidget(
  slotIndex: Int,
  template: AmbientWidgetTemplate,
): AmbientPageDeck =
  updateSelectedPage { page ->
    page.copy(
      widgets =
        page.slotWidgets().apply {
          set(slotIndex, template.toWidgetInstance())
        }.filterNotNull(),
    )
  }

private fun AmbientPageDeck.swapSelectedPageSlots(slotIndex: Int): AmbientPageDeck =
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

private fun AmbientPageDeck.addPageAfterSelected(): AmbientPageDeck {
  val page = createBlankPage(pageNumber = pages.size + 1)
  val insertIndex = (selectedPageIndex() + 1).coerceIn(0, pages.size)
  return copy(
    pages = pages.toMutableList().apply { add(insertIndex, page) },
    selectedPageId = page.id,
  )
}

private fun AmbientPageDeck.deleteSelectedPage(): AmbientPageDeck {
  if (pages.size <= 1) return this
  val selectedIndex = selectedPageIndex()
  val nextPages = pages.toMutableList().apply { removeAt(selectedIndex) }
  val nextSelectedIndex = selectedIndex.coerceAtMost(nextPages.lastIndex)
  return copy(
    pages = nextPages,
    selectedPageId = nextPages[nextSelectedIndex].id,
  )
}

private fun AmbientPageDeck.moveSelectedPageBy(offset: Int): AmbientPageDeck {
  val fromIndex = selectedPageIndex()
  val toIndex = (fromIndex + offset).coerceIn(0, pages.lastIndex)
  if (fromIndex == toIndex) return this

  val nextPages =
    pages.toMutableList().apply {
      add(toIndex, removeAt(fromIndex))
    }
  return copy(pages = nextPages)
}

private fun AmbientPageDeck.updateSelectedPage(transform: (AmbientPage) -> AmbientPage): AmbientPageDeck =
  copy(
    pages =
      pages.map { page ->
        if (page.id == selectedPage?.id) transform(page) else page
      },
  )

private fun AmbientPageDeck.selectedPageIndex(): Int =
  pages.indexOfFirst { it.id == selectedPageId }.takeIf { it >= 0 } ?: 0

private fun AmbientPage.slotWidgets(): MutableList<WidgetInstance?> =
  MutableList(layout.slotCount) { index -> widgets.getOrNull(index) }

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
        WidgetInstance(
          id = UUID.randomUUID().toString(),
          widgetType = "dummy.text",
          configuration =
            WidgetConfiguration(
              values =
                mapOf(
                  "label" to "Page $pageNumber",
                  "value" to "Ready",
                ),
            ),
        ),
        WidgetInstance(
          id = UUID.randomUUID().toString(),
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

private fun ambientWindowClass(width: Dp, height: Dp): AmbientWindowClass =
  if (width > height * 1.8f) AmbientWindowClass.WideCoverLandscape else AmbientWindowClass.Standard

private enum class AmbientWindowClass {
  Standard,
  WideCoverLandscape,
}

@Composable
private fun SectionSpacer() {
  Spacer(modifier = Modifier.height(18.dp))
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  FoldAmbientTheme {
    MainScreen(
      isAmbientActive = false,
      pageDeck = previewPageDeck,
      widgetRegistry = previewWidgetRegistry,
      onPageDeckChange = {},
      onAmbientActiveChange = {},
    )
  }
}

@Preview(showBackground = true, widthDp = 720, heightDp = 320)
@Composable
fun AmbientDashboardCoverPreview() {
  FoldAmbientTheme {
    AmbientDashboard(
      pageDeck = previewPageDeck,
      widgetRegistry = previewWidgetRegistry,
      onPageDeckChange = {},
      onExit = {},
    )
  }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 560)
@Composable
fun AmbientDashboardStandardPreview() {
  FoldAmbientTheme {
    AmbientDashboard(
      pageDeck = previewPageDeck,
      widgetRegistry = previewWidgetRegistry,
      onPageDeckChange = {},
      onExit = {},
    )
  }
}

private val Night = Color(0xFF05070A)
private val Muted = Color(0xFF9CA3AF)
private val previewPageDeck = DefaultAmbientPages.createDeck()
private val previewWidgetRegistry = defaultAmbientWidgetRegistry()
