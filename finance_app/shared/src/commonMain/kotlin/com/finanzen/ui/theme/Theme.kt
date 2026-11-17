package com.finanzen.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/** Tema elegido en Apariencia. [System] sigue el modo claro u oscuro del teléfono. */
enum class ThemeMode(val label: String) {
    System("Automático"),
    Light("Claro"),
    Dark("Oscuro"),
    ;

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.name == key } ?: System
    }
}

/** `true` cuando se está pintando el tema oscuro (para los pocos dibujos que lo necesitan). */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

/**
 * Tema raíz. El acento elegido reescribe solo la tríada de acento; fondos, superficies, colores
 * financieros y tipografía son fijos (ver Color.kt y Type.kt).
 */
@Composable
fun FinanZenTheme(
    themeMode: ThemeMode = ThemeMode.System,
    accent: AccentPreset = AccentPreset.Terracotta,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val colorScheme = if (darkTheme) accent.dark else accent.light
    val sageType = sageTextStyles()

    CompositionLocalProvider(
        LocalFinanceColors provides if (darkTheme) DarkFinanceColors else LightFinanceColors,
        LocalCategoryColors provides if (darkTheme) DarkCategoryColors else LightCategoryColors,
        LocalSpacing provides Spacing(),
        LocalAccentColor provides accent.light.primary,
        LocalReduceMotion provides reduceMotion,
        LocalSageType provides sageType,
        LocalIsDarkTheme provides darkTheme,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = sageType.toMaterialTypography(),
            shapes = FinanZenShapes,
            content = content,
        )
    }
}
