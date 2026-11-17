package com.finanzen.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * Paleta Sage (design system "Sage", docs/DESIGN.md). Correspondencia token → slot de Material 3:
 *
 *   bg              → background
 *   surface         → surface y surfaceContainerLowest..High (las tarjetas: superficie + borde fino)
 *   surface-sunken  → surfaceVariant y surfaceContainerHighest (campos, pistas de progreso, segmentos)
 *   border          → outlineVariant (bordes de tarjeta, divisores)
 *   border-strong   → outline (borde de campos y botones secundarios)
 *   ink / ink-muted → onSurface / onSurfaceVariant
 *   primary-ink     → onPrimaryContainer (texto en color de acción; el primary claro no llega a 4.5:1 como texto)
 *   primary-soft    → primaryContainer
 *   danger(-soft)   → error / errorContainer
 */
internal val FinanZenLightScheme = lightColorScheme(
    primary = Color(0xFFAD6332),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF7EBE3),
    onPrimaryContainer = Color(0xFF7F4824),
    inversePrimary = Color(0xFFD79B74),
    secondary = Color(0xFFAD6332),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF7EBE3),
    onSecondaryContainer = Color(0xFF7F4824),
    tertiary = Color(0xFFAD6332),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF7EBE3),
    onTertiaryContainer = Color(0xFF7F4824),
    background = Color(0xFFF5EAD8),
    onBackground = Color(0xFF201E1D),
    surface = Color(0xFFFAF3E6),
    onSurface = Color(0xFF201E1D),
    surfaceVariant = Color(0xFFECDCC2),
    onSurfaceVariant = Color(0xFF5B5450),
    surfaceTint = Color.Transparent,
    surfaceDim = Color(0xFFECDCC2),
    surfaceBright = Color(0xFFFAF3E6),
    surfaceContainerLowest = Color(0xFFFAF3E6),
    surfaceContainerLow = Color(0xFFFAF3E6),
    surfaceContainer = Color(0xFFFAF3E6),
    surfaceContainerHigh = Color(0xFFFAF3E6),
    surfaceContainerHighest = Color(0xFFECDCC2),
    inverseSurface = Color(0xFF201E1D),
    inverseOnSurface = Color(0xFFFAF3E6),
    outline = Color(0xFF8A7859),
    outlineVariant = Color(0xFFD8C9AE),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    scrim = Color(0xFF201E1D),
)

internal val FinanZenDarkScheme = darkColorScheme(
    primary = Color(0xFFD79B74),
    onPrimary = Color(0xFF372010),
    primaryContainer = Color(0xFF573219),
    onPrimaryContainer = Color(0xFFE4BBA0),
    inversePrimary = Color(0xFFAD6332),
    secondary = Color(0xFFD79B74),
    onSecondary = Color(0xFF372010),
    secondaryContainer = Color(0xFF573219),
    onSecondaryContainer = Color(0xFFE4BBA0),
    tertiary = Color(0xFFD79B74),
    onTertiary = Color(0xFF372010),
    tertiaryContainer = Color(0xFF573219),
    onTertiaryContainer = Color(0xFFE4BBA0),
    background = Color(0xFF221F19),
    onBackground = Color(0xFFFBF7F0),
    surface = Color(0xFF2B2720),
    onSurface = Color(0xFFFBF7F0),
    surfaceVariant = Color(0xFF382F23),
    onSurfaceVariant = Color(0xFFC4B9A5),
    surfaceTint = Color.Transparent,
    surfaceDim = Color(0xFF221F19),
    surfaceBright = Color(0xFF382F23),
    surfaceContainerLowest = Color(0xFF2B2720),
    surfaceContainerLow = Color(0xFF2B2720),
    surfaceContainer = Color(0xFF2B2720),
    surfaceContainerHigh = Color(0xFF2B2720),
    surfaceContainerHighest = Color(0xFF382F23),
    inverseSurface = Color(0xFFFBF7F0),
    inverseOnSurface = Color(0xFF2B2720),
    outline = Color(0xFF8C7E66),
    outlineVariant = Color(0xFF5A4F3C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF5C1512),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color.Black,
)

