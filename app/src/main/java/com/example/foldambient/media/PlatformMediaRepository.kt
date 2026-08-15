package com.example.foldambient.media

import android.content.ComponentName
import android.content.Context
import android.database.ContentObserver
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlatformMediaRepository(
  context: Context,
) : AmbientMediaRepository {
  private val applicationContext = context.applicationContext
  private val mediaSessionManager =
    applicationContext.getSystemService(MediaSessionManager::class.java)
  private val mainHandler = Handler(Looper.getMainLooper())
  private val notificationListenerComponent =
    ComponentName(applicationContext, FoldAmbientNotificationListenerService::class.java)
  private val mutableState =
    MutableStateFlow(
      AmbientMediaSnapshot(
        isNotificationListenerEnabled =
          applicationContext.isNotificationListenerEnabled(notificationListenerComponent),
      ),
    )
  private val activeSessionsChangedListener =
    MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
      updateActiveController(controllers.orEmpty())
    }
  private val notificationListenerObserver =
    object : ContentObserver(mainHandler) {
      override fun onChange(selfChange: Boolean) {
        refresh()
      }
    }

  private var isStarted = false
  private var isActiveSessionsListenerRegistered = false
  private var currentController: MediaController? = null
  private var currentControllerCallback: ActiveControllerCallback? = null

  override val state: StateFlow<AmbientMediaSnapshot> = mutableState.asStateFlow()

  override fun start() {
    if (isStarted) return
    isStarted = true
    runCatching {
      applicationContext.contentResolver.registerContentObserver(
        Settings.Secure.getUriFor(EnabledNotificationListenersSetting),
        false,
        notificationListenerObserver,
      )
    }
    refresh()
  }

  override fun stop() {
    if (!isStarted) return
    unregisterActiveSessionsListener()
    clearCurrentController()
    runCatching {
      applicationContext.contentResolver.unregisterContentObserver(notificationListenerObserver)
    }
    isStarted = false
  }

  override fun playPause() {
    val controller = currentController ?: return
    val state = controller.playbackState
    val controls = controller.transportControls
    when {
      state?.state == PlaybackState.STATE_PLAYING &&
        state.supports(PlaybackState.ACTION_PAUSE) -> controls.pause()
      state?.state != PlaybackState.STATE_PLAYING &&
        state.supports(PlaybackState.ACTION_PLAY) -> controls.play()
      state.supports(PlaybackState.ACTION_PLAY_PAUSE) -> controller.dispatchPlayPause()
    }
  }

  override fun previous() {
    currentController?.transportControls?.skipToPrevious()
  }

  override fun next() {
    currentController?.transportControls?.skipToNext()
  }

  override fun seekTo(positionMillis: Long) {
    val controller = currentController ?: return
    if (controller.playbackState.supports(PlaybackState.ACTION_SEEK_TO)) {
      controller.transportControls.seekTo(positionMillis)
      publishControllerState(
        controller = controller,
        overridePositionMillis = positionMillis,
        overridePositionUpdateRealtimeMillis = SystemClock.elapsedRealtime(),
      )
    }
  }

  private fun refresh() {
    val hasMediaAccess =
      applicationContext.isNotificationListenerEnabled(notificationListenerComponent)
    if (!hasMediaAccess) {
      unregisterActiveSessionsListener()
      clearCurrentController()
      mutableState.value = AmbientMediaSnapshot(isNotificationListenerEnabled = false)
      return
    }

    if (!ensureActiveSessionsListener()) {
      clearCurrentController()
      mutableState.value = AmbientMediaSnapshot(isNotificationListenerEnabled = false)
      return
    }

    val controllers =
      runCatching { mediaSessionManager.getActiveSessions(notificationListenerComponent) }
        .getOrDefault(emptyList())
    updateActiveController(controllers)
  }

  private fun ensureActiveSessionsListener(): Boolean {
    if (isActiveSessionsListenerRegistered) return true
    return runCatching {
      mediaSessionManager.addOnActiveSessionsChangedListener(
        activeSessionsChangedListener,
        notificationListenerComponent,
        mainHandler,
      )
    }.fold(
      onSuccess = {
        isActiveSessionsListenerRegistered = true
        true
      },
      onFailure = { false },
    )
  }

  private fun unregisterActiveSessionsListener() {
    if (!isActiveSessionsListenerRegistered) return
    runCatching {
      mediaSessionManager.removeOnActiveSessionsChangedListener(activeSessionsChangedListener)
    }
    isActiveSessionsListenerRegistered = false
  }

  private fun updateActiveController(controllers: List<MediaController>) {
    if (!applicationContext.isNotificationListenerEnabled(notificationListenerComponent)) {
      refresh()
      return
    }

    val nextController =
      controllers
        .sortedWith(
          compareByDescending<MediaController> {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
          }.thenByDescending {
            it.playbackState?.state == PlaybackState.STATE_BUFFERING
          },
        )
        .firstOrNull()

    setCurrentController(nextController)
  }

  private fun setCurrentController(controller: MediaController?) {
    if (controller == null) {
      clearCurrentController()
      mutableState.value = AmbientMediaSnapshot(isNotificationListenerEnabled = true)
      return
    }

    val currentToken = currentController?.sessionToken
    if (currentToken != controller.sessionToken) {
      clearCurrentController()
      val callback = ActiveControllerCallback(controller.sessionToken)
      currentController = controller
      currentControllerCallback = callback
      runCatching {
        controller.registerCallback(callback, mainHandler)
      }.onFailure {
        clearCurrentController()
        mutableState.value = AmbientMediaSnapshot(isNotificationListenerEnabled = true)
        return
      }
    } else {
      currentController = controller
    }

    publishControllerState(controller)
  }

  private fun clearCurrentController() {
    val controller = currentController
    val callback = currentControllerCallback
    if (controller != null && callback != null) {
      runCatching { controller.unregisterCallback(callback) }
    }
    currentController = null
    currentControllerCallback = null
  }

  private fun publishControllerState(
    controller: MediaController,
    overridePositionMillis: Long? = null,
    overridePositionUpdateRealtimeMillis: Long? = null,
  ) {
    mutableState.value =
      controller.toAmbientSnapshot(
        isNotificationListenerEnabled = true,
        overridePositionMillis = overridePositionMillis,
        overridePositionUpdateRealtimeMillis = overridePositionUpdateRealtimeMillis,
      )
  }

  private inner class ActiveControllerCallback(
    private val token: MediaSession.Token,
  ) : MediaController.Callback() {
    override fun onPlaybackStateChanged(state: PlaybackState?) {
      currentController
        ?.takeIf { controller -> controller.sessionToken == token }
        ?.let(::publishControllerState)
    }

    override fun onMetadataChanged(metadata: MediaMetadata?) {
      currentController
        ?.takeIf { controller -> controller.sessionToken == token }
        ?.let(::publishControllerState)
    }

    override fun onSessionDestroyed() {
      if (currentController?.sessionToken == token) {
        clearCurrentController()
        refresh()
      }
    }
  }
}

