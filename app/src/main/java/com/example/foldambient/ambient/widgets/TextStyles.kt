package com.example.foldambient.ambient.widgets

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow

private val Muted = Color(0xFF9CA3AF)

internal val LocalWidgetLabelsVisible = staticCompositionLocalOf { true }

@Composable
internal fun WidgetLabel(text: String) {
  if (!LocalWidgetLabelsVisible.current) return

  Text(
    text = text,
    color = Muted,
    style = MaterialTheme.typography.titleMedium,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
  )
}

@Composable
internal fun WidgetValue(text: String) {
  Text(
    text = text,
    color = Color.White,
    style = MaterialTheme.typography.displaySmall,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
  )
}