/**
 * Colores semánticos de finanzas y de datos. Viven fuera del ColorScheme a propósito: no cambian con
 * el color de acento. Ingreso (salvia) y gasto (terracota oscuro) se distinguen además por signo y
 * flecha, nunca solo por color (daltonismo rojo-verde).
 */
@Immutable
data class FinanceColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val neutral: Color,
    val warning: Color,
    val warningContainer: Color,
    /** Anillo de foco de teclado: azul, distinto de cualquier acento. */
    val focus: Color,
    /** Series de gráficos, en orden: pizarra, terracota, salvia, ámbar, rosa polvo, ocre, gris (referencia). */
    val chart: List<Color>,
)

internal val LightFinanceColors = FinanceColors(
    income = Color(0xFF4B553A),
    incomeContainer = Color(0xFFE7EAE1),
    expense = Color(0xFF7F4824),
    expenseContainer = Color(0xFFF3DCCB),
    neutral = Color(0xFF5B5450),
    warning = Color(0xFF8F4A07),
    warningContainer = Color(0xFFF6E3BF),
    focus = Color(0xFF1A56C4),
    chart = listOf(
        Color(0xFF4A6A85),
        Color(0xFFAD6332),
        Color(0xFF5F7343),
        Color(0xFF8C6B32),
        Color(0xFF8A5A5A),
        Color(0xFF7F7740),
        Color(0xFF7A7064),
    ),
)

internal val DarkFinanceColors = FinanceColors(
    income = Color(0xFFBBC5AA),
    incomeContainer = Color(0xFF363D29),
    expense = Color(0xFFE4BBA0),
    expenseContainer = Color(0xFF573219),
    neutral = Color(0xFFC4B9A5),
    warning = Color(0xFFFBBF24),
    warningContainer = Color(0xFF4A3A10),
    focus = Color(0xFF9CC0FF),
    chart = listOf(
        Color(0xFF9DB8D0),
        Color(0xFFD79B74),
        Color(0xFFB8C794),
        Color(0xFFD2B682),
        Color(0xFFD0A5AA),
        Color(0xFFCAC394),
        Color(0xFFA89D8C),
    ),
)

val LocalFinanceColors = staticCompositionLocalOf { LightFinanceColors }

/**
 * Paleta de categorías: 12 matices cálidos de la marca. El orden importa: [colorForCategory] usa
 * `hash % size`, así que cambiarlo recolorearía las categorías existentes. Nunca es la única
 * codificación: cada porción/fila lleva nombre y porcentaje.
 */
val LightCategoryColors = listOf(
    Color(0xFF875F44), // terracota
    Color(0xFF877F44), // ocre
    Color(0xFF6E8644), // oliva
    Color(0xFF4E8644), // musgo
    Color(0xFF47865E), // helecho
    Color(0xFF8C6B32), // ámbar
    Color(0xFF954935), // siena
    Color(0xFF5E6B34), // oliva oscuro
    Color(0xFF8A5A5A), // rosa polvo
    Color(0xFFA37656), // terracota clara
    Color(0xFF87456B), // rosa
    Color(0xFF87454B), // arcilla
)

val DarkCategoryColors = listOf(
    Color(0xFFCAAC94),
    Color(0xFFCAC394),
    Color(0xFFB8C794),
    Color(0xFFA1C794),
    Color(0xFF9CC7A7),
    Color(0xFFD2B682),
    Color(0xFFDB9485),
    Color(0xFFA4B684),
    Color(0xFFD0A5AA),
    Color(0xFFE9C1A6),
    Color(0xFFCA99B0),
    Color(0xFFCA9999),
)

val LocalCategoryColors = staticCompositionLocalOf { LightCategoryColors }

/**
 * Color de acento fijo para fondos de avatar con glifo blanco: siempre el primary claro del acento,
 * también en oscuro (el primary oscuro es demasiado claro para sostener un glifo blanco a 3:1).
 */
val LocalAccentColor = staticCompositionLocalOf { FinanZenLightScheme.primary }
