package com.finanzen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Tema raíz. Si [useDynamic] y la plataforma lo soporta (Android 12+), aplica color dinámico
 * Material You; si no, usa el [accent] elegido por el usuario. Los [FinanceColors] semánticos y la
 * [Spacing] se proveen vía CompositionLocal encima del scheme — independientes del acento.
 */
@Composable
fun FinanZenTheme(
    darkTheme: Boolean = false,
    accent: AccentPreset = AccentPreset.Teal,
    useDynamic: Boolean = true,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dynamicScheme = if (useDynamic) platformColorScheme(darkTheme) else null
    val colorScheme = dynamicScheme ?: if (darkTheme) accent.dark else accent.light
    val financeColors = if (darkTheme) DarkFinanceColors else LightFinanceColors
    val categoryColors = if (darkTheme) DarkCategoryColors else LightCategoryColors
    val accentColor = (if (useDynamic) platformColorScheme(false) else null)?.primary ?: accent.light.primary

    CompositionLocalProvider(
        LocalFinanceColors provides financeColors,
        LocalCategoryColors provides categoryColors,
        LocalSpacing provides Spacing(),
        LocalAccentColor provides accentColor,
        LocalReduceMotion provides reduceMotion,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = finanZenTypography(),
            shapes = FinanZenShapes,
            content = content,
        )
    }
}
