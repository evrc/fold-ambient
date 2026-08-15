package com.example.foldambient.ambient.widgets

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.foldambient.media.AmbientMediaSnapshot
import com.example.foldambient.media.AmbientPlaybackStatus
import kotlinx.coroutines.delay

@Composable
internal fun rememberEstimatedMediaSnapshot(
  snapshot: AmbientMediaSnapshot,
  refreshMillis: Long,
): AmbientMediaSnapshot {
  var nowRealtimeMillis by remember(
    snapshot.trackKey,
    snapshot.playbackStatus,
    snapshot.positionMillis,
    snapshot.positionUpdateRealtimeMillis,
  ) {
    mutableLongStateOf(SystemClock.elapsedRealtime())
  }

  StartedWidgetEffect(
    snapshot.trackKey,
    snapshot.playbackStatus,
    snapshot.positionMillis,
    snapshot.positionUpdateRealtimeMillis,
    refreshMillis,
  ) {
    nowRealtimeMillis = SystemClock.elapsedRealtime()
    while (snapshot.playbackStatus == AmbientPlaybackStatus.Playing && snapshot.positionMillis != null) {
      delay(refreshMillis)
      nowRealtimeMillis = SystemClock.elapsedRealtime()
    }
  }

  return snapshot.withEstimatedPositionAt(nowRealtimeMillis)
}
