package com.finanzen.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Fallback teal refinado (cuando no hay color dinámico Material You). Paleta tonal M3 más completa
 * que la original — incluye contenedores, superficies y roles de error, para que componentes M3
 * (chips, badges, dialogs) tengan colores coherentes sin parches sueltos.
 */
internal val FinanZenLightScheme = lightColorScheme(
    primary = Color(0xFF1E6F5C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA8F2DE),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF4A635D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE8E0),
    onSecondaryContainer = Color(0xFF06201B),
    tertiary = Color(0xFF42618C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD4E3FF),
    onTertiaryContainer = Color(0xFF001C39),
    background = Color(0xFFF7FBF8),
    onBackground = Color(0xFF171D1B),
    surface = Color(0xFFF7FBF8),
    onSurface = Color(0xFF171D1B),
    surfaceVariant = Color(0xFFDBE5E0),
    onSurfaceVariant = Color(0xFF3F4945),
    surfaceContainer = Color(0xFFECF2EE),
    surfaceContainerHigh = Color(0xFFE6ECE8),
    outline = Color(0xFF6F7975),
    outlineVariant = Color(0xFFBFC9C4),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

internal val FinanZenDarkScheme = darkColorScheme(
    primary = Color(0xFF8CDABF),
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF005245),
    onPrimaryContainer = Color(0xFFA8F2DE),
    secondary = Color(0xFFB1CCC4),
    onSecondary = Color(0xFF1C352F),
    secondaryContainer = Color(0xFF324B45),
    onSecondaryContainer = Color(0xFFCDE8E0),
    tertiary = Color(0xFFA8C8FF),
    onTertiary = Color(0xFF09305B),
    tertiaryContainer = Color(0xFF294873),
    onTertiaryContainer = Color(0xFFD4E3FF),
    background = Color(0xFF0E1513),
    onBackground = Color(0xFFDDE4E0),
    surface = Color(0xFF0E1513),
    onSurface = Color(0xFFDDE4E0),
    surfaceVariant = Color(0xFF3F4945),
    onSurfaceVariant = Color(0xFFBFC9C4),
    surfaceContainer = Color(0xFF1A211E),
    surfaceContainerHigh = Color(0xFF242B28),
    outline = Color(0xFF89938E),
    outlineVariant = Color(0xFF3F4945),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/**
 * Colores semánticos de finanzas. Viven fuera del [androidx.compose.material3.ColorScheme] a propósito:
 * ingreso=verde / gasto=rojo deben sobrevivir al color dinámico Material You (que reescribe primary).
 */
data class FinanceColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val neutral: Color,
)

internal val LightFinanceColors = FinanceColors(
    income = Color(0xFF1E7D5A),
    incomeContainer = Color(0xFFCDEFDE),
    expense = Color(0xFFB13E53),
    expenseContainer = Color(0xFFFADDE2),
    neutral = Color(0xFF5A6661),
)

internal val DarkFinanceColors = FinanceColors(
    income = Color(0xFF7FD8AE),
    incomeContainer = Color(0xFF1B3D30),
    expense = Color(0xFFFF9DAE),
    expenseContainer = Color(0xFF512530),
    neutral = Color(0xFFA3AEA8),
)

val LocalFinanceColors = staticCompositionLocalOf { LightFinanceColors }
