package com.example.foldambient.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
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
import com.example.foldambient.ambient.WidgetConfigurationField
import com.example.foldambient.ambient.WidgetConfigurationFieldType
import com.example.foldambient.ambient.WidgetConfiguration
import com.example.foldambient.ambient.WidgetInstance
import com.example.foldambient.ambient.ui.AmbientPageRenderer
import com.example.foldambient.ambient.widgets.defaultAmbientWidgetRegistry
import com.example.foldambient.theme.FoldAmbientTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs
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
  var isConfigurationOpen by remember { mutableStateOf(false) }
  var isPageIndicatorVisible by remember { mutableStateOf(false) }
  val selectedPageIndex = pageDeck.selectedPageIndex()
  val realPageCount = pageDeck.pages.size
  val virtualPageCount = if (realPageCount > 0) CyclicPagerPageCount else 0
  val pagerState =
    rememberPagerState(initialPage = initialVirtualPage(selectedPageIndex, realPageCount)) {
      virtualPageCount
    }

  LaunchedEffect(pageDeck.selectedPageId, realPageCount) {
    if (realPageCount > 0) {
      val targetPage = nearestVirtualPage(
        currentVirtualPage = pagerState.currentPage,
        targetRealPage = pageDeck.selectedPageIndex(),
        realPageCount = realPageCount,
      )
      if (targetPage != pagerState.currentPage) {
        pagerState.scrollToPage(targetPage)
      }
    }
  }

  LaunchedEffect(pagerState, pageDeck, realPageCount) {
    snapshotFlow { pagerState.settledPage }
      .collect { virtualPageIndex ->
        val pageIndex = realPageIndex(virtualPageIndex, realPageCount)
        val pageId = pageDeck.pages.getOrNull(pageIndex)?.id
        if (pageId != null && pageId != pageDeck.selectedPageId) {
          onPageDeckChange(pageDeck.copy(selectedPageId = pageId))
          isPickerOpen = false
          isConfigurationOpen = false
          selectedSlotIndex = 0
        }
      }
  }

  LaunchedEffect(pagerState) {
    snapshotFlow { pagerState.isScrollInProgress }
      .collectLatest { isScrollInProgress ->
        if (isScrollInProgress) {
          isPageIndicatorVisible = true
        } else {
          delay(PageIndicatorHideDelayMillis)
          isPageIndicatorVisible = false
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
    val selectedWidget = selectedPage?.slotWidgets()?.getOrNull(selectedSlotIndex)
    val selectedWidgetRenderer = selectedWidget?.let(widgetRegistry::widgetFor)
    val canConfigureSelectedWidget =
      selectedWidgetRenderer?.configurationSpec?.isEmpty == false

    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
      ) {
        HorizontalPager(
          state = pagerState,
          modifier = Modifier.fillMaxSize(),
        ) { virtualPageIndex ->
          val page = pageDeck.pages[realPageIndex(virtualPageIndex, realPageCount)]
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
              isConfigurationOpen = false
              if (page.id != pageDeck.selectedPageId) {
                onPageDeckChange(pageDeck.copy(selectedPageId = page.id))
              }
            },
            onSlotClick = { slotIndex ->
              selectedSlotIndex = slotIndex
              isPickerOpen = true
              isConfigurationOpen = false
              if (page.id != pageDeck.selectedPageId) {
                onPageDeckChange(pageDeck.copy(selectedPageId = page.id))
              }
            },
            modifier = Modifier.fillMaxSize(),
          )
        }

        if (isPageIndicatorVisible && realPageCount > 1) {
          PageIndicator(
            pageCount = realPageCount,
            selectedPageIndex = realPageIndex(pagerState.currentPage, realPageCount),
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .padding(bottom = 10.dp),
          )
        }
      }

      if (isEditing && selectedPage != null) {
        PageEditorBar(
          pageTitle = selectedPage.title,
          pageCount = pageDeck.pages.size,
          selectedLayout = selectedPage.layout,
          canSwapWidgets = selectedPage.layout == AmbientLayoutKind.Duo,
          canConfigureWidget = canConfigureSelectedWidget,
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
            isConfigurationOpen = false
          },
          onConfigureWidget = {
            isPickerOpen = false
            isConfigurationOpen = !isConfigurationOpen
          },
          onLayoutSelected = { layout ->
            onPageDeckChange(pageDeck.updateSelectedPageLayout(layout))
            selectedSlotIndex = selectedSlotIndex.coerceAtMost(layout.slotCount - 1)
            isPickerOpen = false
            isConfigurationOpen = false
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
            isConfigurationOpen = false
          },
          onDone = {
            isEditing = false
            isPickerOpen = false
            isConfigurationOpen = false
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
            isConfigurationOpen = false
          },
        )
      }

      if (
        isEditing &&
        isConfigurationOpen &&
        selectedWidget != null &&
        selectedWidgetRenderer != null
      ) {
        WidgetConfigurationPanel(
          widget = selectedWidget,
          widgetName = selectedWidgetRenderer.displayName,
          fields = selectedWidgetRenderer.configurationSpec.fields,
          onFieldChange = { field, value ->
            onPageDeckChange(
              pageDeck.updateSelectedWidgetConfiguration(
                slotIndex = selectedSlotIndex,
                field = field,
                value = value,
              ),
            )
          },
        )
      }
    }
  }
}

