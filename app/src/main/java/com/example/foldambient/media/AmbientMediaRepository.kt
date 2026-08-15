package com.example.foldambient.media

import android.graphics.Bitmap

data class AmbientMediaSnapshot(
  val isNotificationListenerEnabled: Boolean,
  val packageName: String? = null,
  val title: String? = null,
  val artist: String? = null,
  val album: String? = null,
  val artwork: Bitmap? = null,
  val playbackStatus: AmbientPlaybackStatus = AmbientPlaybackStatus.Unknown,
  val positionMillis: Long? = null,
  val durationMillis: Long? = null,
  val supportedActions: Set<AmbientMediaAction> = emptySet(),
) {
  val hasActiveSession: Boolean
    get() = packageName != null
}

enum class AmbientPlaybackStatus {
  Buffering,
  Paused,
  Playing,
  Stopped,
  Unknown,
}

enum class AmbientMediaAction {
  Next,
  PlayPause,
  Previous,
  Seek,
}

interface AmbientMediaRepository {
  fun snapshot(): AmbientMediaSnapshot
  fun playPause()
  fun previous()
  fun next()
  fun seekTo(positionMillis: Long)
}
