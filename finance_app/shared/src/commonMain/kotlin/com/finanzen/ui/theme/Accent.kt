package com.finanzen.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Presets de acento elegibles por el usuario. La cromática neutra (fondo, superficies, error) se
 * mantiene constante entre presets — solo cambia la tríada de acento (primary/secondary/tertiary y
 * sus contenedores) — para que la app se sienta del mismo producto, no de dos apps distintas.
 *
 * [swatch] es el color que se pinta en el selector (el primary en claro, reconocible de un vistazo).
 * Mono-acento a propósito: secondary y tertiary heredan el primary del preset para un look limpio.
 *
 * Paleta unificada: los 12 acentos (salvo Terracota) reutilizan exactamente los 12 matices de
 * [LightCategoryColors]/[DarkCategoryColors] usados en el chart de gastos por categoría, para que el
 * selector de Apariencia y el chart compartan una sola identidad cromática. Terracota se mantiene con
 * su valor histórico más brillante (identidad con el scheme base, no-op) en vez del terracota de
 * categoría (más oscuro) para no cambiar el look por defecto de la app en un dispositivo de uso
 * personal.
 *
 * Derivación de container/onContainer (H,S constantes del primary de cada tema, solo se mueve L):
 * container claro = L90%, onContainer claro = L28%, onPrimary oscuro = L13%, container oscuro = L20%,
 * onContainer oscuro = L72%. Los primarios claros de ochre/olive/moss/fern/light terracotta se
 * oscurecieron ligeramente (mismo H,S, ΔL 1-4 puntos) respecto del valor de categoría porque ese
 * valor no alcanzaba 4.5:1 contra blanco (ver [LocalAccentColor]).
 */
enum class AccentPreset(
    val label: String,
    val swatch: Color,
    val light: ColorScheme,
    val dark: ColorScheme,
) {
    // Terracotta es el acento por defecto histórico — identidad con el scheme base (no-op), se
    // mantiene aparte del terracota de categoría (más oscuro) para no romper el look por defecto.
    Terracotta("Terracota", Color(0xFFAD6332), FinanZenLightScheme, FinanZenDarkScheme),

    Ochre(
        "Ocre",
        Color(0xFF7F7740),
        FinanZenLightScheme.withAccent(Color(0xFF7F7740), Color.White, Color(0xFFEEECDD), Color(0xFF5F5930)),
        FinanZenDarkScheme.withAccent(Color(0xFFCAC394), Color(0xFF2C2916), Color(0xFF444022), Color(0xFFD0C9A0)),
    ),

    Olive(
        "Oliva",
        Color(0xFF677D40),
        FinanZenLightScheme.withAccent(Color(0xFF677D40), Color.White, Color(0xFFE8EEDD), Color(0xFF4E5F30)),
        FinanZenDarkScheme.withAccent(Color(0xFFB8C794), Color(0xFF252C17), Color(0xFF3A4323), Color(0xFFC1CEA1)),
    ),

    Moss(
        "Musgo",
        Color(0xFF4C8242),
        FinanZenLightScheme.withAccent(Color(0xFF4C8242), Color.White, Color(0xFFE0EEDD), Color(0xFF375F30)),
        FinanZenDarkScheme.withAccent(Color(0xFFA1C794), Color(0xFF1C2C17), Color(0xFF2B4323), Color(0xFFADCEA1)),
    ),

    Fern(
        "Helecho",
        Color(0xFF45825B),
        FinanZenLightScheme.withAccent(Color(0xFF45825B), Color.White, Color(0xFFDEEDE3), Color(0xFF315D41)),
        FinanZenDarkScheme.withAccent(Color(0xFF9CC7A7), Color(0xFF182A1D), Color(0xFF25412C), Color(0xFFA4CBAE)),
    ),

    Amber(
        "Ámbar",
        Color(0xFF8C6B32),
        FinanZenLightScheme.withAccent(Color(0xFF8C6B32), Color.White, Color(0xFFF2E9D9), Color(0xFF695026)),
        FinanZenDarkScheme.withAccent(Color(0xFFD2B682), Color(0xFF312612), Color(0xFF4B3A1B), Color(0xFFD9C296)),
    ),

    Sienna(
        "Siena",
        Color(0xFF954935),
        FinanZenLightScheme.withAccent(Color(0xFF954935), Color.White, Color(0xFFF2DED9), Color(0xFF693425)),
        FinanZenDarkScheme.withAccent(Color(0xFFDB9485), Color(0xFF33150F), Color(0xFF4F2117), Color(0xFFDE9E91)),
    ),

    DarkOlive(
        "Oliva oscuro",
        Color(0xFF5E6B34),
        FinanZenLightScheme.withAccent(Color(0xFF5E6B34), Color.White, Color(0xFFEAEEDD), Color(0xFF54602F)),
        FinanZenDarkScheme.withAccent(Color(0xFFA4B684), Color(0xFF242A19), Color(0xFF374026), Color(0xFFBDCAA5)),
    ),

    DustyRose(
        "Rosa polvo",
        Color(0xFF8A5A5A),
        FinanZenLightScheme.withAccent(Color(0xFF8A5A5A), Color.White, Color(0xFFEBE0E0), Color(0xFF563838)),
        FinanZenDarkScheme.withAccent(Color(0xFFD0A5AA), Color(0xFF2C1719), Color(0xFF432327), Color(0xFFCEA1A6)),
    ),

    LightTerracotta(
        "Terracota clara",
        Color(0xFF966D4F),
        FinanZenLightScheme.withAccent(Color(0xFF966D4F), Color.White, Color(0xFFEDE4DE), Color(0xFF5D4431)),
        FinanZenDarkScheme.withAccent(Color(0xFFE9C1A6), Color(0xFF351D0D), Color(0xFF522D14), Color(0xFFE3AF8D)),
    ),

    Rose(
        "Rosa",
        Color(0xFF87456B),
        FinanZenLightScheme.withAccent(Color(0xFF87456B), Color.White, Color(0xFFEEDDE7), Color(0xFF5F304B)),
        FinanZenDarkScheme.withAccent(Color(0xFFCA99B0), Color(0xFF2C1721), Color(0xFF432332), Color(0xFFCEA1B6)),
    ),

    Clay(
        "Arcilla",
        Color(0xFF87454B),
        FinanZenLightScheme.withAccent(Color(0xFF87454B), Color.White, Color(0xFFEEDDDF), Color(0xFF5F3034)),
        FinanZenDarkScheme.withAccent(Color(0xFFCA9999), Color(0xFF2C1717), Color(0xFF432323), Color(0xFFCEA1A1)),
    ),
    ;

    companion object {
        fun fromKey(key: String): AccentPreset = entries.firstOrNull { it.name == key } ?: Terracotta
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
