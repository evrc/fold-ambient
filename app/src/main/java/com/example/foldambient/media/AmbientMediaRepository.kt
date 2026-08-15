package com.example.foldambient.media

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AmbientMediaSnapshot(
  val isNotificationListenerEnabled: Boolean,
  val packageName: String? = null,
  val title: String? = null,
  val artist: String? = null,
  val album: String? = null,
  val artwork: Bitmap? = null,
  val playbackStatus: AmbientPlaybackStatus = AmbientPlaybackStatus.Unknown,
  val positionMillis: Long? = null,
  val positionUpdateRealtimeMillis: Long? = null,
  val playbackSpeed: Float = 0f,
  val durationMillis: Long? = null,
  val supportedActions: Set<AmbientMediaAction> = emptySet(),
) {
  val hasActiveSession: Boolean
    get() = packageName != null

  val trackKey: String?
    get() {
      val title = title?.takeIf { it.isNotBlank() } ?: return null
      val artist = artist?.takeIf { it.isNotBlank() } ?: return null
      return listOf(
        packageName.orEmpty().normalizedMediaKeyPart(),
        title.normalizedMediaKeyPart(),
        artist.normalizedMediaKeyPart(),
        album.orEmpty().normalizedMediaKeyPart(),
        durationMillis?.toString().orEmpty(),
      ).joinToString("|")
    }

  fun estimatedPositionMillisAt(elapsedRealtimeMillis: Long): Long? {
    val basePosition = positionMillis ?: return null
    val estimatedPosition =
      if (
        playbackStatus == AmbientPlaybackStatus.Playing &&
        playbackSpeed > 0f &&
        positionUpdateRealtimeMillis != null
      ) {
        val elapsed = (elapsedRealtimeMillis - positionUpdateRealtimeMillis).coerceAtLeast(0L)
        basePosition + (elapsed * playbackSpeed).toLong()
      } else {
        basePosition
      }

    return durationMillis
      ?.takeIf { it > 0L }
      ?.let { duration -> estimatedPosition.coerceIn(0L, duration) }
      ?: estimatedPosition.coerceAtLeast(0L)
  }

  fun withEstimatedPositionAt(elapsedRealtimeMillis: Long): AmbientMediaSnapshot =
    copy(positionMillis = estimatedPositionMillisAt(elapsedRealtimeMillis))
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
  val state: StateFlow<AmbientMediaSnapshot>
  fun start() = Unit
  fun stop() = Unit
  fun playPause()
  fun previous()
  fun next()
  fun seekTo(positionMillis: Long)
}

class StaticAmbientMediaRepository(
  initialState: AmbientMediaSnapshot = AmbientMediaSnapshot(isNotificationListenerEnabled = false),
) : AmbientMediaRepository {
  private val stateFlow = MutableStateFlow(initialState)

  override val state: StateFlow<AmbientMediaSnapshot> = stateFlow
  override fun playPause() = Unit
  override fun previous() = Unit
  override fun next() = Unit
  override fun seekTo(positionMillis: Long) = Unit
}

private fun String.normalizedMediaKeyPart(): String =
  lowercase()
    .replace(NonKeyCharacters, " ")
    .replace(RepeatedWhitespace, " ")
    .trim()

private val NonKeyCharacters = Regex("""[^\p{L}\p{Nd}]+""")
private val RepeatedWhitespace = Regex("""\s+""")
