package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF6366F1), // Bright Indigo
    secondary = Color(0xFF94A3B8), // Sleek Slate
    tertiary = Color(0xFF10B981), // Emerald Accent
    background = Color(0xFF090D16), // Premium Deep Dark Slate
    surface = Color(0xFF151D2F), // Solid Slate Card
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFF8FAFC), // High-Contrast Text Light Gray
    onSurface = Color(0xFFCBD5E1) // Text Gray
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF4F46E5), // Indigo
    secondary = Color(0xFF475569), // Slate Secondary
    tertiary = Color(0xFF059669), // Emerald
    background = Color(0xFFF8FAFC), // Light Slate Background
    surface = Color(0xFFFFFFFF), // Solid White Card
    onPrimary = Color.White,
    onSecondary = Color(0xFF334155),
    onTertiary = Color.White,
    onBackground = Color(0xFF1E293B), // Softer-Contrast Text Dark Slate
    onSurface = Color(0xFF475569)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  brandPrimary: Color? = null,
  selectedFont: String = "estedad",
  content: @Composable () -> Unit,
) {
  val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val colorScheme = if (brandPrimary != null) {
    baseScheme.copy(primary = brandPrimary)
  } else {
    baseScheme
  }

  val dynamicTypography = getTypography(selectedFont)

  MaterialTheme(colorScheme = colorScheme, typography = dynamicTypography, content = content)
}
