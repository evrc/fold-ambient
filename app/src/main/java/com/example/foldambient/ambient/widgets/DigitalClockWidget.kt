package com.example.foldambient.ambient.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foldambient.ambient.AmbientWidget
import com.example.foldambient.ambient.WidgetConfigurationField
import com.example.foldambient.ambient.WidgetConfigurationOption
import com.example.foldambient.ambient.WidgetConfigurationFieldType
import com.example.foldambient.ambient.WidgetConfigurationSpec
import com.example.foldambient.ambient.WidgetInstance
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class DigitalClockWidget : AmbientWidget {
  override val type = "clock.digital"
  override val displayName = "Digital Clock"
  override val configurationSpec =
    WidgetConfigurationSpec(
      fields =
        listOf(
          WidgetConfigurationField(
            key = "label",
            label = "Label",
            type = WidgetConfigurationFieldType.Text,
            defaultValue = "Clock",
          ),
          WidgetConfigurationField(
            key = "style",
            label = "Style",
            type = WidgetConfigurationFieldType.Option,
            defaultValue = DigitalClockStyle.Classic.value,
            options = DigitalClockStyle.entries.map { WidgetConfigurationOption(it.value, it.label) },
          ),
          WidgetConfigurationField(
            key = "fillSpace",
            label = "Fill widget",
            type = WidgetConfigurationFieldType.Boolean,
            defaultValue = "false",
          ),
          WidgetConfigurationField(
            key = "use24Hour",
            label = "24-hour mode",
            type = WidgetConfigurationFieldType.Boolean,
            defaultValue = "true",
          ),
          WidgetConfigurationField(
            key = "showSeconds",
            label = "Seconds",
            type = WidgetConfigurationFieldType.Boolean,
            defaultValue = "false",
          ),
        ),
    )

  @Composable
  override fun Content(instance: WidgetInstance, modifier: Modifier) {
    val showSeconds = instance.configuration.text("showSeconds", "false").toBoolean()
    val use24Hour = instance.configuration.text("use24Hour", "true").toBoolean()
    val style = DigitalClockStyle.fromValue(instance.configuration.text("style", "classic"))
    val fillSpace = instance.configuration.text("fillSpace", "false").toBoolean()
    var now by remember { mutableStateOf(LocalTime.now()) }
    var today by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(showSeconds) {
      while (true) {
        now = LocalTime.now()
        today = LocalDate.now()
        delay(if (showSeconds) 1_000L else 30_000L)
      }
    }

    Column(
      modifier = modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
    ) {
      WidgetLabel(instance.configuration.text("label", displayName))
      DigitalClockFace(
        now = now,
        today = today,
        style = style,
        use24Hour = use24Hour,
        showSeconds = showSeconds,
        fillSpace = fillSpace,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
      )
    }
  }
}

@Composable
private fun DigitalClockFace(
  now: LocalTime,
  today: LocalDate,
  style: DigitalClockStyle,
  use24Hour: Boolean,
  showSeconds: Boolean,
  fillSpace: Boolean,
  modifier: Modifier = Modifier,
) {
  when (style) {
    DigitalClockStyle.Classic ->
      ClassicClock(
        now = now,
        use24Hour = use24Hour,
        showSeconds = showSeconds,
        fillSpace = fillSpace,
        modifier = modifier,
      )
    DigitalClockStyle.StandBy ->
      StandByClock(
        now = now,
        today = today,
        use24Hour = use24Hour,
        showSeconds = showSeconds,
        fillSpace = fillSpace,
        modifier = modifier,
      )
    DigitalClockStyle.Stacked ->
      StackedClock(
        now = now,
        use24Hour = use24Hour,
        showSeconds = showSeconds,
        fillSpace = fillSpace,
        modifier = modifier,
      )
    DigitalClockStyle.Split ->
      SplitClock(
        now = now,
        use24Hour = use24Hour,
        showSeconds = showSeconds,
        fillSpace = fillSpace,
        modifier = modifier,
      )
  }
}

