package com.finanzen.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Sistema Swiss Modernism 2.0 / OLED — ver DESIGN.md. Oscuro es fondo negro puro (#000000, no un
 * casi-negro con tinte) con una rampa tonal de 4 escalones para profundidad; claro es un blanco frío
 * verdadero, no crema cálido. El acento (teal) y los neutros conviven aquí; los otros 9 presets
 * reescriben solo la tríada de acento sobre esta base vía [withAccent].
 */
internal val FinanZenLightScheme = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA8F2DE),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF0F766E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFA8F2DE),
    onSecondaryContainer = Color(0xFF00201A),
    tertiary = Color(0xFF0F766E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFA8F2DE),
    onTertiaryContainer = Color(0xFF00201A),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF101012),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF101012),
    surfaceVariant = Color(0xFFE4E4E7),
    onSurfaceVariant = Color(0xFF55565C),
    surfaceDim = Color(0xFFE4E4E7),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F4F5),
    surfaceContainer = Color(0xFFEDEDEF),
    surfaceContainerHigh = Color(0xFFE4E4E7),
    surfaceContainerHighest = Color(0xFFDADADD),
    outline = Color(0xFF8A8B90),
    outlineVariant = Color(0xFFD4D4D8),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

internal val FinanZenDarkScheme = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF06211D),
    primaryContainer = Color(0xFF0F4A42),
    onPrimaryContainer = Color(0xFFA8F2DE),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF06211D),
    secondaryContainer = Color(0xFF0F4A42),
    onSecondaryContainer = Color(0xFFA8F2DE),
    tertiary = Color(0xFF2DD4BF),
    onTertiary = Color(0xFF06211D),
    tertiaryContainer = Color(0xFF0F4A42),
    onTertiaryContainer = Color(0xFFA8F2DE),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F2F3),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFF2F2F3),
    surfaceVariant = Color(0xFF1A1A1E),
    onSurfaceVariant = Color(0xFFA9AAB0),
    surfaceDim = Color(0xFF000000),
    surfaceBright = Color(0xFF232328),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0A0A0C),
    surfaceContainer = Color(0xFF111114),
    surfaceContainerHigh = Color(0xFF1A1A1E),
    surfaceContainerHighest = Color(0xFF232328),
    outline = Color(0xFF4A4B50),
    outlineVariant = Color(0xFF2E2F33),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/**
 * Colores semánticos de finanzas. Viven fuera del [androidx.compose.material3.ColorScheme] a propósito:
 * ingreso=verde / gasto=rojo deben sobrevivir al color dinámico Material You (que reescribe primary).
 * Ninguno es la única codificación de estado — cada uso va acompañado de un ícono o etiqueta (ver
 * DESIGN.md, regla "The No-Shame Rule": gasto/sobregiro nunca usa rojo de alarma ni parpadeo).
 *
 * [glassSurface]/[glassBorder] son el material del único tile de vidrio permitido por pantalla
 * (DESIGN.md, "The One Glass Tile Rule") — alpha ya incluido, no tonos planos de Material.
 */
data class FinanceColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val neutral: Color,
    val warning: Color,
    val warningContainer: Color,
    val glassSurface: Color,
    val glassBorder: Color,
)

internal val LightFinanceColors = FinanceColors(
    // 047857 (emerald-700), no 059669: a 600 solo da ~3.8:1 sobre blanco, bajo el mínimo AA de 4.5:1
    // para texto de tamaño normal (montos en MoneyText/KpiCard no son "texto grande" WCAG).
    income = Color(0xFF047857),
    incomeContainer = Color(0xFFCDEFDE),
    expense = Color(0xFFE11D48),
    expenseContainer = Color(0xFFFADDE2),
    neutral = Color(0xFF55565C),
    warning = Color(0xFFB45309),
    warningContainer = Color(0xFFFBEAC6),
    glassSurface = Color(0x8CFFFFFF),
    glassBorder = Color(0x33000000),
)

internal val DarkFinanceColors = FinanceColors(
    income = Color(0xFF34D399),
    incomeContainer = Color(0xFF1B3D30),
    expense = Color(0xFFFB7185),
    expenseContainer = Color(0xFF512530),
    neutral = Color(0xFFA9AAB0),
    warning = Color(0xFFFBBF24),
    warningContainer = Color(0xFF4A3410),
    glassSurface = Color(0x8C232328),
    glassBorder = Color(0x33FFFFFF),
)

val LocalFinanceColors = staticCompositionLocalOf { LightFinanceColors }

/**
 * Paleta de codificación de datos para los charts de categoría (donut, desglose): 12 matices
 * repartidos por la rueda de color, misma receta H/S/L dentro de cada tema (no Tailwind/Material
 * genérico) para que se sientan primas del acento y los semánticos — oscuro con L alta (brilla
 * sobre el negro OLED puro, igual que [DarkFinanceColors]/el acento en oscuro), claro con L baja
 * (contraste ≥3:1 sobre blanco, igual que [LightFinanceColors]/el acento en claro). Nunca la única
 * codificación: cada porción/fila va acompañada de nombre y porcentaje.
 */
val LightCategoryColors = listOf(
    Color(0xFFAA1830), // rose
    Color(0xFFAA4918), // orange
    Color(0xFF8F7A14), // amber
    Color(0xFF668F14), // lime
    Color(0xFF298F14), // green
    Color(0xFF18AA49), // emerald
    Color(0xFF18AA91), // teal
    Color(0xFF1879AA), // cyan
    Color(0xFF1830AA), // blue
    Color(0xFF4918AA), // indigo
    Color(0xFF9118AA), // violet
    Color(0xFFAA1879), // fuchsia
)

val DarkCategoryColors = listOf(
    Color(0xFFDC6A7D), // rose
    Color(0xFFDC906A), // orange
    Color(0xFFD4BD49), // amber
    Color(0xFFA6D449), // lime
    Color(0xFF60D449), // green
    Color(0xFF6ADC90), // emerald
    Color(0xFF6ADCC9), // teal
    Color(0xFF6AB6DC), // cyan
    Color(0xFF6A7DDC), // blue
    Color(0xFF906ADC), // indigo
    Color(0xFFC96ADC), // violet
    Color(0xFFDC6AB6), // fuchsia
)

val LocalCategoryColors = staticCompositionLocalOf { LightCategoryColors }

/**
 * Color de acento fijo para fondos de avatar (p.ej. [com.finanzen.ui.components.CategoryAvatar]).
 * Siempre el primary "claro" del acento elegido en Apariencia, nunca el de [FinanZenDarkScheme]:
 * en oscuro el primary se aclara para contrastar con superficies negras y deja de sostener un
 * glifo blanco encima (falla el 3:1 de WCAG 1.4.11). Ver [FinanZenTheme].
 */
val LocalAccentColor = staticCompositionLocalOf { FinanZenLightScheme.primary }
