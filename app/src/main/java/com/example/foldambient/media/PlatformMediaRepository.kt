package com.example.foldambient.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent

class PlatformMediaRepository(
  context: Context,
) : AmbientMediaRepository {
  private val applicationContext = context.applicationContext
  private val mediaSessionManager =
    applicationContext.getSystemService(MediaSessionManager::class.java)

  override fun snapshot(): AmbientMediaSnapshot {
    val isListenerEnabled = applicationContext.isNotificationListenerEnabled()
    if (!isListenerEnabled) {
      return AmbientMediaSnapshot(isNotificationListenerEnabled = false)
    }

    val controller = activeController()
      ?: return AmbientMediaSnapshot(isNotificationListenerEnabled = true)
    val metadata = controller.metadata
    val playbackState = controller.playbackState

    return AmbientMediaSnapshot(
      isNotificationListenerEnabled = true,
      packageName = controller.packageName,
      title = metadata?.text(MediaMetadata.METADATA_KEY_TITLE),
      artist = metadata?.text(MediaMetadata.METADATA_KEY_ARTIST),
      album = metadata?.text(MediaMetadata.METADATA_KEY_ALBUM),
      artwork = metadata?.artwork(),
      playbackStatus = playbackState.toAmbientPlaybackStatus(),
      positionMillis = playbackState?.currentPositionMillis(),
      durationMillis = metadata?.longOrNull(MediaMetadata.METADATA_KEY_DURATION),
      supportedActions = playbackState.supportedAmbientActions(),
    )
  }

  override fun playPause() {
    val controller = activeController() ?: return
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
    activeController()?.transportControls?.skipToPrevious()
  }

  override fun next() {
    activeController()?.transportControls?.skipToNext()
  }

  override fun seekTo(positionMillis: Long) {
    val state = activeController()?.playbackState ?: return
    if (state.supports(PlaybackState.ACTION_SEEK_TO)) {
      activeController()?.transportControls?.seekTo(positionMillis)
    }
  }

  private fun activeController(): MediaController? =
    runCatching {
      mediaSessionManager.getActiveSessions(notificationListenerComponent())
    }.getOrDefault(emptyList())
      .sortedWith(compareByDescending<MediaController> { it.playbackState?.state == PlaybackState.STATE_PLAYING })
      .firstOrNull()

  private fun notificationListenerComponent(): ComponentName =
    ComponentName(applicationContext, FoldAmbientNotificationListenerService::class.java)
}

private fun Context.isNotificationListenerEnabled(): Boolean {
  val enabledListeners =
    Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
      ?: return false
  return enabledListeners.split(':').any { component ->
    component.contains(packageName, ignoreCase = true)
  }
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

private fun PlaybackState.currentPositionMillis(): Long? {
  if (position < 0L) return null
  if (state != PlaybackState.STATE_PLAYING || lastPositionUpdateTime <= 0L) return position

  val elapsed = SystemClock.elapsedRealtime() - lastPositionUpdateTime
  return (position + elapsed * playbackSpeed).toLong().coerceAtLeast(0L)
}

private fun MediaController.dispatchPlayPause() {
  dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
  dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
}
