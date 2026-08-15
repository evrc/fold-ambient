package com.example.foldambient.ambient.widgets

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.foldambient.ambient.AmbientWidget
import com.example.foldambient.ambient.WidgetInstance
import com.example.foldambient.media.AmbientMediaAction
import com.example.foldambient.media.AmbientMediaRepository
import com.example.foldambient.media.AmbientMediaSnapshot
import com.example.foldambient.media.AmbientPlaybackStatus
import com.example.foldambient.media.PlatformMediaRepository
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

class MediaWidget : AmbientWidget {
  override val type = "media.playback"
  override val displayName = "Media"

  @Composable
  override fun Content(instance: WidgetInstance, modifier: Modifier) {
    val context = LocalContext.current
    val repository =
      remember(context) {
        PlatformMediaRepository(context.applicationContext)
      }
    var snapshot by remember(repository) { mutableStateOf(repository.snapshot()) }

    StartedWidgetEffect(repository) {
      while (true) {
        snapshot = repository.snapshot()
        delay(if (snapshot.playbackStatus == AmbientPlaybackStatus.Playing) 1_000L else 2_500L)
      }
    }

    MediaWidgetContent(
      snapshot = snapshot,
      repository = repository,
      onOpenNotificationSettings = {
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
      },
      onOpenMediaApp = { packageName ->
        context.packageManager.getLaunchIntentForPackage(packageName)
          ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          ?.let(context::startActivity)
      },
      modifier = modifier,
    )
  }

  @Composable
  override fun PreviewContent(instance: WidgetInstance, modifier: Modifier) {
    MediaWidgetContent(
      snapshot = PreviewMediaSnapshot,
      repository = PreviewMediaRepository,
      onOpenNotificationSettings = {},
      onOpenMediaApp = {},
      modifier = modifier,
    )
  }
}

private val PreviewMediaSnapshot =
  AmbientMediaSnapshot(
    isNotificationListenerEnabled = true,
    packageName = "com.example.music",
    title = "Night Drive",
    artist = "Fold Ambient",
    album = "Desk Mode",
    playbackStatus = AmbientPlaybackStatus.Playing,
    positionMillis = 74_000L,
    durationMillis = 214_000L,
    supportedActions =
      setOf(
        AmbientMediaAction.Previous,
        AmbientMediaAction.PlayPause,
        AmbientMediaAction.Next,
        AmbientMediaAction.Seek,
      ),
  )

private object PreviewMediaRepository : AmbientMediaRepository {
  override fun snapshot(): AmbientMediaSnapshot = PreviewMediaSnapshot
  override fun playPause() = Unit
  override fun previous() = Unit
  override fun next() = Unit
  override fun seekTo(positionMillis: Long) = Unit
}

@Composable
private fun MediaWidgetContent(
  snapshot: AmbientMediaSnapshot,
  repository: AmbientMediaRepository,
  onOpenNotificationSettings: () -> Unit,
  onOpenMediaApp: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
  ) {
    WidgetLabel("Media")
    when {
      !snapshot.isNotificationListenerEnabled -> {
        WidgetValue("Media access")
        TextButton(onClick = onOpenNotificationSettings) {
          Text("Enable")
        }
      }
      !snapshot.hasActiveSession -> {
        WidgetValue("No media")
      }
      else -> ActiveMedia(
        snapshot = snapshot,
        repository = repository,
        onOpenMediaApp = onOpenMediaApp,
      )
    }
  }
}

@Composable
private fun ActiveMedia(
  snapshot: AmbientMediaSnapshot,
  repository: AmbientMediaRepository,
  onOpenMediaApp: (String) -> Unit,
) {
  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val coverSize = mediaCoverSize(maxWidth = maxWidth, maxHeight = maxHeight)
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      MediaArtwork(
        snapshot = snapshot,
        coverSize = coverSize,
        onOpenMediaApp = onOpenMediaApp,
      )

      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = snapshot.title ?: "Unknown title",
          color = Color.White,
          style = MaterialTheme.typography.headlineSmall,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.openMediaAppOnDoubleTap(snapshot.packageName, onOpenMediaApp),
        )
        Text(
          text = snapshot.artist ?: snapshot.album ?: snapshot.packageName.orEmpty(),
          color = Color(0xFF9CA3AF),
          style = MaterialTheme.typography.bodyLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      MediaProgress(
        snapshot = snapshot,
        repository = repository,
      )

      MediaControls(
        snapshot = snapshot,
        repository = repository,
      )
    }
  }
}

