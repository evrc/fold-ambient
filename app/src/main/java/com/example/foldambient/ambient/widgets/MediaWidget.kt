package com.example.foldambient.ambient.widgets

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
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

    LaunchedEffect(repository) {
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
      modifier = modifier,
    )
  }
}

@Composable
private fun MediaWidgetContent(
  snapshot: AmbientMediaSnapshot,
  repository: AmbientMediaRepository,
  onOpenNotificationSettings: () -> Unit,
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
      )
    }
  }
}

@Composable
private fun ActiveMedia(
  snapshot: AmbientMediaSnapshot,
  repository: AmbientMediaRepository,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val artwork = snapshot.artwork
    if (artwork != null) {
      Image(
        bitmap = artwork.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier.size(96.dp),
        contentScale = ContentScale.Crop,
      )
    }
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = snapshot.title ?: "Unknown title",
        color = androidx.compose.ui.graphics.Color.White,
        style = MaterialTheme.typography.headlineSmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = snapshot.artist ?: snapshot.album ?: snapshot.packageName.orEmpty(),
        color = androidx.compose.ui.graphics.Color(0xFF9CA3AF),
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }

  MediaProgress(
    snapshot = snapshot,
    repository = repository,
  )

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center,
  ) {
    TextButton(
      onClick = repository::previous,
      enabled = AmbientMediaAction.Previous in snapshot.supportedActions,
    ) {
      Text("Prev")
    }
    TextButton(
      onClick = repository::playPause,
      enabled = AmbientMediaAction.PlayPause in snapshot.supportedActions,
    ) {
      Text(if (snapshot.playbackStatus == AmbientPlaybackStatus.Playing) "Pause" else "Play")
    }
    TextButton(
      onClick = repository::next,
      enabled = AmbientMediaAction.Next in snapshot.supportedActions,
    ) {
      Text("Next")
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
      color = androidx.compose.ui.graphics.Color(0xFF9CA3AF),
      style = MaterialTheme.typography.labelMedium,
    )
    Slider(
      value = sliderValue,
      onValueChange = { pendingSeek = it },
      valueRange = 0f..duration.toFloat(),
      enabled = canSeek,
      onValueChangeFinished = {
        pendingSeek?.roundToLong()?.let(repository::seekTo)
        pendingSeek = null
      },
      modifier = Modifier
        .weight(1f)
        .height(32.dp),
    )
    Text(
      text = duration.formatDuration(),
      color = androidx.compose.ui.graphics.Color(0xFF9CA3AF),
      style = MaterialTheme.typography.labelMedium,
    )
  }
}

private fun Long.formatDuration(): String {
  val totalSeconds = (this / 1_000L).coerceAtLeast(0L)
  val minutes = totalSeconds / 60L
  val seconds = totalSeconds % 60L
  return "$minutes:${seconds.toString().padStart(2, '0')}"
}
