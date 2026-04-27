package com.example.ai_guardian.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PrimaryBlue = Color(0xFF1976D2)
private val LightBg = Color(0xFFF5F7FA)
private val DarkBg = Color(0xFF0F172A)

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    background = LightBg,
    surface = Color.White
)

private val DarkColors = darkColorScheme(
    primary = PrimaryBlue,
    background = DarkBg,
    surface = Color(0xFF1E293B)
)

@Composable
fun AIGuardianTheme(
    darkMode: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkMode) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}