@Composable
private fun MediaArtwork(
  snapshot: AmbientMediaSnapshot,
  coverSize: Dp,
  onOpenMediaApp: (String) -> Unit,
) {
  val artwork = snapshot.artwork
  val modifier = Modifier
    .size(coverSize)
    .clip(RoundedCornerShape(8.dp))
    .openMediaAppOnDoubleTap(snapshot.packageName, onOpenMediaApp)
  if (artwork != null) {
    Image(
      bitmap = artwork.asImageBitmap(),
      contentDescription = null,
      modifier = modifier,
      contentScale = ContentScale.Crop,
    )
  } else {
    Box(
      modifier = modifier,
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "Music",
        color = Color(0xFF9CA3AF),
        style = MaterialTheme.typography.titleMedium,
      )
    }
  }
}

@Composable
private fun MediaProgress(
  snapshot: AmbientMediaSnapshot,
  repository: AmbientMediaRepository,
) {
  val duration = snapshot.durationMillis ?: return
  val position = snapshot.positionMillis ?: return
  if (duration <= 0L) return

  var pendingSeek by remember(snapshot.packageName, snapshot.title, position) {
    mutableStateOf<Float?>(null)
  }
  val sliderValue = pendingSeek ?: position.coerceIn(0L, duration).toFloat()
  val canSeek = AmbientMediaAction.Seek in snapshot.supportedActions

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = position.formatDuration(),
      color = Color(0xFF9CA3AF),
      style = MaterialTheme.typography.labelMedium,
    )
    MediaProgressTrack(
      position = sliderValue,
      duration = duration.toFloat(),
      enabled = canSeek,
      onSeek = { seekPosition ->
        pendingSeek = seekPosition
        repository.seekTo(seekPosition.roundToLong())
      },
      modifier = Modifier.weight(1f),
    )
    Text(
      text = duration.formatDuration(),
      color = Color(0xFF9CA3AF),
      style = MaterialTheme.typography.labelMedium,
    )
  }
}

@Composable
private fun MediaProgressTrack(
  position: Float,
  duration: Float,
  enabled: Boolean,
  onSeek: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  val progress = (position / duration).coerceIn(0f, 1f)
  val trackModifier =
    if (enabled) {
      modifier.pointerInput(duration) {
        detectTapGestures { offset ->
          val tappedProgress = (offset.x / size.width).coerceIn(0f, 1f)
          onSeek(tappedProgress * duration)
        }
      }
    } else {
      modifier
    }

  Canvas(
    modifier = trackModifier
      .fillMaxWidth()
      .height(26.dp),
  ) {
    val centerY = size.height / 2f
    val trackWidth = 2.dp.toPx()
    val headRadius = 4.dp.toPx()
    val headX = size.width * progress

    drawLine(
      color = Color(0xFF374151),
      start = Offset(0f, centerY),
      end = Offset(size.width, centerY),
      strokeWidth = trackWidth,
      cap = StrokeCap.Round,
    )
    drawLine(
      color = if (enabled) Color.White else Color(0xFF6B7280),
      start = Offset(0f, centerY),
      end = Offset(headX, centerY),
      strokeWidth = trackWidth,
      cap = StrokeCap.Round,
    )
    drawCircle(
      color = if (enabled) Color.White else Color(0xFF6B7280),
      radius = headRadius,
      center = Offset(headX, centerY),
    )
  }
}

