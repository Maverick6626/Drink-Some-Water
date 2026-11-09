package com.example.drinksomewater.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E88E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E9FB),
    onPrimaryContainer = Color(0xFF0B3A66),
    background = Color(0xFFF3F7FB),
    onBackground = Color(0xFF0F1B2A),
    surface = Color.White,
    onSurface = Color(0xFF0F1B2A),
    surfaceVariant = Color(0xFFE4EDF6),
    onSurfaceVariant = Color(0xFF52627A),
    outline = Color(0xFFB9CADD)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF05233F),
    primaryContainer = Color(0xFF1B3A5C),
    onPrimaryContainer = Color(0xFFD3E8FF),
    background = Color(0xFF0B131C),
    onBackground = Color(0xFFE6EEF7),
    surface = Color(0xFF131E2A),
    onSurface = Color(0xFFE6EEF7),
    surfaceVariant = Color(0xFF1D2B3B),
    onSurfaceVariant = Color(0xFF9FB1C6),
    outline = Color(0xFF3A5068)
)

@Composable
fun DrinkSomeWaterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
