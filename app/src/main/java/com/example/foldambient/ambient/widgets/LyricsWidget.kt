package com.example.foldambient.ambient.widgets

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.foldambient.ambient.AmbientWidget
import com.example.foldambient.ambient.WidgetInstance
import com.example.foldambient.lyrics.AmbientLyricLine
import com.example.foldambient.lyrics.AmbientLyrics
import com.example.foldambient.lyrics.AmbientLyricsLookupResult
import com.example.foldambient.lyrics.LrcLibLyricsRepository
import com.example.foldambient.lyrics.LyricsTrackQuery
import com.example.foldambient.media.AmbientMediaSnapshot
import com.example.foldambient.media.AmbientPlaybackStatus
import com.example.foldambient.media.PlatformMediaRepository
import kotlinx.coroutines.delay

class LyricsWidget : AmbientWidget {
  override val type = "lyrics.current"
  override val displayName = "Lyrics"

  @Composable
  override fun Content(instance: WidgetInstance, modifier: Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mediaRepository =
      remember(context) {
        PlatformMediaRepository(context.applicationContext)
      }
    val lyricsRepository = remember { LrcLibLyricsRepository() }
    var snapshot by remember(mediaRepository) { mutableStateOf(mediaRepository.snapshot()) }
    var lookupResult by remember { mutableStateOf<AmbientLyricsLookupResult?>(null) }
    val query = LyricsTrackQuery.from(snapshot)

    LaunchedEffect(mediaRepository) {
      while (true) {
        snapshot = mediaRepository.snapshot()
        delay(if (snapshot.playbackStatus == AmbientPlaybackStatus.Playing) 500L else 1_500L)
      }
    }

    LaunchedEffect(query?.cacheKey) {
      lookupResult = null
      if (query != null) {
        lookupResult = lyricsRepository.lyricsFor(query)
      }
    }

    LyricsWidgetContent(
      snapshot = snapshot,
      lookupResult = lookupResult,
      onOpenNotificationSettings = {
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
      },
      modifier = modifier,
    )
  }
}

@Composable
private fun LyricsWidgetContent(
  snapshot: AmbientMediaSnapshot,
  lookupResult: AmbientLyricsLookupResult?,
  onOpenNotificationSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    WidgetLabel("Lyrics")
    when {
      !snapshot.isNotificationListenerEnabled -> {
        WidgetValue("Lyrics access")
        TextButton(onClick = onOpenNotificationSettings) {
          Text("Enable")
        }
      }
      !snapshot.hasActiveSession -> WidgetValue("No media")
      snapshot.title.isNullOrBlank() || snapshot.artist.isNullOrBlank() -> WidgetValue("No track")
      lookupResult == null -> WidgetValue("Finding lyrics")
      lookupResult is AmbientLyricsLookupResult.Found ->
        LyricsDisplay(
          lyrics = lookupResult.lyrics,
          positionMillis = snapshot.positionMillis,
          modifier = Modifier.fillMaxSize(),
        )
      lookupResult is AmbientLyricsLookupResult.NotFound -> WidgetValue("No lyrics")
      lookupResult is AmbientLyricsLookupResult.Unavailable -> WidgetValue("Lyrics unavailable")
    }
  }
}

@Composable
private fun LyricsDisplay(
  lyrics: AmbientLyrics,
  positionMillis: Long?,
  modifier: Modifier = Modifier,
) {
  if (lyrics.isSynced) {
    SyncedLyrics(
      lines = lyrics.lines,
      positionMillis = positionMillis,
      modifier = modifier,
    )
  } else {
    PlainLyrics(
      lines = lyrics.lines,
      modifier = modifier,
    )
  }
}

@Composable
private fun SyncedLyrics(
  lines: List<AmbientLyricLine>,
  positionMillis: Long?,
  modifier: Modifier = Modifier,
) {
  val activeIndex =
    lines.indexOfLast { line ->
      val startMillis = line.startMillis
      startMillis != null && positionMillis != null && startMillis <= positionMillis
    }.coerceAtLeast(0)
  val animatedIndex by animateFloatAsState(
    targetValue = activeIndex.toFloat(),
    animationSpec =
      tween(
        durationMillis = 850,
        easing = FastOutSlowInEasing,
      ),
    label = "activeLyricIndex",
  )

  BoxWithConstraints(modifier = modifier) {
    val density = LocalDensity.current
    val lineStep = lyricLineStep(maxHeight)
    val lineStepPx = with(density) { lineStep.toPx() }
    val visibleRange = (activeIndex - VisibleSyncedLyricRadius)..(activeIndex + VisibleSyncedLyricRadius)

    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      visibleRange.forEach { index ->
        val line = lines.getOrNull(index) ?: return@forEach
        val distance = index - animatedIndex
        val absoluteDistance = kotlin.math.abs(distance)
        val alpha = (1f - absoluteDistance * 0.24f).coerceIn(0f, 1f)
        val scale = (1f - absoluteDistance * 0.055f).coerceIn(0.88f, 1f)

        LyricLine(
          text = line.text,
          isActive = index == activeIndex,
          modifier =
            Modifier
              .align(Alignment.Center)
              .graphicsLayer {
                translationY = distance * lineStepPx
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
              },
        )
      }
    }
  }
}

@Composable
private fun PlainLyrics(
  lines: List<AmbientLyricLine>,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(10.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    itemsIndexed(lines) { _, line ->
      LyricLine(
        text = line.text,
        isActive = false,
      )
    }
  }
}

@Composable
private fun LyricLine(
  text: String,
  isActive: Boolean,
  modifier: Modifier = Modifier,
) {
  Text(
    text = text,
    color = if (isActive) Color.White else Color(0xFF9CA3AF),
    style = if (isActive) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
    textAlign = TextAlign.Center,
    maxLines = if (isActive) 3 else 2,
    overflow = TextOverflow.Ellipsis,
    modifier = modifier,
  )
}

private fun lyricLineStep(maxHeight: Dp): Dp =
  when {
    maxHeight < 180.dp -> 40.dp
    maxHeight < 280.dp -> 52.dp
    else -> 66.dp
  }

private const val VisibleSyncedLyricRadius = 3