@Composable
private fun MediaControls(
  snapshot: AmbientMediaSnapshot,
  repository: AmbientMediaRepository,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    IconButton(
      onClick = repository::previous,
      enabled = AmbientMediaAction.Previous in snapshot.supportedActions,
    ) {
      MediaControlIcon(kind = MediaControlIconKind.Previous)
    }
    IconButton(
      onClick = repository::playPause,
      enabled = AmbientMediaAction.PlayPause in snapshot.supportedActions,
    ) {
      MediaControlIcon(
        kind =
          if (snapshot.playbackStatus == AmbientPlaybackStatus.Playing) {
            MediaControlIconKind.Pause
          } else {
            MediaControlIconKind.Play
          },
      )
    }
    IconButton(
      onClick = repository::next,
      enabled = AmbientMediaAction.Next in snapshot.supportedActions,
    ) {
      MediaControlIcon(kind = MediaControlIconKind.Next)
    }
  }
}

@Composable
private fun MediaControlIcon(kind: MediaControlIconKind) {
  Canvas(modifier = Modifier.size(28.dp)) {
    val iconColor = Color.White
    when (kind) {
      MediaControlIconKind.Play -> {
        val path =
          Path().apply {
            moveTo(size.width * 0.36f, size.height * 0.24f)
            lineTo(size.width * 0.36f, size.height * 0.76f)
            lineTo(size.width * 0.76f, size.height * 0.50f)
            close()
          }
        drawPath(path = path, color = iconColor)
      }
      MediaControlIconKind.Pause -> {
        val strokeWidth = 4.dp.toPx()
        drawLine(
          color = iconColor,
          start = Offset(size.width * 0.40f, size.height * 0.25f),
          end = Offset(size.width * 0.40f, size.height * 0.75f),
          strokeWidth = strokeWidth,
          cap = StrokeCap.Round,
        )
        drawLine(
          color = iconColor,
          start = Offset(size.width * 0.60f, size.height * 0.25f),
          end = Offset(size.width * 0.60f, size.height * 0.75f),
          strokeWidth = strokeWidth,
          cap = StrokeCap.Round,
        )
      }
      MediaControlIconKind.Previous -> {
        drawLine(
          color = iconColor,
          start = Offset(size.width * 0.22f, size.height * 0.28f),
          end = Offset(size.width * 0.22f, size.height * 0.72f),
          strokeWidth = 3.dp.toPx(),
          cap = StrokeCap.Round,
        )
        drawPath(path = skipPath(isNext = false), color = iconColor)
      }
      MediaControlIconKind.Next -> {
        drawLine(
          color = iconColor,
          start = Offset(size.width * 0.78f, size.height * 0.28f),
          end = Offset(size.width * 0.78f, size.height * 0.72f),
          strokeWidth = 3.dp.toPx(),
          cap = StrokeCap.Round,
        )
        drawPath(path = skipPath(isNext = true), color = iconColor)
      }
    }
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.skipPath(isNext: Boolean): Path =
  Path().apply {
    if (isNext) {
      moveTo(size.width * 0.28f, size.height * 0.26f)
      lineTo(size.width * 0.28f, size.height * 0.74f)
      lineTo(size.width * 0.66f, size.height * 0.50f)
    } else {
      moveTo(size.width * 0.72f, size.height * 0.26f)
      lineTo(size.width * 0.72f, size.height * 0.74f)
      lineTo(size.width * 0.34f, size.height * 0.50f)
    }
    close()
  }

private enum class MediaControlIconKind {
  Next,
  Pause,
  Play,
  Previous,
}

private fun Modifier.openMediaAppOnDoubleTap(
  packageName: String?,
  onOpenMediaApp: (String) -> Unit,
): Modifier {
  if (packageName.isNullOrBlank()) return this
  return pointerInput(packageName) {
    detectTapGestures(
      onDoubleTap = { onOpenMediaApp(packageName) },
    )
  }
}

private fun mediaCoverSize(maxWidth: Dp, maxHeight: Dp): Dp {
  val byHeight = maxHeight * 0.42f
  val byWidth = maxWidth * 0.58f
  return minOf(byHeight, byWidth, 220.dp).coerceAtLeast(112.dp)
}

private fun Long.formatDuration(): String {
  val totalSeconds = (this / 1_000L).coerceAtLeast(0L)
  val minutes = totalSeconds / 60L
  val seconds = totalSeconds % 60L
  return "$minutes:${seconds.toString().padStart(2, '0')}"
}
