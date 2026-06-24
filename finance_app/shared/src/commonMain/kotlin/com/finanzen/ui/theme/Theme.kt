package com.finanzen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E6F5C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA8F2DE),
    onPrimaryContainer = Color(0xFF002019),
    secondary = Color(0xFF4A635D),
    background = Color(0xFFFBFDFA),
    surface = Color(0xFFFBFDFA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8CDABF),
    onPrimary = Color(0xFF003830),
    primaryContainer = Color(0xFF005245),
    onPrimaryContainer = Color(0xFFA8F2DE),
    secondary = Color(0xFFB1CCC4),
    background = Color(0xFF0F1411),
    surface = Color(0xFF0F1411),
)

@Composable
fun FinanZenTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
