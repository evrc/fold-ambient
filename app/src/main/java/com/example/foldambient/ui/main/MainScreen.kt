package com.example.foldambient.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.foldambient.diagnostics.DeviceDiagnostics
import com.example.foldambient.diagnostics.FoldingFeatureDiagnostics
import com.example.foldambient.theme.FoldAmbientTheme

@Composable
fun MainScreen(
  diagnostics: DeviceDiagnostics,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF05070A))
      .verticalScroll(rememberScrollState())
      .padding(24.dp),
  ) {
    Text(
      text = "Fold Ambient",
      color = Color.White,
      style = MaterialTheme.typography.headlineMedium,
    )
    Text(
      text = "Diagnostics",
      color = Color(0xFF9CA3AF),
      style = MaterialTheme.typography.titleMedium,
    )

    SectionSpacer()
    DiagnosticRow("App display ID", diagnostics.displayId)
    DiagnosticRow("Display rotation", diagnostics.displayRotation)
    DiagnosticRow("Window width px", diagnostics.windowWidthPx.toString())
    DiagnosticRow("Window height px", diagnostics.windowHeightPx.toString())
    DiagnosticRow("Orientation", diagnostics.orientation)

    SectionSpacer()
    Text(
      text = "Folding features",
      color = Color.White,
      style = MaterialTheme.typography.titleMedium,
    )
    if (diagnostics.foldingFeatures.isEmpty()) {
      DiagnosticRow("Reported", "None")
    } else {
      diagnostics.foldingFeatures.forEachIndexed { index, feature ->
        FoldingFeatureBlock(index = index + 1, feature = feature)
      }
    }

    SectionSpacer()
    DiagnosticRow(
      label = "TYPE_HINGE_ANGLE",
      value = if (diagnostics.isHingeAngleSensorAvailable) "Available" else "Unavailable",
    )
    DiagnosticRow(
      label = "Hinge angle",
      value = diagnostics.hingeAngleDegrees?.let { "%.1f deg".format(it) } ?: "Not reported",
    )
  }
}

@Composable
private fun FoldingFeatureBlock(index: Int, feature: FoldingFeatureDiagnostics) {
  SectionSpacer()
  Text(
    text = "Feature $index",
    color = Color.White,
    style = MaterialTheme.typography.titleSmall,
  )
  DiagnosticRow("State", feature.state)
  DiagnosticRow("Orientation", feature.orientation)
  DiagnosticRow("Bounds", feature.bounds)
  DiagnosticRow("Occlusion type", feature.occlusionType)
  DiagnosticRow("Is separating", feature.isSeparating.toString())
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
  Column(modifier = Modifier.padding(top = 10.dp)) {
    Text(
      text = label,
      color = Color(0xFF9CA3AF),
      style = MaterialTheme.typography.labelLarge,
    )
    Text(
      text = value,
      color = Color.White,
      style = MaterialTheme.typography.bodyLarge,
    )
  }
}

@Composable
private fun SectionSpacer() {
  Spacer(modifier = Modifier.height(18.dp))
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  FoldAmbientTheme { MainScreen(diagnostics = previewDiagnostics) }
}

@Preview(showBackground = true, widthDp = 340)
@Composable
fun MainScreenPortraitPreview() {
  FoldAmbientTheme { MainScreen(diagnostics = previewDiagnostics) }
}

private val previewDiagnostics = DeviceDiagnostics(
  displayId = "0",
  displayRotation = "ROTATION_0",
  windowWidthPx = 904,
  windowHeightPx = 2316,
  orientation = "Portrait",
  foldingFeatures = listOf(
    FoldingFeatureDiagnostics(
      state = "HALF_OPENED",
      orientation = "HORIZONTAL",
      bounds = "Rect(0, 1120 - 904, 1160)",
      occlusionType = "NONE",
      isSeparating = true,
    ),
  ),
  isHingeAngleSensorAvailable = true,
  hingeAngleDegrees = 73.4f,
)
