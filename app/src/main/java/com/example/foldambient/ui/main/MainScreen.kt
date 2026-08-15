package com.example.foldambient.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.foldambient.activation.AmbientActivationEvaluation
import com.example.foldambient.activation.AmbientActivationSettings
import com.example.foldambient.activation.AmbientActivationSignals
import com.example.foldambient.activation.normalized
import com.example.foldambient.ambient.AmbientLayoutKind
import com.example.foldambient.ambient.AmbientPageDeck
import com.example.foldambient.ambient.AmbientWidgetRegistry
import com.example.foldambient.ambient.AmbientWidgetTemplate
import com.example.foldambient.ambient.DefaultAmbientPages
import com.example.foldambient.ambient.WidgetConfigurationField
import com.example.foldambient.ambient.WidgetConfigurationFieldType
import com.example.foldambient.ambient.WidgetInstance
import com.example.foldambient.ambient.addPageAfterSelected
import com.example.foldambient.ambient.deleteSelectedPage
import com.example.foldambient.ambient.moveSelectedPageBy
import com.example.foldambient.ambient.replaceSelectedPageWidget
import com.example.foldambient.ambient.selectedPageIndex
import com.example.foldambient.ambient.slotWidgets
import com.example.foldambient.ambient.swapSelectedPageSlots
import com.example.foldambient.ambient.updateSelectedPageLayout
import com.example.foldambient.ambient.updateSelectedWidgetConfiguration
import com.example.foldambient.ambient.ui.AmbientPageRenderer
import com.example.foldambient.ambient.widgets.defaultAmbientWidgetRegistry
import com.example.foldambient.display.AmbientDisplaySettings
import com.example.foldambient.display.normalized
import com.example.foldambient.theme.FoldAmbientTheme
import com.example.foldambient.weather.OpenMeteoGeocodingRepository
import com.example.foldambient.weather.PhoneWeatherLocationProvider
import com.example.foldambient.weather.PhoneWeatherLocationResult
import com.example.foldambient.weather.WeatherLocationMode
import com.example.foldambient.weather.WeatherLocationSearchResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun MainScreen(
  isAmbientActive: Boolean,
  pageDeck: AmbientPageDeck,
  widgetRegistry: AmbientWidgetRegistry,
  activationSettings: AmbientActivationSettings = AmbientActivationSettings(),
  activationSignals: AmbientActivationSignals = AmbientActivationSignals(),
  activationEvaluation: AmbientActivationEvaluation =
    AmbientActivationEvaluation(
      shouldActivate = false,
      isCoverLikeLandscape = false,
      isHingeInRange = false,
      isChargingSatisfied = true,
    ),
  displaySettings: AmbientDisplaySettings = AmbientDisplaySettings(),
  isAmbientIdle: Boolean = false,
  onPageDeckChange: (AmbientPageDeck) -> Unit,
  onActivationSettingsChange: (AmbientActivationSettings) -> Unit = {},
  onDisplaySettingsChange: (AmbientDisplaySettings) -> Unit = {},
  onAmbientActiveChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (isAmbientActive) {
    AmbientDashboard(
      pageDeck = pageDeck,
      widgetRegistry = widgetRegistry,
      displaySettings = displaySettings,
      isAmbientIdle = isAmbientIdle,
      onPageDeckChange = onPageDeckChange,
      onExit = { onAmbientActiveChange(false) },
      modifier = modifier,
    )
  } else {
    EntryShell(
      activationSettings = activationSettings,
      activationSignals = activationSignals,
      activationEvaluation = activationEvaluation,
      displaySettings = displaySettings,
      onActivationSettingsChange = onActivationSettingsChange,
      onDisplaySettingsChange = onDisplaySettingsChange,
      onEnter = { onAmbientActiveChange(true) },
      modifier = modifier,
    )
  }
}

@Composable
private fun EntryShell(
  activationSettings: AmbientActivationSettings,
  activationSignals: AmbientActivationSignals,
  activationEvaluation: AmbientActivationEvaluation,
  displaySettings: AmbientDisplaySettings,
  onActivationSettingsChange: (AmbientActivationSettings) -> Unit,
  onDisplaySettingsChange: (AmbientDisplaySettings) -> Unit,
  onEnter: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Night)
      .verticalScroll(rememberScrollState())
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
    SectionSpacer()
    AutoActivationPanel(
      settings = activationSettings,
      signals = activationSignals,
      evaluation = activationEvaluation,
      onSettingsChange = onActivationSettingsChange,
    )
    SectionSpacer()
    DisplaySettingsPanel(
      settings = displaySettings,
      onSettingsChange = onDisplaySettingsChange,
    )
  }
}

