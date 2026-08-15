package com.example.foldambient.ambient.widgets

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.foldambient.ambient.AmbientWidget
import com.example.foldambient.ambient.WidgetInstance
import com.example.foldambient.lyrics.AmbientLyricLine
import com.example.foldambient.lyrics.AmbientLyrics
import com.example.foldambient.lyrics.AmbientLyricsLookupResult
import com.example.foldambient.lyrics.AmbientLyricsSource
import com.example.foldambient.lyrics.LrcLibLyricsRepository
import com.example.foldambient.lyrics.LyricsTrackQuery
import com.example.foldambient.media.AmbientMediaRepository
import com.example.foldambient.media.AmbientMediaSnapshot
import com.example.foldambient.media.AmbientPlaybackStatus
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class LyricsWidget(
  private val mediaRepository: AmbientMediaRepository,
) : AmbientWidget {
  override val type = "lyrics.current"
  override val displayName = "Lyrics"

  @Composable
  override fun Content(instance: WidgetInstance, modifier: Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lyricsRepository = remember { LrcLibLyricsRepository() }
    val sourceSnapshot by mediaRepository.state.collectAsStateWithLifecycle()
    val snapshot =
      rememberEstimatedMediaSnapshot(
        snapshot = sourceSnapshot,
        refreshMillis = LyricsPositionRefreshMillis,
      )
    var lookupResult by remember { mutableStateOf<AmbientLyricsLookupResult?>(null) }
    val query = LyricsTrackQuery.from(sourceSnapshot)

    StartedWidgetEffect(query?.cacheKey, lyricsRepository) {
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
      onSeekTo = mediaRepository::seekTo,
      modifier = modifier,
    )
  }

  @Composable
  override fun PreviewContent(instance: WidgetInstance, modifier: Modifier) {
    LyricsWidgetContent(
      snapshot = PreviewLyricsMediaSnapshot,
      lookupResult = PreviewLyricsResult,
      onOpenNotificationSettings = {},
      onSeekTo = {},
      modifier = modifier,
    )
  }
}

private val PreviewLyricsMediaSnapshot =
  AmbientMediaSnapshot(
    isNotificationListenerEnabled = true,
    packageName = "com.example.music",
    title = "Night Drive",
    artist = "Fold Ambient",
    playbackStatus = AmbientPlaybackStatus.Playing,
    positionMillis = 74_000L,
    durationMillis = 214_000L,
  )

private val PreviewLyricsResult =
  AmbientLyricsLookupResult.Found(
    AmbientLyrics(
      trackName = "Night Drive",
      artistName = "Fold Ambient",
      albumName = "Desk Mode",
      durationMillis = 214_000L,
      source = AmbientLyricsSource.Synced,
      lines =
        listOf(
          AmbientLyricLine(startMillis = 65_000L, text = "city lights soften"),
          AmbientLyricLine(startMillis = 74_000L, text = "the night keeps time"),
          AmbientLyricLine(startMillis = 83_000L, text = "and everything settles"),
        ),
    ),
  )

@Composable
private fun LyricsWidgetContent(
  snapshot: AmbientMediaSnapshot,
  lookupResult: AmbientLyricsLookupResult?,
  onOpenNotificationSettings: () -> Unit,
  onSeekTo: (Long) -> Unit,
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
          onSeekTo = onSeekTo,
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
  onSeekTo: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (lyrics.isSynced) {
    SyncedLyrics(
      lines = lyrics.lines,
      positionMillis = positionMillis,
      onSeekTo = onSeekTo,
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
  onSeekTo: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  val activeIndex =
    lines.indexOfLast { line ->
      val startMillis = line.startMillis
      startMillis != null && positionMillis != null && startMillis <= positionMillis
    }.coerceAtLeast(0)
  val listState = rememberLazyListState()

  LaunchedEffect(activeIndex) {
    listState.animateScrollToItem((activeIndex - SyncedLyricLeadRows).coerceAtLeast(0))
  }

  LazyColumn(
    state = listState,
    modifier = modifier,
    contentPadding = PaddingValues(vertical = 28.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    itemsIndexed(
      items = lines,
      key = { index, line -> "${line.startMillis}-${line.text}-$index" },
    ) { index, line ->
      SyncedLyricLine(
        line = line,
        isActive = index == activeIndex,
        onSeekTo = onSeekTo,
      )
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

@Composable
private fun SyncedLyricLine(
  line: AmbientLyricLine,
  isActive: Boolean,
  onSeekTo: (Long) -> Unit,
) {
  val alpha by animateFloatAsState(
    targetValue = if (isActive) 1f else 0.58f,
    animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
    label = "lyricLineAlpha",
  )
  val scale by animateFloatAsState(
    targetValue = if (isActive) 1f else 0.94f,
    animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
    label = "lyricLineScale",
  )

  LyricLine(
    text = line.text,
    isActive = isActive,
    modifier =
      Modifier
        .seekToLyricLine(line, onSeekTo)
        .graphicsLayer {
          this.alpha = alpha
          scaleX = scale
          scaleY = scale
        },
  )
}

private fun Modifier.seekToLyricLine(
  line: AmbientLyricLine,
  onSeekTo: (Long) -> Unit,
): Modifier {
  val startMillis = line.startMillis ?: return this
  return pointerInput(startMillis) {
    detectTapGestures(
      onTap = { onSeekTo(startMillis) },
    )
  }
}

private const val SyncedLyricLeadRows = 2
private const val LyricsPositionRefreshMillis = 500L