@Composable
private fun PageIndicator(
  pageCount: Int,
  selectedPageIndex: Int,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(7.dp),
  ) {
    repeat(pageCount) { pageIndex ->
      val color = if (pageIndex == selectedPageIndex) Color.White else Color(0xFF4B5563)
      Canvas(modifier = Modifier.size(if (pageIndex == selectedPageIndex) 7.dp else 5.dp)) {
        drawCircle(color = color)
      }
    }
  }
}

@Composable
private fun PageEditorBar(
  pageTitle: String,
  pageCount: Int,
  selectedLayout: AmbientLayoutKind,
  canSwapWidgets: Boolean,
  canConfigureWidget: Boolean,
  canMovePageLeft: Boolean,
  canMovePageRight: Boolean,
  canDeletePage: Boolean,
  onSwapWidgets: () -> Unit,
  onAddPage: () -> Unit,
  onConfigureWidget: () -> Unit,
  onLayoutSelected: (AmbientLayoutKind) -> Unit,
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
        onClick = onConfigureWidget,
        enabled = canConfigureWidget,
      ) {
        Text("Configure", color = if (canConfigureWidget) Color.White else Muted)
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
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      AmbientLayoutKind.entries.forEach { layout ->
        val isSelected = layout == selectedLayout
        TextButton(onClick = { onLayoutSelected(layout) }) {
          Text(layout.name, color = if (isSelected) Color.White else Muted)
        }
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

@Composable
private fun WidgetConfigurationPanel(
  widget: WidgetInstance,
  widgetName: String,
  fields: List<WidgetConfigurationField>,
  onFieldChange: (WidgetConfigurationField, String) -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, Color(0xFF374151), RoundedCornerShape(8.dp))
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = widgetName,
      color = Color.White,
      style = MaterialTheme.typography.titleSmall,
    )
    fields.forEach { field ->
      when (field.type) {
        WidgetConfigurationFieldType.Text ->
          TextField(
            value = widget.configuration.text(field.key, field.defaultValue),
            onValueChange = { value -> onFieldChange(field, value) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(field.label) },
          )
        WidgetConfigurationFieldType.Boolean ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = field.label,
              color = Color.White,
              style = MaterialTheme.typography.bodyLarge,
            )
            Switch(
              checked = widget.configuration.text(field.key, field.defaultValue).toBoolean(),
              onCheckedChange = { checked -> onFieldChange(field, checked.toString()) },
            )
          }
      }
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
          if (slotIndex in indices) {
            set(slotIndex, template.toWidgetInstance())
          }
        }.filterNotNull(),
    )
  }

private fun AmbientPageDeck.updateSelectedWidgetConfiguration(
  slotIndex: Int,
  field: WidgetConfigurationField,
  value: String,
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
                    values = widget.configuration.values + (field.key to value),
                  ),
              ),
            )
          }
        }.filterNotNull(),
    )
  }

private fun AmbientPageDeck.updateSelectedPageLayout(layout: AmbientLayoutKind): AmbientPageDeck =
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

private fun initialVirtualPage(selectedRealPage: Int, realPageCount: Int): Int {
  if (realPageCount <= 0) return 0
  val midpoint = CyclicPagerPageCount / 2
  return midpoint - realPageIndex(midpoint, realPageCount) + selectedRealPage
}

private fun nearestVirtualPage(
  currentVirtualPage: Int,
  targetRealPage: Int,
  realPageCount: Int,
): Int {
  if (realPageCount <= 0) return 0
  if (realPageCount == 1) return currentVirtualPage
  val currentRealPage = realPageIndex(currentVirtualPage, realPageCount)
  val forwardDistance = realPageIndex(targetRealPage - currentRealPage, realPageCount)
  val backwardDistance = forwardDistance - realPageCount
  return if (abs(forwardDistance) <= abs(backwardDistance)) {
    currentVirtualPage + forwardDistance
  } else {
    currentVirtualPage + backwardDistance
  }
}

private fun realPageIndex(virtualPageIndex: Int, realPageCount: Int): Int {
  if (realPageCount <= 1) return 0
  return ((virtualPageIndex % realPageCount) + realPageCount) % realPageCount
}

private fun AmbientPage.slotWidgets(): MutableList<WidgetInstance?> =
  MutableList(layout.slotCount) { index -> widgets.getOrNull(index) }

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
private const val CyclicPagerPageCount = 10_000
private const val PageIndicatorHideDelayMillis = 1_200L
private val previewPageDeck = DefaultAmbientPages.createDeck()
private val previewWidgetRegistry = defaultAmbientWidgetRegistry()
