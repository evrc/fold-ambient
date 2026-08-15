package com.example.foldambient.ambient.widgets

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.foldambient.ambient.AmbientWidget
import com.example.foldambient.ambient.WidgetInstance

class EmptyWidget : AmbientWidget {
  override val type = "empty"
  override val displayName = "Empty"

  @Composable
  override fun Content(instance: WidgetInstance, modifier: Modifier) {
    Spacer(modifier = modifier.fillMaxSize())
  }
}
