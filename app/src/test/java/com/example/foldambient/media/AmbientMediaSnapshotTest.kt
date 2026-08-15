package com.example.foldambient.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AmbientMediaSnapshotTest {
  @Test
  fun playingPositionInterpolatesFromUpdateTimeAndSpeed() {
    val snapshot =
      AmbientMediaSnapshot(
        isNotificationListenerEnabled = true,
        packageName = "music.app",
        playbackStatus = AmbientPlaybackStatus.Playing,
        positionMillis = 10_000L,
        positionUpdateRealtimeMillis = 100_000L,
        playbackSpeed = 1f,
        durationMillis = 60_000L,
      )

    assertEquals(15_000L, snapshot.estimatedPositionMillisAt(105_000L))
  }

  @Test
  fun playingPositionUsesPlaybackSpeed() {
    val snapshot =
      AmbientMediaSnapshot(
        isNotificationListenerEnabled = true,
        packageName = "music.app",
        playbackStatus = AmbientPlaybackStatus.Playing,
        positionMillis = 10_000L,
        positionUpdateRealtimeMillis = 100_000L,
        playbackSpeed = 2f,
        durationMillis = 60_000L,
      )

    assertEquals(20_000L, snapshot.estimatedPositionMillisAt(105_000L))
  }

  @Test
  fun interpolatedPositionIsClampedToDuration() {
    val snapshot =
      AmbientMediaSnapshot(
        isNotificationListenerEnabled = true,
        playbackStatus = AmbientPlaybackStatus.Playing,
        positionMillis = 58_000L,
        positionUpdateRealtimeMillis = 100_000L,
        playbackSpeed = 1f,
        durationMillis = 60_000L,
      )

    assertEquals(60_000L, snapshot.estimatedPositionMillisAt(110_000L))
  }

  @Test
  fun pausedPositionDoesNotInterpolate() {
    val snapshot =
      AmbientMediaSnapshot(
        isNotificationListenerEnabled = true,
        playbackStatus = AmbientPlaybackStatus.Paused,
        positionMillis = 10_000L,
        positionUpdateRealtimeMillis = 100_000L,
        playbackSpeed = 1f,
      )

    assertEquals(10_000L, snapshot.estimatedPositionMillisAt(105_000L))
  }

  @Test
  fun trackKeyIgnoresPlaybackPositionChanges() {
    val base =
      AmbientMediaSnapshot(
        isNotificationListenerEnabled = true,
        packageName = "music.app",
        title = "Night Drive",
        artist = "Fold Ambient",
        album = "Desk Mode",
        durationMillis = 214_000L,
        positionMillis = 10_000L,
      )
    val later = base.copy(positionMillis = 75_000L)

    assertEquals(base.trackKey, later.trackKey)
  }

  @Test
  fun trackKeyChangesWhenTrackIdentityChanges() {
    val base =
      AmbientMediaSnapshot(
        isNotificationListenerEnabled = true,
        packageName = "music.app",
        title = "Night Drive",
        artist = "Fold Ambient",
        album = "Desk Mode",
        durationMillis = 214_000L,
      )

    assertNotEquals(base.trackKey, base.copy(title = "Morning Drive").trackKey)
  }

  @Test
  fun trackKeyChangesWhenMeaningfulMetadataChanges() {
    val base =
      AmbientMediaSnapshot(
        isNotificationListenerEnabled = true,
        packageName = "music.app",
        title = "Night Drive",
        artist = "Fold Ambient",
        album = "Desk Mode",
        durationMillis = 214_000L,
      )

    assertNotEquals(base.trackKey, base.copy(artist = "Another Artist").trackKey)
    assertNotEquals(base.trackKey, base.copy(album = "Another Album").trackKey)
    assertNotEquals(base.trackKey, base.copy(durationMillis = 215_000L).trackKey)
  }

  @Test
  fun trackKeyNormalizesCaseAndPunctuation() {
    val base =
      AmbientMediaSnapshot(
        isNotificationListenerEnabled = true,
        packageName = "music.app",
        title = "Night Drive",
        artist = "Fold Ambient",
      )
    val formatted = base.copy(title = "night-drive", artist = "FOLD  AMBIENT")

    assertEquals(base.trackKey, formatted.trackKey)
  }
}