@Composable
private fun AutoActivationPanel(
  settings: AmbientActivationSettings,
  signals: AmbientActivationSignals,
  evaluation: AmbientActivationEvaluation,
  onSettingsChange: (AmbientActivationSettings) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Auto ambient",
        color = Color.White,
        style = MaterialTheme.typography.titleSmall,
      )
      Switch(
        checked = settings.isEnabled,
        onCheckedChange = { enabled ->
          onSettingsChange(settings.copy(isEnabled = enabled).normalized())
        },
      )
    }

    Text(
      text = activationStatusText(signals, evaluation),
      color = Muted,
      style = MaterialTheme.typography.bodyMedium,
    )

    if (settings.isEnabled) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Require charging",
          color = Color.White,
          style = MaterialTheme.typography.bodyLarge,
        )
        Switch(
          checked = settings.requireCharging,
          onCheckedChange = { requireCharging ->
            onSettingsChange(settings.copy(requireCharging = requireCharging).normalized())
          },
        )
      }

      Text(
        text = "Hinge ${settings.minHingeAngle.roundToInt()}-${settings.maxHingeAngle.roundToInt()} deg",
        color = Muted,
        style = MaterialTheme.typography.labelLarge,
      )
      Slider(
        value = settings.minHingeAngle,
        onValueChange = { value ->
          onSettingsChange(
            settings.copy(minHingeAngle = value.coerceAtMost(settings.maxHingeAngle - 5f))
              .normalized(),
          )
        },
        valueRange = 0f..175f,
      )
      Slider(
        value = settings.maxHingeAngle,
        onValueChange = { value ->
          onSettingsChange(
            settings.copy(maxHingeAngle = value.coerceAtLeast(settings.minHingeAngle + 5f))
              .normalized(),
          )
        },
        valueRange = 5f..180f,
      )

      Text(
        text = "Cover shape ${settings.minCoverLandscapeAspectRatio.roundToTenth()}",
        color = Muted,
        style = MaterialTheme.typography.labelLarge,
      )
      Slider(
        value = settings.minCoverLandscapeAspectRatio,
        onValueChange = { value ->
          onSettingsChange(settings.copy(minCoverLandscapeAspectRatio = value).normalized())
        },
        valueRange = 1.5f..2.5f,
      )
    }
  }
}

@Composable
private fun DisplaySettingsPanel(
  settings: AmbientDisplaySettings,
  onSettingsChange: (AmbientDisplaySettings) -> Unit,
) {
  val normalized = settings.normalized()
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text(
      text = "Display",
      color = Color.White,
      style = MaterialTheme.typography.titleSmall,
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Keep screen on",
        color = Color.White,
        style = MaterialTheme.typography.bodyLarge,
      )
      Switch(
        checked = normalized.keepScreenOn,
        onCheckedChange = { keepScreenOn ->
          onSettingsChange(normalized.copy(keepScreenOn = keepScreenOn).normalized())
        },
      )
    }

    SettingSlider(
      label = "Active brightness ${normalized.activeBrightness.toPercent()}%",
      value = normalized.activeBrightness,
      valueRange = 0.02f..1f,
      onValueChange = { brightness ->
        onSettingsChange(normalized.copy(activeBrightness = brightness).normalized())
      },
    )
    SettingSlider(
      label = "Idle brightness ${normalized.idleBrightness.toPercent()}%",
      value = normalized.idleBrightness,
      valueRange = 0.02f..normalized.activeBrightness.coerceAtLeast(0.03f),
      onValueChange = { brightness ->
        onSettingsChange(normalized.copy(idleBrightness = brightness).normalized())
      },
    )
    SettingSlider(
      label = "Dim after ${normalized.idleDelayMillis.toSeconds()}s",
      value = normalized.idleDelayMillis / 1_000f,
      valueRange = 30f..300f,
      onValueChange = { seconds ->
        onSettingsChange(normalized.copy(idleDelayMillis = seconds.roundToInt() * 1_000L).normalized())
      },
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Pixel shift",
        color = Color.White,
        style = MaterialTheme.typography.bodyLarge,
      )
      Switch(
        checked = normalized.pixelShiftEnabled,
        onCheckedChange = { enabled ->
          onSettingsChange(normalized.copy(pixelShiftEnabled = enabled).normalized())
        },
      )
    }

    if (normalized.pixelShiftEnabled) {
      SettingSlider(
        label = "Shift active ${normalized.pixelShiftActiveIntervalMillis.toSeconds()}s",
        value = normalized.pixelShiftActiveIntervalMillis / 1_000f,
        valueRange = 15f..180f,
        onValueChange = { seconds ->
          onSettingsChange(
            normalized.copy(pixelShiftActiveIntervalMillis = seconds.roundToInt() * 1_000L)
              .normalized(),
          )
        },
      )
      SettingSlider(
        label = "Shift idle ${normalized.pixelShiftIdleIntervalMillis.toSeconds()}s",
        value = normalized.pixelShiftIdleIntervalMillis / 1_000f,
        valueRange = 15f..180f,
        onValueChange = { seconds ->
          onSettingsChange(
            normalized.copy(pixelShiftIdleIntervalMillis = seconds.roundToInt() * 1_000L)
              .normalized(),
          )
        },
      )
    }
  }
}

