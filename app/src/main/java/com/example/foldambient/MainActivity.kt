package com.example.foldambient

import android.os.Bundle
import android.view.Surface
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.example.foldambient.activation.AmbientActivationMonitor
import com.example.foldambient.activation.AmbientActivationPolicy
import com.example.foldambient.activation.AmbientActivationSignals
import com.example.foldambient.activation.SharedPreferencesAmbientActivationSettingsRepository
import com.example.foldambient.ambient.SharedPreferencesAmbientPageRepository
import com.example.foldambient.ambient.widgets.defaultAmbientWidgetRegistry
import com.example.foldambient.display.AmbientDisplayPolicy
import com.example.foldambient.theme.FoldAmbientTheme
import com.example.foldambient.ui.main.MainScreen
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
  private var requestedAmbientWindowMode = false
  private val activationSignalsState = mutableStateOf(AmbientActivationSignals())
  private val userInteractionTickState = mutableStateOf(0L)
  private lateinit var activationMonitor: AmbientActivationMonitor

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activationMonitor =
      AmbientActivationMonitor(applicationContext) { signals ->
        activationSignalsState.value = signals
      }

    enableEdgeToEdge()
    setContent {
      var isAmbientActive by rememberSaveable { mutableStateOf(false) }
      var isAutomaticAmbientSession by rememberSaveable { mutableStateOf(false) }
      var isAutoActivationPaused by rememberSaveable { mutableStateOf(false) }
      var isAmbientIdle by remember { mutableStateOf(false) }
      val userInteractionTick by userInteractionTickState
      val pageRepository =
        remember {
          SharedPreferencesAmbientPageRepository(applicationContext)
        }
      val activationSettingsRepository =
        remember {
          SharedPreferencesAmbientActivationSettingsRepository(applicationContext)
        }
      var pageDeck by remember { mutableStateOf(pageRepository.loadDeck()) }
      var activationSettings by remember { mutableStateOf(activationSettingsRepository.load()) }
      val activationSignals by activationSignalsState
      val activationEvaluation =
        AmbientActivationPolicy.evaluate(
          settings = activationSettings,
          signals = activationSignals,
        )
      val widgetRegistry = remember { defaultAmbientWidgetRegistry() }

      LaunchedEffect(isAmbientActive, userInteractionTick) {
        isAmbientIdle = false
        if (isAmbientActive) {
          delay(AmbientDisplayPolicy.IdleDelayMillis)
          isAmbientIdle = true
        }
      }
      LaunchedEffect(isAmbientActive, isAmbientIdle) {
        setAmbientWindowMode(
          isAmbientActive = isAmbientActive,
          isIdle = isAmbientIdle,
        )
      }
      LaunchedEffect(
        activationEvaluation.shouldActivate,
        isAmbientActive,
        isAutomaticAmbientSession,
        isAutoActivationPaused,
      ) {
        if (!activationEvaluation.shouldActivate) {
          isAutoActivationPaused = false
          if (isAmbientActive && isAutomaticAmbientSession) {
            isAutomaticAmbientSession = false
            isAmbientActive = false
          }
        } else if (!isAmbientActive && !isAutoActivationPaused) {
          isAutomaticAmbientSession = true
          isAmbientActive = true
        }
      }
      DisposableEffect(Unit) {
        onDispose {
          setAmbientWindowMode(
            isAmbientActive = false,
            isIdle = false,
          )
        }
      }

      FoldAmbientTheme {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
          val density = LocalDensity.current
          val displayRotation = this@MainActivity.display?.rotation ?: Surface.ROTATION_0

          LaunchedEffect(maxWidth, maxHeight, density, displayRotation) {
            activationMonitor.updateWindowGeometry(
              widthPx = with(density) { maxWidth.roundToPx() },
              heightPx = with(density) { maxHeight.roundToPx() },
              displayRotation = displayRotation,
            )
          }

          MainScreen(
            isAmbientActive = isAmbientActive,
            pageDeck = pageDeck,
            widgetRegistry = widgetRegistry,
            activationSettings = activationSettings,
            activationSignals = activationSignals,
            activationEvaluation = activationEvaluation,
            isAmbientIdle = isAmbientIdle,
            onPageDeckChange = { updatedDeck ->
              pageDeck = updatedDeck
              pageRepository.saveDeck(updatedDeck)
            },
            onActivationSettingsChange = { updatedSettings ->
              activationSettings = updatedSettings
              activationSettingsRepository.save(updatedSettings)
            },
            onAmbientActiveChange = { shouldBeActive ->
              if (shouldBeActive) {
                isAutomaticAmbientSession = false
                isAutoActivationPaused = false
                isAmbientActive = true
              } else {
                if (isAutomaticAmbientSession && activationEvaluation.shouldActivate) {
                  isAutoActivationPaused = true
                }
                isAutomaticAmbientSession = false
                isAmbientActive = false
              }
            },
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
    }
  }

  override fun onStart() {
    super.onStart()
    activationMonitor.start()
    applyAmbientWindowMode(requestedAmbientWindowMode)
  }

  override fun onStop() {
    applyAmbientWindowMode(false)
    activationMonitor.stop()
    super.onStop()
  }

  override fun onUserInteraction() {
    super.onUserInteraction()
    userInteractionTickState.value += 1L
  }

  private fun setAmbientWindowMode(
    isAmbientActive: Boolean,
    isIdle: Boolean,
  ) {
    requestedAmbientWindowMode = isAmbientActive
    applyAmbientWindowMode(
      isAmbientActive = isAmbientActive,
      isIdle = isIdle,
    )
  }

  private fun applyAmbientWindowMode(
    isAmbientActive: Boolean,
    isIdle: Boolean = false,
  ) {
    val controller = window.insetsController
    window.attributes =
      window.attributes.apply {
        screenBrightness =
          AmbientDisplayPolicy.brightnessFor(
            isAmbientActive = isAmbientActive,
            isIdle = isIdle,
          )
      }

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
