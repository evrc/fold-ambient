package com.example.foldambient.ambient.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.foldambient.ambient.AmbientWidget
import com.example.foldambient.ambient.WidgetConfigurationField
import com.example.foldambient.ambient.WidgetConfigurationFieldType
import com.example.foldambient.ambient.WidgetConfigurationSpec
import com.example.foldambient.ambient.WidgetInstance
import kotlinx.coroutines.delay
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class AnalogClockWidget : AmbientWidget {
  override val type = "clock.analog"
  override val displayName = "Analog Clock"
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
        ),
    )

  @Composable
  override fun Content(instance: WidgetInstance, modifier: Modifier) {
    var now by remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(Unit) {
      while (true) {
        now = LocalTime.now()
        delay(1_000L)
      }
    }

    Column(
      modifier = modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
    ) {
      WidgetLabel(instance.configuration.text("label", displayName))
      Canvas(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(140.dp),
      ) {
        val radius = min(size.width, size.height) / 2f * 0.82f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
          color = Color(0xFF374151),
          radius = radius,
          center = center,
          style = Stroke(width = 3.dp.toPx()),
        )

        repeat(12) { index ->
          val angle = index * 30f
          val outer = center.pointAt(radius, angle)
          val inner = center.pointAt(radius * 0.88f, angle)
          drawLine(
            color = Color(0xFF9CA3AF),
            start = inner,
            end = outer,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
          )
        }

        val hourAngle = ((now.hour % 12) + now.minute / 60f) * 30f
        val minuteAngle = (now.minute + now.second / 60f) * 6f
        val secondAngle = now.second * 6f
        drawHand(center, radius * 0.46f, hourAngle, Color.White, 5.dp.toPx())
        drawHand(center, radius * 0.68f, minuteAngle, Color.White, 3.dp.toPx())
        drawHand(center, radius * 0.74f, secondAngle, Color(0xFF9CA3AF), 1.5.dp.toPx())
        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = center)
      }
    }
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHand(
  center: Offset,
  length: Float,
  angleDegrees: Float,
  color: Color,
  strokeWidth: Float,
) {
  drawLine(
    color = color,
    start = center,
    end = center.pointAt(length, angleDegrees),
    strokeWidth = strokeWidth,
    cap = StrokeCap.Round,
  )
}

private fun Offset.pointAt(length: Float, angleDegrees: Float): Offset {
  val radians = (angleDegrees - 90f) * PI.toFloat() / 180f
  return Offset(
    x = x + cos(radians) * length,
    y = y + sin(radians) * length,
  )
}
