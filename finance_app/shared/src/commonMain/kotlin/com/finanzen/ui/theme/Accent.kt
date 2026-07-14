package com.finanzen.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Presets de acento elegibles por el usuario. La cromática neutra (fondo, superficies, error) se
 * mantiene constante entre presets — solo cambia la tríada de acento (primary/secondary/tertiary y
 * sus contenedores) — para que la app se sienta del mismo producto, no de cinco apps distintas.
 *
 * [swatch] es el color que se pinta en el selector (el primary en claro, reconocible de un vistazo).
 * Mono-acento a propósito: secondary y tertiary heredan el primary del preset para un look limpio.
 */
enum class AccentPreset(
    val label: String,
    val swatch: Color,
    val light: ColorScheme,
    val dark: ColorScheme,
) {
    // Ordenados por matiz para que el selector lea como una rueda de color.
    Teal("Verde azulado", Color(0xFF1E6F5C), FinanZenLightScheme, FinanZenDarkScheme),

    Cyan(
        "Cian",
        Color(0xFF00696E),
        FinanZenLightScheme.withAccent(Color(0xFF00696E), Color.White, Color(0xFF6FF6FE), Color(0xFF002022)),
        FinanZenDarkScheme.withAccent(Color(0xFF4CD9E1), Color(0xFF00363A), Color(0xFF004F53), Color(0xFF6FF6FE)),
    ),

    Blue(
        "Azul",
        Color(0xFF1560C0),
        FinanZenLightScheme.withAccent(Color(0xFF1560C0), Color.White, Color(0xFFD6E3FF), Color(0xFF001B3D)),
        FinanZenDarkScheme.withAccent(Color(0xFFA8C8FF), Color(0xFF002F65), Color(0xFF00468F), Color(0xFFD6E3FF)),
    ),

    Indigo(
        "Índigo",
        Color(0xFF4655B6),
        FinanZenLightScheme.withAccent(Color(0xFF4655B6), Color.White, Color(0xFFDFE0FF), Color(0xFF00115C)),
        FinanZenDarkScheme.withAccent(Color(0xFFBBC3FF), Color(0xFF13277B), Color(0xFF2D3D93), Color(0xFFDFE0FF)),
    ),

    Violet(
        "Violeta",
        Color(0xFF6750A4),
        FinanZenLightScheme.withAccent(Color(0xFF6750A4), Color.White, Color(0xFFEADDFF), Color(0xFF21005D)),
        FinanZenDarkScheme.withAccent(Color(0xFFD0BCFF), Color(0xFF381E72), Color(0xFF4F378B), Color(0xFFEADDFF)),
    ),

    Orange(
        "Naranja",
        Color(0xFF9A4500),
        FinanZenLightScheme.withAccent(Color(0xFF9A4500), Color.White, Color(0xFFFFDBCC), Color(0xFF331200)),
        FinanZenDarkScheme.withAccent(Color(0xFFFFB694), Color(0xFF522300), Color(0xFF753400), Color(0xFFFFDBCC)),
    ),

    Amber(
        "Ámbar",
        Color(0xFF8A5100),
        FinanZenLightScheme.withAccent(Color(0xFF8A5100), Color.White, Color(0xFFFFDDB6), Color(0xFF2C1600)),
        FinanZenDarkScheme.withAccent(Color(0xFFFFB868), Color(0xFF4A2800), Color(0xFF693C00), Color(0xFFFFDDB6)),
    ),

    Amarillo(
        "Amarillo",
        Color(0xFF6B5D00),
        FinanZenLightScheme.withAccent(Color(0xFF6B5D00), Color.White, Color(0xFFFFE173), Color(0xFF221B00)),
        FinanZenDarkScheme.withAccent(Color(0xFFE7C400), Color(0xFF3A2F00), Color(0xFF544600), Color(0xFFFFE173)),
    ),

    Lima(
        "Lima",
        Color(0xFF56690A),
        FinanZenLightScheme.withAccent(Color(0xFF56690A), Color.White, Color(0xFFD7EE8F), Color(0xFF171E00)),
        FinanZenDarkScheme.withAccent(Color(0xFFBBD273), Color(0xFF2B3300), Color(0xFF404D00), Color(0xFFD7EE8F)),
    ),

    Green(
        "Verde",
        Color(0xFF386A20),
        FinanZenLightScheme.withAccent(Color(0xFF386A20), Color.White, Color(0xFFB7F397), Color(0xFF042100)),
        FinanZenDarkScheme.withAccent(Color(0xFF9CD67D), Color(0xFF0C3900), Color(0xFF205107), Color(0xFFB7F397)),
    ),
    ;

    companion object {
        fun fromKey(key: String): AccentPreset = entries.firstOrNull { it.name == key } ?: Teal
    }
}

/** Reescribe solo la tríada de acento sobre un scheme base, conservando neutros/error/superficies. */
private fun ColorScheme.withAccent(
    primary: Color,
    onPrimary: Color,
    container: Color,
    onContainer: Color,
): ColorScheme = copy(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = container,
    onPrimaryContainer = onContainer,
    secondary = primary,
    onSecondary = onPrimary,
    secondaryContainer = container,
    onSecondaryContainer = onContainer,
    tertiary = primary,
    onTertiary = onPrimary,
    tertiaryContainer = container,
    onTertiaryContainer = onContainer,
    inversePrimary = container,
)
