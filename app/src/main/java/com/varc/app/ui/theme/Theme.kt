package com.varc.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6C63FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3D34CC),
    secondary = Color(0xFF00D9FF),
    tertiary = Color(0xFFFF6584),
    background = Color(0xFF1A1A2E),
    surface = Color(0xFF16213E),
    surfaceVariant = Color(0xFF0F3460),
    onBackground = Color.White,
    onSurface = Color.White,
    error = Color(0xFFCF6679)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6C63FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E0FF),
    secondary = Color(0xFF008BA3),
    tertiary = Color(0xFFC00040),
    background = Color(0xFFF8F9FF),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8E0F0),
    onBackground = Color(0xFF1A1A2E),
    onSurface = Color(0xFF1A1A2E),
    error = Color(0xFFBA1A1A)
)

@Composable
fun VARCTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
