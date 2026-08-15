package com.example.foldambient.lyrics

fun parseSyncedLyricLines(source: String): List<AmbientLyricLine> =
  source
    .lineSequence()
    .flatMap { line ->
      val timestamps = LrcTimestamp.findAll(line).toList()
      val text =
        timestamps.lastOrNull()
          ?.let { match -> line.substring(match.range.last + 1).trim() }
          .orEmpty()
      timestamps.mapNotNull { timestamp ->
        timestamp.toMillis()?.let { millis ->
          AmbientLyricLine(startMillis = millis, text = text)
        }
      }
    }
    .filter { it.text.isNotBlank() }
    .sortedBy { it.startMillis }
    .toList()

fun parsePlainLyricLines(source: String): List<AmbientLyricLine> =
  source
    .lineSequence()
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .map { AmbientLyricLine(startMillis = null, text = it) }
    .toList()

fun activeLyricLineIndex(
  lines: List<AmbientLyricLine>,
  positionMillis: Long?,
): Int =
  lines
    .indexOfLast { line ->
      val startMillis = line.startMillis
      startMillis != null && positionMillis != null && startMillis <= positionMillis
    }
    .coerceAtLeast(0)

private fun MatchResult.toMillis(): Long? {
  val minutes = groupValues.getOrNull(1)?.toLongOrNull() ?: return null
  val seconds = groupValues.getOrNull(2)?.toLongOrNull() ?: return null
  val fraction =
    groupValues.getOrNull(3)
      ?.takeIf { it.isNotBlank() }
      ?.padEnd(3, '0')
      ?.take(3)
      ?.toLongOrNull()
      ?: 0L
  return minutes * 60_000L + seconds * 1_000L + fraction
}

private val LrcTimestamp = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
