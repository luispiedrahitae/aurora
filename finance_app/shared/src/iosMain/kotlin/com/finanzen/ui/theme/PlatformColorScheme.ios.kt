package com.finanzen.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

// iOS no tiene Material You → siempre fallback al preset de acento.
@Composable
actual fun platformColorScheme(darkTheme: Boolean): ColorScheme? = null

actual fun dynamicColorSupported(): Boolean = false