@Composable
private fun SettingSlider(
  label: String,
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  onValueChange: (Float) -> Unit,
) {
  Text(
    text = label,
    color = Muted,
    style = MaterialTheme.typography.labelLarge,
  )
  Slider(
    value = value.coerceIn(valueRange.start, valueRange.endInclusive),
    onValueChange = onValueChange,
    valueRange = valueRange,
  )
}

@Composable
private fun AmbientDashboard(
  pageDeck: AmbientPageDeck,
  widgetRegistry: AmbientWidgetRegistry,
  displaySettings: AmbientDisplaySettings,
  isAmbientIdle: Boolean,
  onPageDeckChange: (AmbientPageDeck) -> Unit,
  onExit: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isEditing by remember { mutableStateOf(false) }
  var selectedSlotIndex by remember { mutableIntStateOf(0) }
  var isPickerOpen by remember { mutableStateOf(false) }
  var isConfigurationOpen by remember { mutableStateOf(false) }
  var isPageIndicatorVisible by remember { mutableStateOf(false) }
  var pixelShiftIndex by remember { mutableIntStateOf(0) }
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

  LaunchedEffect(isEditing, isAmbientIdle, displaySettings) {
    while (!isEditing && displaySettings.pixelShiftEnabled) {
      delay(
        if (isAmbientIdle) {
          displaySettings.pixelShiftIdleIntervalMillis
        } else {
          displaySettings.pixelShiftActiveIntervalMillis
        },
      )
      pixelShiftIndex = (pixelShiftIndex + 1) % PixelShiftSteps.size
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
    val editPanelMaxHeight = if (maxWidth > maxHeight) maxHeight * 0.52f else maxHeight * 0.38f
    val pixelShift =
      if (isEditing || !displaySettings.pixelShiftEnabled) {
        PixelShiftOrigin
      } else {
        PixelShiftSteps[pixelShiftIndex]
      }
    val animatedPixelShiftX by animateDpAsState(
      targetValue = pixelShift.x,
      animationSpec = tween(durationMillis = 1_200, easing = FastOutSlowInEasing),
      label = "pixelShiftX",
    )
    val animatedPixelShiftY by animateDpAsState(
      targetValue = pixelShift.y,
      animationSpec = tween(durationMillis = 1_200, easing = FastOutSlowInEasing),
      label = "pixelShiftY",
    )

    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
      ) {
        Box(
          modifier =
            Modifier
              .fillMaxSize()
              .offset(x = animatedPixelShiftX, y = animatedPixelShiftY),
        ) {
          HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
          ) { virtualPageIndex ->
            val page = pageDeck.pages[realPageIndex(virtualPageIndex, realPageCount)]
            val pageOffset =
              abs(
                (pagerState.currentPage - virtualPageIndex) +
                  pagerState.currentPageOffsetFraction,
              ).coerceIn(0f, 1f)
            val pageScale = 0.965f + ((1f - pageOffset) * 0.035f)
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
              modifier =
                Modifier
                  .fillMaxSize()
                  .graphicsLayer {
                    alpha = 0.72f + ((1f - pageOffset) * 0.28f)
                    scaleX = pageScale
                    scaleY = pageScale
                  },
            )
          }
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
        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .heightIn(max = editPanelMaxHeight)
              .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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

          if (isPickerOpen) {
            WidgetPicker(
              widgetRegistry = widgetRegistry,
              onTemplateSelected = { template ->
                onPageDeckChange(pageDeck.replaceSelectedPageWidget(selectedSlotIndex, template))
                isPickerOpen = false
                isConfigurationOpen = false
              },
            )
          }

          if (isConfigurationOpen && selectedWidget != null && selectedWidgetRenderer != null) {
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
              onValuesChange = { values ->
                onPageDeckChange(
                  pageDeck.updateSelectedWidgetConfiguration(
                    slotIndex = selectedSlotIndex,
                    values = values,
                  ),
                )
              },
            )
          }
        }
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
      .background(Color(0xFF0A0D10), RoundedCornerShape(8.dp))
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
      widget.PreviewContent(
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
  onValuesChange: (Map<String, String>) -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0xFF0A0D10), RoundedCornerShape(8.dp))
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
        WidgetConfigurationFieldType.Option ->
          OptionField(
            field = field,
            selectedValue = widget.configuration.text(field.key, field.defaultValue),
            onFieldChange = onFieldChange,
          )
        WidgetConfigurationFieldType.Location ->
          LocationField(
            widget = widget,
            field = field,
            onValuesChange = onValuesChange,
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

@Composable
private fun LocationField(
  widget: WidgetInstance,
  field: WidgetConfigurationField,
  onValuesChange: (Map<String, String>) -> Unit,
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val phoneLocationProvider =
    remember(context) { PhoneWeatherLocationProvider(context.applicationContext) }
  val geocodingRepository = remember { OpenMeteoGeocodingRepository() }
  val locationMode =
    WeatherLocationMode.fromValue(
      widget.configuration.text("locationMode", WeatherLocationMode.Manual.value),
    )
  var query by remember(widget.id) {
    mutableStateOf(widget.configuration.text("locationName", field.defaultValue))
  }
  var searchResults by remember { mutableStateOf(emptyList<WeatherLocationSearchResult>()) }
  var searchStatus by remember { mutableStateOf<String?>(null) }
  var phoneStatus by remember { mutableStateOf<String?>(null) }

  fun savePhoneLocation(location: Location) {
    onValuesChange(
      mapOf(
        "locationMode" to WeatherLocationMode.Phone.value,
        "locationName" to "Current location",
        "latitude" to location.latitude.toString(),
        "longitude" to location.longitude.toString(),
      ),
    )
  }

  fun resolvePhoneLocation() {
    coroutineScope.launch {
      phoneStatus = "Locating"
      when (val result = phoneLocationProvider.currentLocation()) {
        is PhoneWeatherLocationResult.Available -> {
          savePhoneLocation(result.location)
          phoneStatus = null
        }
        PhoneWeatherLocationResult.MissingPermission -> {
          phoneStatus = "Location permission needed"
        }
        is PhoneWeatherLocationResult.Unavailable -> {
          phoneStatus = result.reason
        }
      }
    }
  }

  val locationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        resolvePhoneLocation()
      } else {
        phoneStatus = "Location denied"
        onValuesChange(mapOf("locationMode" to WeatherLocationMode.Manual.value))
      }
    }

  LaunchedEffect(query, locationMode) {
    if (locationMode != WeatherLocationMode.Manual) return@LaunchedEffect
    val trimmedQuery = query.trim()
    if (trimmedQuery.length < 2) {
      searchResults = emptyList()
      searchStatus = null
      return@LaunchedEffect
    }

    searchStatus = "Searching"
    delay(LocationSearchDebounceMillis)
    searchResults = geocodingRepository.searchLocations(trimmedQuery)
    searchStatus = if (searchResults.isEmpty()) "No matches" else null
  }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = field.label,
      color = Color.White,
      style = MaterialTheme.typography.bodyLarge,
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Use phone location",
        color = if (locationMode == WeatherLocationMode.Phone) Color.White else Muted,
        style = MaterialTheme.typography.bodyMedium,
      )
      Switch(
        checked = locationMode == WeatherLocationMode.Phone,
        onCheckedChange = { usePhoneLocation ->
          if (usePhoneLocation) {
            onValuesChange(
              mapOf(
                "locationMode" to WeatherLocationMode.Phone.value,
                "locationName" to "Current location",
              ),
            )
            if (context.hasCoarseLocationPermission()) {
              resolvePhoneLocation()
            } else {
              locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
          } else {
            phoneStatus = null
            onValuesChange(mapOf("locationMode" to WeatherLocationMode.Manual.value))
          }
        },
      )
    }

    phoneStatus?.let { status ->
      Text(
        text = status,
        color = Muted,
        style = MaterialTheme.typography.labelLarge,
      )
    }

    if (locationMode == WeatherLocationMode.Manual) {
      TextField(
        value = query,
        onValueChange = { value -> query = value },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Search city") },
      )
      searchStatus?.let { status ->
        Text(
          text = status,
          color = Muted,
          style = MaterialTheme.typography.labelLarge,
        )
      }
      searchResults.forEach { result ->
        TextButton(
          onClick = {
            query = result.displayName
            searchResults = emptyList()
            searchStatus = null
            onValuesChange(
              mapOf(
                "locationMode" to WeatherLocationMode.Manual.value,
                "locationName" to result.displayName,
                "latitude" to result.latitude.toString(),
                "longitude" to result.longitude.toString(),
              ),
            )
          },
        ) {
          Text(
            text = result.displayName,
            color = Color.White,
          )
        }
      }
    } else {
      Text(
        text = widget.configuration.text("locationName", "Current location"),
        color = Muted,
        style = MaterialTheme.typography.labelLarge,
      )
    }
  }
}