@Composable
private fun ClassicClock(
  now: LocalTime,
  use24Hour: Boolean,
  showSeconds: Boolean,
  fillSpace: Boolean,
  modifier: Modifier = Modifier,
) {
  if (!fillSpace) {
    Column(
      modifier = modifier,
      verticalArrangement = Arrangement.Center,
    ) {
      WidgetValue(now.format(clockFormatter(use24Hour = use24Hour, showSeconds = showSeconds)))
    }
    return
  }

  val timeText = now.format(clockFormatter(use24Hour = use24Hour, showSeconds = showSeconds))
  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val textStyle = fittedTextStyle(
      text = timeText,
      maxWidth = maxWidth,
      maxHeight = maxHeight,
      fontWeight = FontWeight.Black,
    )
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text(
        text = timeText,
        color = Color.White,
        style = textStyle,
        maxLines = 1,
        overflow = TextOverflow.Clip,
      )
    }
  }
}

@Composable
private fun StandByClock(
  now: LocalTime,
  today: LocalDate,
  use24Hour: Boolean,
  showSeconds: Boolean,
  fillSpace: Boolean,
  modifier: Modifier = Modifier,
) {
  val parts = now.clockParts(use24Hour = use24Hour, showSeconds = showSeconds)
  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isWide = maxWidth > maxHeight
    val fittedSize =
      if (fillSpace) {
        fittedStandByFontSize(
          timeText = "${parts.hour}:${parts.minute}",
          sideText = today.format(DateFormatter).uppercase(),
          secondaryText = parts.secondaryText,
          maxWidth = maxWidth,
          maxHeight = maxHeight,
        )
      } else {
        if (isWide) 84f else 70f
      }
    val timeSize = fittedSize.sp
    val sideSize = (fittedSize * 0.16f).coerceAtLeast(11f).sp
    val sideLineHeight = (fittedSize * 0.19f).coerceAtLeast(14f).sp
    val sideSpacing = (fittedSize * 0.12f).coerceAtLeast(8f).dp

    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 2.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "${parts.hour}:${parts.minute}",
        color = StandByGreen,
        fontSize = timeSize,
        lineHeight = timeSize,
        fontWeight = FontWeight.Black,
        maxLines = 1,
        overflow = TextOverflow.Clip,
      )
      Column(
        modifier = Modifier.padding(start = sideSpacing),
        verticalArrangement = Arrangement.Center,
      ) {
        Text(
          text = today.format(DateFormatter).uppercase(),
          color = Color.White,
          fontSize = sideSize,
          lineHeight = sideLineHeight,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
        )
        Text(
          text = parts.secondaryText,
          color = Color.White,
          fontSize = sideSize,
          lineHeight = sideLineHeight,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
private fun StackedClock(
  now: LocalTime,
  use24Hour: Boolean,
  showSeconds: Boolean,
  fillSpace: Boolean,
  modifier: Modifier = Modifier,
) {
  val parts = now.clockParts(use24Hour = use24Hour, showSeconds = showSeconds)
  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val numberSize =
      if (fillSpace) {
        fittedStackedFontSize(
          parts = parts,
          maxWidth = maxWidth,
          maxHeight = maxHeight,
        ).sp
      } else {
        if (maxWidth > maxHeight) 58.sp else 64.sp
      }
    val overlapPadding = if (maxWidth > maxHeight) 0.dp else 4.dp
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = parts.hour,
        color = Cyan,
        fontSize = numberSize,
        lineHeight = numberSize,
        fontWeight = FontWeight.Black,
        maxLines = 1,
      )
      Text(
        text = parts.minute,
        color = Blue,
        fontSize = numberSize,
        lineHeight = numberSize,
        fontWeight = FontWeight.Black,
        maxLines = 1,
        modifier = Modifier.padding(top = overlapPadding),
      )
      if (parts.secondaryText.isNotBlank()) {
        Text(
          text = parts.secondaryText,
          color = Color.White,
          style = MaterialTheme.typography.labelMedium,
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
private fun SplitClock(
  now: LocalTime,
  use24Hour: Boolean,
  showSeconds: Boolean,
  fillSpace: Boolean,
  modifier: Modifier = Modifier,
) {
  val parts = now.clockParts(use24Hour = use24Hour, showSeconds = showSeconds)
  val fixedSize = 66f
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val numberSize =
        if (fillSpace) {
          fittedSplitFontSize(
            parts = parts,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
          )
        } else {
          fixedSize
        }
      Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = parts.hour,
          color = Pink,
          fontSize = numberSize.sp,
          lineHeight = numberSize.sp,
          fontWeight = FontWeight.Black,
          textAlign = TextAlign.End,
          maxLines = 1,
        )
        Text(
          text = parts.minute,
          color = Yellow,
          fontSize = numberSize.sp,
          lineHeight = numberSize.sp,
          fontWeight = FontWeight.Black,
          textAlign = TextAlign.Start,
          maxLines = 1,
        )
      }
    }
    if (parts.secondaryText.isNotBlank()) {
      Text(
        text = parts.secondaryText,
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.align(Alignment.BottomEnd),
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun fittedTextStyle(
  text: String,
  maxWidth: Dp,
  maxHeight: Dp,
  fontWeight: FontWeight,
): TextStyle {
  val measurer = rememberTextMeasurer()
  val fontSize =
    fittedFontSize(maxWidth = maxWidth, maxHeight = maxHeight) { candidate ->
      val layout =
        measurer.measure(
          text = AnnotatedString(text),
          style =
            TextStyle(
              fontSize = candidate.sp,
              lineHeight = candidate.sp,
              fontWeight = fontWeight,
            ),
          maxLines = 1,
        )
      ClockBounds(
        width = layout.size.width.toFloat(),
        height = layout.size.height.toFloat(),
      )
    }
  return TextStyle(
    fontSize = fontSize.sp,
    lineHeight = fontSize.sp,
    fontWeight = fontWeight,
  )
}

@Composable
private fun fittedStandByFontSize(
  timeText: String,
  sideText: String,
  secondaryText: String,
  maxWidth: Dp,
  maxHeight: Dp,
): Float {
  val measurer = rememberTextMeasurer()
  return fittedFontSize(maxWidth = maxWidth, maxHeight = maxHeight) { candidate ->
    val main = measurer.measureClockText(timeText, candidate, FontWeight.Black)
    val sideSize = candidate * 0.16f
    val sideOne = measurer.measureClockText(sideText, sideSize, FontWeight.SemiBold)
    val sideTwo =
      if (secondaryText.isBlank()) {
        ClockBounds(width = 0f, height = 0f)
      } else {
        measurer.measureClockText(secondaryText, sideSize, FontWeight.Medium)
      }
    ClockBounds(
      width = main.width + (candidate * 0.12f) + maxOf(sideOne.width, sideTwo.width),
      height = maxOf(main.height, sideOne.height + sideTwo.height),
    )
  }
}

@Composable
private fun fittedStackedFontSize(
  parts: ClockParts,
  maxWidth: Dp,
  maxHeight: Dp,
): Float {
  val measurer = rememberTextMeasurer()
  return fittedFontSize(maxWidth = maxWidth, maxHeight = maxHeight) { candidate ->
    val hour = measurer.measureClockText(parts.hour, candidate, FontWeight.Black)
    val minute = measurer.measureClockText(parts.minute, candidate, FontWeight.Black)
    val secondary =
      if (parts.secondaryText.isBlank()) {
        ClockBounds(width = 0f, height = 0f)
      } else {
        measurer.measureClockText(parts.secondaryText, candidate * 0.18f, FontWeight.Medium)
      }
    ClockBounds(
      width = maxOf(hour.width, minute.width, secondary.width),
      height = hour.height + minute.height + secondary.height,
    )
  }
}

@Composable
private fun fittedSplitFontSize(
  parts: ClockParts,
  maxWidth: Dp,
  maxHeight: Dp,
): Float {
  val measurer = rememberTextMeasurer()
  return fittedFontSize(maxWidth = maxWidth, maxHeight = maxHeight) { candidate ->
    val hour = measurer.measureClockText(parts.hour, candidate, FontWeight.Black)
    val minute = measurer.measureClockText(parts.minute, candidate, FontWeight.Black)
    ClockBounds(
      width = hour.width + minute.width,
      height = maxOf(hour.height, minute.height),
    )
  }
}

@Composable
private fun fittedFontSize(
  maxWidth: Dp,
  maxHeight: Dp,
  measure: (Float) -> ClockBounds,
): Float {
  val density = LocalDensity.current
  val maxWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
  val maxHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
  var low = 6f
  var high = 240f

  repeat(14) {
    val candidate = (low + high) / 2f
    val bounds = measure(candidate)
    if (bounds.width <= maxWidthPx && bounds.height <= maxHeightPx) {
      low = candidate
    } else {
      high = candidate
    }
  }

  return (low * 0.96f).coerceAtLeast(6f)
}

private fun androidx.compose.ui.text.TextMeasurer.measureClockText(
  text: String,
  fontSize: Float,
  fontWeight: FontWeight,
): ClockBounds {
  if (text.isBlank()) return ClockBounds(width = 0f, height = 0f)
  val layout =
    measure(
      text = AnnotatedString(text),
      style =
        TextStyle(
          fontSize = fontSize.sp,
          lineHeight = fontSize.sp,
          fontWeight = fontWeight,
        ),
      maxLines = 1,
    )
  return ClockBounds(
    width = layout.size.width.toFloat(),
    height = layout.size.height.toFloat(),
  )
}

private fun clockFormatter(use24Hour: Boolean, showSeconds: Boolean): DateTimeFormatter =
  DateTimeFormatter.ofPattern(
    when {
      use24Hour && showSeconds -> "HH:mm:ss"
      use24Hour -> "HH:mm"
      showSeconds -> "h:mm:ss a"
      else -> "h:mm a"
    },
  )

private fun LocalTime.clockParts(use24Hour: Boolean, showSeconds: Boolean): ClockParts {
  val hourValue = if (use24Hour) hour else ((hour + 11) % 12) + 1
  return ClockParts(
    hour = if (use24Hour) hourValue.toString().padStart(2, '0') else hourValue.toString(),
    minute = minute.toString().padStart(2, '0'),
    secondaryText =
      when {
        showSeconds -> second.toString().padStart(2, '0')
        use24Hour -> ""
        hour < 12 -> "AM"
        else -> "PM"
      },
  )
}

private data class ClockParts(
  val hour: String,
  val minute: String,
  val secondaryText: String,
)

private data class ClockBounds(
  val width: Float,
  val height: Float,
)

private enum class DigitalClockStyle(
  val value: String,
  val label: String,
) {
  Classic(value = "classic", label = "Classic"),
  StandBy(value = "standby", label = "StandBy"),
  Stacked(value = "stacked", label = "Stacked"),
  Split(value = "split", label = "Split");

  companion object {
    fun fromValue(value: String): DigitalClockStyle =
      entries.firstOrNull { it.value == value } ?: Classic
  }
}

private val DateFormatter = DateTimeFormatter.ofPattern("EEE d")
private val StandByGreen = Color(0xFF8DDC8B)
private val Cyan = Color(0xFF22D3EE)
private val Blue = Color(0xFF60A5FA)
private val Pink = Color(0xFFFDB8C8)
private val Yellow = Color(0xFFFCD34D)
