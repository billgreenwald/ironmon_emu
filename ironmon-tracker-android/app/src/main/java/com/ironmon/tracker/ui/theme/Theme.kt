package com.ironmon.tracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary         = Color(0xFFE94560),
    onPrimary       = Color(0xFFFFFFFF),
    primaryContainer= Color(0xFF7B1230),
    secondary       = Color(0xFF16213E),
    background      = Color(0xFF0F3460),
    surface         = Color(0xFF1A1A2E),
    onBackground    = Color(0xFFEEEEEE),
    onSurface       = Color(0xFFEEEEEE),
)

@Composable
fun IronmonTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content     = content,
    )
}