@Composable
private fun OptionField(
  field: WidgetConfigurationField,
  selectedValue: String,
  onFieldChange: (WidgetConfigurationField, String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(
      text = field.label,
      color = Color.White,
      style = MaterialTheme.typography.bodyLarge,
    )
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      field.options.forEach { option ->
        val isSelected = option.value == selectedValue
        TextButton(onClick = { onFieldChange(field, option.value) }) {
          Text(
            text = option.label,
            color = if (isSelected) Color.White else Muted,
          )
        }
      }
    }
  }
}

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

private fun ambientWindowClass(width: Dp, height: Dp): AmbientWindowClass =
  if (width > height * 1.8f) AmbientWindowClass.WideCoverLandscape else AmbientWindowClass.Standard

private fun activationStatusText(
  signals: AmbientActivationSignals,
  evaluation: AmbientActivationEvaluation,
): String =
  when {
    evaluation.shouldActivate -> "Auto ready"
    !signals.hingeSensorAvailable -> "Hinge sensor unavailable"
    !evaluation.isCoverLikeLandscape -> "Waiting for cover landscape"
    !evaluation.isHingeInRange ->
      "Hinge ${signals.hingeAngleDegrees?.roundToInt()?.toString() ?: "--"} deg"
    !evaluation.isChargingSatisfied -> "Waiting for charging"
    else -> "Waiting"
  }

