package com.example.foldambient.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.foldambient.theme.FoldAmbientTheme

@Composable
fun MainScreen(
  isAmbientActive: Boolean,
  onAmbientActiveChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (isAmbientActive) {
    AmbientDashboard(
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
  modifier: Modifier = Modifier,
) {
  if (windowClass == AmbientWindowClass.WideCoverLandscape) {
    Row(
      modifier = modifier,
      horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      AmbientPane(
        title = "Fold Ambient",
        value = "Ready",
        modifier = Modifier.weight(1f),
      )
      AmbientPane(
        title = "Page",
        value = "1",
        modifier = Modifier.weight(1f),
      )
    }
  } else {
    Column(
      modifier = modifier,
      verticalArrangement = Arrangement.Center,
    ) {
      AmbientPane(
        title = "Fold Ambient",
        value = "Ready",
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun AmbientPane(
  title: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(8.dp))
      .padding(28.dp),
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = title,
      color = Muted,
      style = MaterialTheme.typography.titleMedium,
    )
    Text(
      text = value,
      color = Color.White,
      style = MaterialTheme.typography.displaySmall,
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
      onAmbientActiveChange = {},
    )
  }
}

@Preview(showBackground = true, widthDp = 720, heightDp = 320)
@Composable
fun AmbientDashboardCoverPreview() {
  FoldAmbientTheme {
    AmbientDashboard(onExit = {})
  }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 560)
@Composable
fun AmbientDashboardStandardPreview() {
  FoldAmbientTheme {
    AmbientDashboard(onExit = {})
  }
}

private val Night = Color(0xFF05070A)
private val Muted = Color(0xFF9CA3AF)
