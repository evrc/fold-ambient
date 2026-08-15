package com.example.foldambient.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsParsingTest {
  @Test
  fun normalSynchronizedLyricsParseTimestampsAndText() {
    val lines =
      parseSyncedLyricLines(
        """
        [00:10.00]first synthetic line
        [00:12.50]second synthetic line
        """.trimIndent(),
      )

    assertEquals(
      listOf(
        AmbientLyricLine(startMillis = 10_000L, text = "first synthetic line"),
        AmbientLyricLine(startMillis = 12_500L, text = "second synthetic line"),
      ),
      lines,
    )
  }

  @Test
  fun multipleTimestampsCreateMultipleLinesWithSameText() {
    val lines = parseSyncedLyricLines("[00:01.00][00:02.00]repeatable phrase")

    assertEquals(
      listOf(
        AmbientLyricLine(startMillis = 1_000L, text = "repeatable phrase"),
        AmbientLyricLine(startMillis = 2_000L, text = "repeatable phrase"),
      ),
      lines,
    )
  }

  @Test
  fun fractionalTimestampsArePaddedOrTruncatedToMillis() {
    val lines =
      parseSyncedLyricLines(
        """
        [00:01.5]half second
        [00:02.050]fifty millis
        """.trimIndent(),
      )

    assertEquals(1_500L, lines[0].startMillis)
    assertEquals(2_050L, lines[1].startMillis)
  }

  @Test
  fun timestampsWithMoreThanThreeFractionDigitsAreCurrentlyIgnored() {
    assertTrue(parseSyncedLyricLines("[00:03.1234]too precise").isEmpty())
  }

  @Test
  fun metadataEmptyAndMalformedLinesAreIgnored() {
    val lines =
      parseSyncedLyricLines(
        """
        [ar:Synthetic Artist]

        not timestamped
        [bad]bad line
        [00:04.00]valid line
        """.trimIndent(),
      )

    assertEquals(listOf(AmbientLyricLine(startMillis = 4_000L, text = "valid line")), lines)
  }

  @Test
  fun duplicateTimestampsArePreservedInInputOrder() {
    val lines =
      parseSyncedLyricLines(
        """
        [00:05.00]first duplicate
        [00:05.00]second duplicate
        """.trimIndent(),
      )

    assertEquals("first duplicate", lines[0].text)
    assertEquals("second duplicate", lines[1].text)
  }

  @Test
  fun unsortedSyncedLyricsAreSortedByTimestamp() {
    val lines =
      parseSyncedLyricLines(
        """
        [00:20.00]later
        [00:10.00]earlier
        """.trimIndent(),
      )

    assertEquals("earlier", lines[0].text)
    assertEquals("later", lines[1].text)
  }

  @Test
  fun plainLyricsIgnoreBlankLinesAndUseUntimedLines() {
    val lines =
      parsePlainLyricLines(
        """
        first synthetic line

          second synthetic line
        """.trimIndent(),
      )

    assertEquals(
      listOf(
        AmbientLyricLine(startMillis = null, text = "first synthetic line"),
        AmbientLyricLine(startMillis = null, text = "second synthetic line"),
      ),
      lines,
    )
  }

  @Test
  fun activeLineBeforeFirstTimestampSelectsFirstLine() {
    val lines = listOf(AmbientLyricLine(startMillis = 10_000L, text = "first"))

    assertEquals(0, activeLyricLineIndex(lines, 5_000L))
  }

  @Test
  fun activeLineExactlyOnTimestampSelectsThatLine() {
    val lines =
      listOf(
        AmbientLyricLine(startMillis = 10_000L, text = "first"),
        AmbientLyricLine(startMillis = 20_000L, text = "second"),
      )

    assertEquals(1, activeLyricLineIndex(lines, 20_000L))
  }

  @Test
  fun activeLineBetweenTimestampsSelectsPreviousLine() {
    val lines =
      listOf(
        AmbientLyricLine(startMillis = 10_000L, text = "first"),
        AmbientLyricLine(startMillis = 20_000L, text = "second"),
      )

    assertEquals(0, activeLyricLineIndex(lines, 19_999L))
  }

  @Test
  fun activeLineAfterLastTimestampSelectsFinalLine() {
    val lines =
      listOf(
        AmbientLyricLine(startMillis = 10_000L, text = "first"),
        AmbientLyricLine(startMillis = 20_000L, text = "second"),
      )

    assertEquals(1, activeLyricLineIndex(lines, 90_000L))
  }

  @Test
  fun activeLineHandlesSeekingBackwardAndForward() {
    val lines =
      listOf(
        AmbientLyricLine(startMillis = 10_000L, text = "first"),
        AmbientLyricLine(startMillis = 20_000L, text = "second"),
        AmbientLyricLine(startMillis = 30_000L, text = "third"),
      )

    assertEquals(2, activeLyricLineIndex(lines, 31_000L))
    assertEquals(0, activeLyricLineIndex(lines, 12_000L))
  }

  @Test
  fun activeLineHandlesEmptyAndOneLineLyrics() {
    assertEquals(0, activeLyricLineIndex(emptyList(), 10_000L))
    assertEquals(
      0,
      activeLyricLineIndex(
        listOf(AmbientLyricLine(startMillis = 10_000L, text = "only")),
        99_000L,
      ),
    )
  }

  @Test
  fun malformedSyncedInputProducesNoLines() {
    assertTrue(parseSyncedLyricLines("[nope]not lrc").isEmpty())
  }
}