private fun Float.roundToTenth(): String =
  ((this * 10f).roundToInt() / 10f).toString()

private fun Float.toPercent(): Int = (this * 100f).roundToInt()

private fun Long.toSeconds(): Long = this / 1_000L

private fun Context.hasCoarseLocationPermission(): Boolean =
  checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

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
      displaySettings = AmbientDisplaySettings(),
      isAmbientIdle = false,
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
      displaySettings = AmbientDisplaySettings(),
      isAmbientIdle = false,
      onPageDeckChange = {},
      onExit = {},
    )
  }
}

private val Night = Color(0xFF05070A)
private val Muted = Color(0xFF9CA3AF)
private const val CyclicPagerPageCount = 10_000
private const val PageIndicatorHideDelayMillis = 1_200L
private const val LocationSearchDebounceMillis = 350L
private data class PixelShiftStep(val x: Dp, val y: Dp)
private val PixelShiftOrigin = PixelShiftStep(x = 0.dp, y = 0.dp)
private val PixelShiftSteps =
  listOf(
    PixelShiftOrigin,
    PixelShiftStep(x = 2.dp, y = 1.dp),
    PixelShiftStep(x = -1.dp, y = 2.dp),
    PixelShiftStep(x = 1.dp, y = -2.dp),
    PixelShiftStep(x = -2.dp, y = -1.dp),
  )
private val previewPageDeck = DefaultAmbientPages.createDeck()
private val previewWidgetRegistry = defaultAmbientWidgetRegistry()
