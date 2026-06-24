package com.finanzen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Tema raíz. Aplica color dinámico Material You en Android 12+ ([platformColorScheme]) con fallback
 * teal refinado en iOS/Desktop/Android<12. Los [FinanceColors] semánticos y la [Spacing] se proveen
 * vía CompositionLocal encima del scheme — independientes del color dinámico.
 */
@Composable
fun FinanZenTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    val colorScheme = platformColorScheme(darkTheme)
        ?: if (darkTheme) FinanZenDarkScheme else FinanZenLightScheme
    val financeColors = if (darkTheme) DarkFinanceColors else LightFinanceColors

    CompositionLocalProvider(
        LocalFinanceColors provides financeColors,
        LocalSpacing provides Spacing(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = finanZenTypography(),
            shapes = FinanZenShapes,
            content = content,
        )
    }
}
