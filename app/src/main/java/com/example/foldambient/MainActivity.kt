package com.example.foldambient

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.foldambient.activation.AmbientActivationCommand
import com.example.foldambient.activation.AmbientActivationMonitor
import com.example.foldambient.activation.AmbientActivationPolicy
import com.example.foldambient.activation.AmbientActivationSignals
import com.example.foldambient.activation.AmbientActivationStateMachine
import com.example.foldambient.activation.AmbientActivationTransition
import com.example.foldambient.activation.SharedPreferencesAmbientActivationSettingsRepository
import com.example.foldambient.ambient.SharedPreferencesAmbientPageRepository
import com.example.foldambient.ambient.widgets.defaultAmbientWidgetRegistry
import com.example.foldambient.display.AmbientDisplaySettings
import com.example.foldambient.display.AmbientDisplayPolicy
import com.example.foldambient.display.SharedPreferencesAmbientDisplaySettingsRepository
import com.example.foldambient.media.PlatformMediaRepository
import com.example.foldambient.theme.FoldAmbientTheme
import com.example.foldambient.ui.main.MainScreen
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
  private var requestedAmbientWindowMode = false
  private var ambientDisplaySettings = AmbientDisplaySettings()
  private val activationSignalsState = mutableStateOf(AmbientActivationSignals())
  private val userInteractionTickState = mutableStateOf(0L)
  private lateinit var activationMonitor: AmbientActivationMonitor
  private lateinit var mediaRepository: PlatformMediaRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activationMonitor =
      AmbientActivationMonitor(applicationContext) { signals ->
        activationSignalsState.value = signals
      }
    mediaRepository = PlatformMediaRepository(applicationContext)

    enableEdgeToEdge()
    setContent {
      var isAmbientActive by rememberSaveable { mutableStateOf(false) }
      var isAmbientIdle by remember { mutableStateOf(false) }
      val userInteractionTick by userInteractionTickState
      val lifecycleOwner = LocalLifecycleOwner.current
      var isLifecycleStarted by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
      }
      val activationStateMachine =
        remember {
          AmbientActivationStateMachine { message ->
            Log.d(ACTIVATION_LOG_TAG, message)
          }
        }
      var activationSnapshot by remember { mutableStateOf(activationStateMachine.snapshot) }
      val pageRepository =
        remember {
          SharedPreferencesAmbientPageRepository(applicationContext)
        }
      val activationSettingsRepository =
        remember {
          SharedPreferencesAmbientActivationSettingsRepository(applicationContext)
        }
      val displaySettingsRepository =
        remember {
          SharedPreferencesAmbientDisplaySettingsRepository(applicationContext)
        }
      var pageDeck by remember { mutableStateOf(pageRepository.loadDeck()) }
      var activationSettings by remember { mutableStateOf(activationSettingsRepository.load()) }
      var displaySettings by remember { mutableStateOf(displaySettingsRepository.load()) }
      val activationSignals by activationSignalsState
      val activationEvaluation =
        AmbientActivationPolicy.evaluate(
          settings = activationSettings,
          signals = activationSignals,
        )
      val widgetRegistry = remember { defaultAmbientWidgetRegistry(mediaRepository) }

      fun applyActivationTransition(transition: AmbientActivationTransition) {
        activationSnapshot = transition.snapshot
        when (transition.command) {
          AmbientActivationCommand.ActivateAmbient -> {
            isAmbientActive = true
          }
          AmbientActivationCommand.DeactivateAmbient -> {
            isAmbientActive = false
          }
          null -> Unit
        }
      }

      DisposableEffect(lifecycleOwner, activationStateMachine) {
        val observer =
          LifecycleEventObserver { _, event ->
            when (event) {
              Lifecycle.Event.ON_START -> {
                isLifecycleStarted = true
              }
              Lifecycle.Event.ON_STOP -> {
                isLifecycleStarted = false
                applyActivationTransition(activationStateMachine.onLifecycleStopped())
              }
              else -> Unit
            }
          }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
          lifecycleOwner.lifecycle.removeObserver(observer)
        }
      }
      LaunchedEffect(
        activationEvaluation.shouldActivate,
        isLifecycleStarted,
        activationStateMachine,
      ) {
        if (isLifecycleStarted) {
          applyActivationTransition(
            activationStateMachine.onRawEligibilityChanged(
              eligible = activationEvaluation.shouldActivate,
              nowMillis = SystemClock.elapsedRealtime(),
            ),
          )
        }
      }
      LaunchedEffect(
        activationSnapshot.timerDeadlineMillis,
        isLifecycleStarted,
        activationStateMachine,
      ) {
        val deadlineMillis = activationSnapshot.timerDeadlineMillis
        if (isLifecycleStarted && deadlineMillis != null) {
          val delayMillis = (deadlineMillis - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
          delay(delayMillis)
          applyActivationTransition(
            activationStateMachine.onTimer(nowMillis = SystemClock.elapsedRealtime()),
          )
        }
      }
      LaunchedEffect(isAmbientActive, userInteractionTick, displaySettings.idleDelayMillis) {
        isAmbientIdle = false
        if (isAmbientActive) {
          delay(displaySettings.idleDelayMillis)
          isAmbientIdle = true
        }
      }
      LaunchedEffect(isAmbientActive, isAmbientIdle, displaySettings) {
        ambientDisplaySettings = displaySettings
        setAmbientWindowMode(
          isAmbientActive = isAmbientActive,
          isIdle = isAmbientIdle,
        )
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
            displaySettings = displaySettings,
            isAmbientIdle = isAmbientIdle,
            onPageDeckChange = { updatedDeck ->
              pageDeck = updatedDeck
              pageRepository.saveDeck(updatedDeck)
            },
            onActivationSettingsChange = { updatedSettings ->
              activationSettings = updatedSettings
              activationSettingsRepository.save(updatedSettings)
            },
            onDisplaySettingsChange = { updatedSettings ->
              displaySettings = updatedSettings
              displaySettingsRepository.save(updatedSettings)
            },
            onAmbientActiveChange = { shouldBeActive ->
              if (shouldBeActive) {
                applyActivationTransition(activationStateMachine.onManualEntered())
                isAmbientActive = true
              } else {
                applyActivationTransition(
                  activationStateMachine.onManualExited(nowMillis = SystemClock.elapsedRealtime()),
                )
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
    mediaRepository.start()
    applyAmbientWindowMode(requestedAmbientWindowMode)
  }

  override fun onStop() {
    applyAmbientWindowMode(false)
    mediaRepository.stop()
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
            settings = ambientDisplaySettings,
          )
      }

    if (isAmbientActive && ambientDisplaySettings.keepScreenOn) {
      window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
      window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    if (isAmbientActive) {
      controller?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      controller?.hide(WindowInsets.Type.systemBars())
    } else {
      controller?.show(WindowInsets.Type.systemBars())
    }
  }
}

private const val ACTIVATION_LOG_TAG = "FoldAmbientActivation"
