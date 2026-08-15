package com.example.foldambient

import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.foldambient.theme.FoldAmbientTheme
import com.example.foldambient.ui.main.MainScreen

class MainActivity : ComponentActivity() {
  private var requestedAmbientWindowMode = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      var isAmbientActive by rememberSaveable { mutableStateOf(false) }

      LaunchedEffect(isAmbientActive) {
        setAmbientWindowMode(isAmbientActive)
      }
      DisposableEffect(Unit) {
        onDispose { setAmbientWindowMode(false) }
      }

      FoldAmbientTheme {
        MainScreen(
          isAmbientActive = isAmbientActive,
          onAmbientActiveChange = { isAmbientActive = it },
          modifier = Modifier.fillMaxSize(),
        )
      }
    }
  }

  override fun onStart() {
    super.onStart()
    applyAmbientWindowMode(requestedAmbientWindowMode)
  }

  override fun onStop() {
    applyAmbientWindowMode(false)
    super.onStop()
  }

  private fun setAmbientWindowMode(isAmbientActive: Boolean) {
    requestedAmbientWindowMode = isAmbientActive
    applyAmbientWindowMode(isAmbientActive)
  }

  private fun applyAmbientWindowMode(isAmbientActive: Boolean) {
    val controller = window.insetsController
    if (isAmbientActive) {
      window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      controller?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      controller?.hide(WindowInsets.Type.systemBars())
    } else {
      window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      controller?.show(WindowInsets.Type.systemBars())
    }
  }
}
