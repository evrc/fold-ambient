package com.example.foldambient.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = AmbientBlue,
    secondary = AmbientAmber,
    tertiary = AmbientOnSurface,
    background = AmbientBlack,
    surface = AmbientSurface,
    surfaceVariant = AmbientSurfaceHigh,
    onPrimary = AmbientBlack,
    onSecondary = AmbientBlack,
    onTertiary = AmbientBlack,
    onBackground = AmbientOnSurface,
    onSurface = AmbientOnSurface,
    onSurfaceVariant = AmbientMuted,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF0E7490),
    secondary = Color(0xFFB45309),
    tertiary = Color(0xFF111827),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFE5E7EB),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF111827),
    onSurfaceVariant = Color(0xFF4B5563),
  )

@Composable
fun FoldAmbientTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
