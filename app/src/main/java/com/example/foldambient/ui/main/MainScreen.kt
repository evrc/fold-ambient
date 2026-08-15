package com.example.foldambient.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.foldambient.ambient.AmbientPageDeck
import com.example.foldambient.ambient.AmbientWidgetRegistry
import com.example.foldambient.ambient.DefaultAmbientPages
import com.example.foldambient.ambient.ui.AmbientPageRenderer
import com.example.foldambient.ambient.widgets.defaultAmbientWidgetRegistry
import com.example.foldambient.theme.FoldAmbientTheme

@Composable
fun MainScreen(
  isAmbientActive: Boolean,
  pageDeck: AmbientPageDeck,
  widgetRegistry: AmbientWidgetRegistry,
  onAmbientActiveChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (isAmbientActive) {
    AmbientDashboard(
      pageDeck = pageDeck,
      widgetRegistry = widgetRegistry,
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
  onExit: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black)
      .padding(18.dp),
  ) {
    val windowClass = ambientWindowClass(maxWidth, maxHeight)
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      AmbientContent(
        windowClass = windowClass,
        pageDeck = pageDeck,
        widgetRegistry = widgetRegistry,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
      )
      TextButton(onClick = onExit) {
        Text("Exit", color = Muted)
      }
    }
  }
}

@Composable
private fun AmbientContent(
  windowClass: AmbientWindowClass,
  pageDeck: AmbientPageDeck,
  widgetRegistry: AmbientWidgetRegistry,
  modifier: Modifier = Modifier,
) {
  pageDeck.selectedPage?.let { selectedPage ->
    AmbientPageRenderer(
      page = selectedPage,
      widgetRegistry = widgetRegistry,
      preferDuo = windowClass == AmbientWindowClass.WideCoverLandscape,
      modifier = modifier,
    )
  }
}

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
      onExit = {},
    )
  }
}

private val Night = Color(0xFF05070A)
private val Muted = Color(0xFF9CA3AF)
private val previewPageDeck = DefaultAmbientPages.createDeck()
private val previewWidgetRegistry = defaultAmbientWidgetRegistry()
