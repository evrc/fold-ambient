package com.example.foldambient.ambient.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.foldambient.ambient.AmbientLayoutKind
import com.example.foldambient.ambient.AmbientPage
import com.example.foldambient.ambient.AmbientWidgetRegistry
import com.example.foldambient.ambient.WidgetInstance

@Composable
fun AmbientPageRenderer(
  page: AmbientPage,
  widgetRegistry: AmbientWidgetRegistry,
  preferDuo: Boolean,
  isEditing: Boolean = false,
  selectedSlotIndex: Int? = null,
  onSlotLongPress: (Int) -> Unit = {},
  onSlotClick: (Int) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val layout = page.layout
  when (layout) {
    AmbientLayoutKind.Full ->
      FullLayout(
        widget = page.widgets.firstOrNull(),
        widgetRegistry = widgetRegistry,
        isEditing = isEditing,
        isSelected = selectedSlotIndex == 0,
        slotIndex = 0,
        onSlotLongPress = onSlotLongPress,
        onSlotClick = onSlotClick,
        modifier = modifier,
      )
    AmbientLayoutKind.Duo ->
      DuoLayout(
        widgets = page.widgets.take(AmbientLayoutKind.Duo.slotCount),
        widgetRegistry = widgetRegistry,
        isEditing = isEditing,
        selectedSlotIndex = selectedSlotIndex,
        onSlotLongPress = onSlotLongPress,
        onSlotClick = onSlotClick,
        modifier = modifier,
      )
    AmbientLayoutKind.Quad ->
      QuadLayout(
        widgets = page.widgets.take(AmbientLayoutKind.Quad.slotCount),
        widgetRegistry = widgetRegistry,
        isEditing = isEditing,
        selectedSlotIndex = selectedSlotIndex,
        onSlotLongPress = onSlotLongPress,
        onSlotClick = onSlotClick,
        modifier = modifier,
      )
  }
}

@Composable
private fun FullLayout(
  widget: WidgetInstance?,
  widgetRegistry: AmbientWidgetRegistry,
  isEditing: Boolean,
  isSelected: Boolean,
  slotIndex: Int,
  onSlotLongPress: (Int) -> Unit,
  onSlotClick: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  WidgetSlot(
    widget = widget,
    widgetRegistry = widgetRegistry,
    isEditing = isEditing,
    isSelected = isSelected,
    slotIndex = slotIndex,
    onSlotLongPress = onSlotLongPress,
    onSlotClick = onSlotClick,
    modifier = modifier,
  )
}

@Composable
private fun DuoLayout(
  widgets: List<WidgetInstance>,
  widgetRegistry: AmbientWidgetRegistry,
  isEditing: Boolean,
  selectedSlotIndex: Int?,
  onSlotLongPress: (Int) -> Unit,
  onSlotClick: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    repeat(AmbientLayoutKind.Duo.slotCount) { index ->
      WidgetSlot(
        widget = widgets.getOrNull(index),
        widgetRegistry = widgetRegistry,
        isEditing = isEditing,
        isSelected = selectedSlotIndex == index,
        slotIndex = index,
        onSlotLongPress = onSlotLongPress,
        onSlotClick = onSlotClick,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun QuadLayout(
  widgets: List<WidgetInstance>,
  widgetRegistry: AmbientWidgetRegistry,
  isEditing: Boolean,
  selectedSlotIndex: Int?,
  onSlotLongPress: (Int) -> Unit,
  onSlotClick: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    repeat(2) { rowIndex ->
      Row(
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        repeat(2) { columnIndex ->
          val slotIndex = rowIndex * 2 + columnIndex
          WidgetSlot(
            widget = widgets.getOrNull(slotIndex),
            widgetRegistry = widgetRegistry,
            isEditing = isEditing,
            isSelected = selectedSlotIndex == slotIndex,
            slotIndex = slotIndex,
            onSlotLongPress = onSlotLongPress,
            onSlotClick = onSlotClick,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }
  }
}

@Composable
private fun WidgetSlot(
  widget: WidgetInstance?,
  widgetRegistry: AmbientWidgetRegistry,
  isEditing: Boolean,
  isSelected: Boolean,
  slotIndex: Int,
  onSlotLongPress: (Int) -> Unit,
  onSlotClick: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val borderColor =
    when {
      isSelected -> Color.White
      isEditing -> Color(0xFF6B7280)
      else -> Color(0xFF1F2937)
    }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .pointerInput(isEditing, slotIndex) {
          detectTapGestures(
            onLongPress = { onSlotLongPress(slotIndex) },
            onTap = {
              if (isEditing) onSlotClick(slotIndex)
            },
          )
        }
        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
        .padding(28.dp),
  ) {
    val renderer = widget?.let(widgetRegistry::widgetFor)
    if (widget != null && renderer != null) {
      renderer.Content(
        instance = widget,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      EmptySlot()
    }
  }
}

@Composable
private fun EmptySlot() {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = "Widget",
      color = Color(0xFF9CA3AF),
      style = MaterialTheme.typography.titleMedium,
    )
    Text(
      text = "Empty",
      color = Color.White,
      style = MaterialTheme.typography.displaySmall,
    )
  }
}