private fun Context.isNotificationListenerEnabled(componentName: ComponentName): Boolean {
  val enabledListeners =
    Settings.Secure.getString(contentResolver, EnabledNotificationListenersSetting)
      ?: return false
  return enabledListeners.split(':').any { flattenedComponent ->
    val enabledComponent = ComponentName.unflattenFromString(flattenedComponent) ?: return@any false
    enabledComponent.packageName == componentName.packageName &&
      enabledComponent.className == componentName.className
  }
}

private fun MediaController.toAmbientSnapshot(
  isNotificationListenerEnabled: Boolean,
  overridePositionMillis: Long? = null,
  overridePositionUpdateRealtimeMillis: Long? = null,
): AmbientMediaSnapshot {
  val metadata = metadata
  val playbackState = playbackState
  return AmbientMediaSnapshot(
    isNotificationListenerEnabled = isNotificationListenerEnabled,
    packageName = packageName,
    title = metadata?.text(MediaMetadata.METADATA_KEY_TITLE),
    artist = metadata?.text(MediaMetadata.METADATA_KEY_ARTIST),
    album = metadata?.text(MediaMetadata.METADATA_KEY_ALBUM),
    artwork = metadata?.artwork(),
    playbackStatus = playbackState.toAmbientPlaybackStatus(),
    positionMillis =
      overridePositionMillis
        ?: playbackState?.position?.takeIf { it >= 0L },
    positionUpdateRealtimeMillis =
      overridePositionUpdateRealtimeMillis
        ?: playbackState?.lastPositionUpdateTime?.takeIf { it > 0L },
    playbackSpeed = playbackState?.playbackSpeed ?: 0f,
    durationMillis = metadata?.longOrNull(MediaMetadata.METADATA_KEY_DURATION),
    supportedActions = playbackState.supportedAmbientActions(),
  )
}

private fun MediaMetadata.text(key: String): String? =
  getText(key)?.toString()?.takeIf { it.isNotBlank() }

private fun MediaMetadata.longOrNull(key: String): Long? =
  getLong(key).takeIf { it > 0L }

private fun MediaMetadata.artwork() =
  getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
    ?: getBitmap(MediaMetadata.METADATA_KEY_ART)

private fun PlaybackState?.toAmbientPlaybackStatus(): AmbientPlaybackStatus =
  when (this?.state) {
    PlaybackState.STATE_BUFFERING,
    PlaybackState.STATE_CONNECTING,
    PlaybackState.STATE_FAST_FORWARDING,
    PlaybackState.STATE_REWINDING,
    PlaybackState.STATE_SKIPPING_TO_NEXT,
    PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
    PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM -> AmbientPlaybackStatus.Buffering
    PlaybackState.STATE_PAUSED -> AmbientPlaybackStatus.Paused
    PlaybackState.STATE_PLAYING -> AmbientPlaybackStatus.Playing
    PlaybackState.STATE_NONE,
    PlaybackState.STATE_STOPPED -> AmbientPlaybackStatus.Stopped
    else -> AmbientPlaybackStatus.Unknown
  }

private fun PlaybackState?.supportedAmbientActions(): Set<AmbientMediaAction> {
  if (this == null) return emptySet()
  return buildSet {
    if (
      supports(PlaybackState.ACTION_PLAY) ||
      supports(PlaybackState.ACTION_PAUSE) ||
      supports(PlaybackState.ACTION_PLAY_PAUSE)
    ) {
      add(AmbientMediaAction.PlayPause)
    }
    if (supports(PlaybackState.ACTION_SKIP_TO_PREVIOUS)) {
      add(AmbientMediaAction.Previous)
    }
    if (supports(PlaybackState.ACTION_SKIP_TO_NEXT)) {
      add(AmbientMediaAction.Next)
    }
    if (supports(PlaybackState.ACTION_SEEK_TO)) {
      add(AmbientMediaAction.Seek)
    }
  }
}

private fun PlaybackState?.supports(action: Long): Boolean =
  this != null && actions and action != 0L

private fun MediaController.dispatchPlayPause() {
  dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
  dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
}

private const val EnabledNotificationListenersSetting = "enabled_notification_listeners"
