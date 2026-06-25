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
    Teal("Verde azulado", Color(0xFF1E6F5C), FinanZenLightScheme, FinanZenDarkScheme),

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

    Amber(
        "Ámbar",
        Color(0xFF8A5100),
        FinanZenLightScheme.withAccent(Color(0xFF8A5100), Color.White, Color(0xFFFFDDB6), Color(0xFF2C1600)),
        FinanZenDarkScheme.withAccent(Color(0xFFFFB868), Color(0xFF4A2800), Color(0xFF693C00), Color(0xFFFFDDB6)),
    ),

    Rose(
        "Rosa",
        Color(0xFFB42058),
        FinanZenLightScheme.withAccent(Color(0xFFB42058), Color.White, Color(0xFFFFD9E2), Color(0xFF3E001C)),
        FinanZenDarkScheme.withAccent(Color(0xFFFFB1C7), Color(0xFF65072F), Color(0xFF8E2C48), Color(0xFFFFD9E2)),
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
