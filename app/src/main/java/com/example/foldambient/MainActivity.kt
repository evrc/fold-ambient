package com.example.foldambient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.foldambient.diagnostics.DeviceDiagnosticsController
import com.example.foldambient.theme.FoldAmbientTheme
import com.example.foldambient.ui.main.MainScreen

class MainActivity : ComponentActivity() {
  private lateinit var diagnosticsController: DeviceDiagnosticsController

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    diagnosticsController = DeviceDiagnosticsController(this)
    enableEdgeToEdge()
    setContent {
      val diagnostics by diagnosticsController.diagnostics.collectAsState()
      FoldAmbientTheme {
        MainScreen(
          diagnostics = diagnostics,
          modifier = Modifier.fillMaxSize(),
        )
      }
    }
  }
}
