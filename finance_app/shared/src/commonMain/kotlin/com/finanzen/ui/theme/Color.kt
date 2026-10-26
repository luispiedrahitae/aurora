package com.finanzen.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Paleta Sage (Fase 1 del rediseño) — claro es crema cálido (`#F5EAD8`) con acento terracotta;
 * oscuro es un orgánico cálido (`#221F19`), no negro OLED puro. El acento terracotta y los neutros
 * conviven aquí; el preset Sage reescribe solo la tríada de acento sobre esta base vía [withAccent].
 */
internal val FinanZenLightScheme = lightColorScheme(
    // Oscurecido desde 0xFFC67139 (mismo H/S, ΔL≈6.2) para llegar a 4.5:1 blanco-sobre-primary —
    // ver LocalAccentColor / el check blanco de AccentSwatch.
    primary = Color(0xFFAD6332),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF7EBE3),
    onPrimaryContainer = Color(0xFF7F4824),
    secondary = Color(0xFF79895D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7EAE1),
    onSecondaryContainer = Color(0xFF4B553A),
    tertiary = Color(0xFF79895D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE7EAE1),
    onTertiaryContainer = Color(0xFF4B553A),
    background = Color(0xFFF5EAD8),
    onBackground = Color(0xFF201E1D),
    surface = Color(0xFFFAF3E6),
    onSurface = Color(0xFF201E1D),
    // surfaceVariant es el track de las barras de progreso (AccountBalanceBars, SavingsRateGauge,
    // BudgetProgressCard) — antes coincidía en valor con surfaceContainerHigh, lo que era inofensivo
    // mientras las tarjetas Medium usaban un tono distinto (surfaceContainer) pero se volvió
    // invisible al unificar todas las tarjetas a ese mismo tono (ver surfaceContainer* abajo). El
    // pixel real del handoff (03-cuentas.png, barra "Cupo") mide ~#EDE6D4 — muy cerca del antiguo
    // surfaceContainer, que se reutiliza aquí.
    surfaceVariant = Color(0xFFF2E6D2),
    onSurfaceVariant = Color(0xFF5B5450),
    surfaceDim = Color(0xFFECDCC2),
    surfaceBright = Color(0xFFFAF3E6),
    surfaceContainerLowest = Color(0xFFFAF3E6),
    // surfaceContainerLow/Container comparten el tono de ContainerHigh: los screenshots del handoff
    // (01-resumen.png, 02-movimientos.png) muestran una única tarjeta ~#EADEC8 para hero/KPI/día,
    // no una escala de 3 tonos crecientes — la escala M3 de 5 pasos aquí quedaba demasiado clara.
    surfaceContainerLow = Color(0xFFECDCC2),
    surfaceContainer = Color(0xFFECDCC2),
    surfaceContainerHigh = Color(0xFFECDCC2),
    surfaceContainerHighest = Color(0xFFE4CDA9),
    outline = Color(0xFFD8C9AE),
    outlineVariant = Color(0xFFD8C9AE),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

internal val FinanZenDarkScheme = darkColorScheme(
    primary = Color(0xFFD79B74),
    onPrimary = Color(0xFF372010),
    primaryContainer = Color(0xFF573219),
    onPrimaryContainer = Color(0xFFE4BBA0),
    secondary = Color(0xFF99A880),
    onSecondary = Color(0xFF23271B),
    secondaryContainer = Color(0xFF363D29),
    onSecondaryContainer = Color(0xFFBBC5AA),
    tertiary = Color(0xFF99A880),
    onTertiary = Color(0xFF23271B),
    tertiaryContainer = Color(0xFF363D29),
    onTertiaryContainer = Color(0xFFBBC5AA),
    background = Color(0xFF221F19),
    onBackground = Color(0xFFFBF7F0),
    surface = Color(0xFF2B2720),
    onSurface = Color(0xFFFBF7F0),
    // Ver nota equivalente en FinanZenLightScheme: track distinto del tono ya unificado de tarjeta.
    surfaceVariant = Color(0xFF382F23),
    onSurfaceVariant = Color(0xFFC4B9A5),
    surfaceDim = Color(0xFF221F19),
    surfaceBright = Color(0xFF4A4030),
    surfaceContainerLowest = Color(0xFF221F19),
    surfaceContainerLow = Color(0xFF413B2F),
    surfaceContainer = Color(0xFF413B2F),
    surfaceContainerHigh = Color(0xFF413B2F),
    surfaceContainerHighest = Color(0xFF4A4030),
    outline = Color(0xFF5A4F3C),
    outlineVariant = Color(0xFF5A4F3C),
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
 */
data class FinanceColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val neutral: Color,
    val warning: Color,
    val warningContainer: Color,
)

internal val LightFinanceColors = FinanceColors(
    // Sage 700 (ingreso) / terracotta 700 (gasto) — ver Task 2 del brief de rediseño Sage.
    income = Color(0xFF4B553A),
    incomeContainer = Color(0xFFCDEFDE),
    expense = Color(0xFF7F4824),
    expenseContainer = Color(0xFFFADDE2),
    neutral = Color(0xFF55565C),
    warning = Color(0xFFB45309),
    warningContainer = Color(0xFFFBEAC6),
)

internal val DarkFinanceColors = FinanceColors(
    // Sage 300 (ingreso) / terracotta 300 (gasto) — ver Task 2 del brief de rediseño Sage.
    income = Color(0xFFBBC5AA),
    incomeContainer = Color(0xFF1B3D30),
    expense = Color(0xFFE4BBA0),
    expenseContainer = Color(0xFF512530),
    neutral = Color(0xFFA9AAB0),
    warning = Color(0xFFFBBF24),
    warningContainer = Color(0xFF4A3410),
)

val LocalFinanceColors = staticCompositionLocalOf { LightFinanceColors }

/**
 * Paleta de codificación de datos para los charts de categoría (donut, desglose): 12 matices, todos
 * dentro de la banda cálida de la marca (terracotta/ochre/olive/moss/fern en el lado verde-amarillo,
 * rose/clay/amber/sienna en el lado rojo-naranja) — nunca azules, violetas ni cianes, para que cada
 * categoría lea como prima del acento y los semánticos en vez de un arcoíris genérico (la versión
 * previa cubría toda la rueda de 360° incluyendo teal/slate/denim/plum/mauve, que seguían leyéndose
 * frías pese a la mezcla). Oscuro con L alta (contraste sobre el fondo orgánico oscuro, igual que
 * [DarkFinanceColors]/el acento en oscuro), claro con L baja (contraste ≥3:1 sobre el fondo crema,
 * igual que [LightFinanceColors]/el acento en claro). Nunca la única codificación: cada
 * porción/fila va acompañada de nombre y porcentaje.
 */
val LightCategoryColors = listOf(
    Color(0xFF875F44), // terracotta
    Color(0xFF877F44), // ochre
    Color(0xFF6E8644), // olive
    Color(0xFF4E8644), // moss
    Color(0xFF47865E), // fern
    Color(0xFF8C6B32), // amber
    Color(0xFF954935), // sienna
    Color(0xFF5E6B34), // dark olive
    Color(0xFF8A5A5A), // dusty rose
    Color(0xFFA37656), // light terracotta
    Color(0xFF87456B), // rose
    Color(0xFF87454B), // clay
)

val DarkCategoryColors = listOf(
    Color(0xFFCAAC94), // terracotta
    Color(0xFFCAC394), // ochre
    Color(0xFFB8C794), // olive
    Color(0xFFA1C794), // moss
    Color(0xFF9CC7A7), // fern
    Color(0xFFD2B682), // amber
    Color(0xFFDB9485), // sienna
    Color(0xFFA4B684), // dark olive
    Color(0xFFD0A5AA), // dusty rose
    Color(0xFFE9C1A6), // light terracotta
    Color(0xFFCA99B0), // rose
    Color(0xFFCA9999), // clay
)

val LocalCategoryColors = staticCompositionLocalOf { LightCategoryColors }

/**
 * Color de acento fijo para fondos de avatar (p.ej. [com.finanzen.ui.components.CategoryAvatar]).
 * Siempre el primary "claro" del acento elegido en Apariencia, nunca el de [FinanZenDarkScheme]:
 * en oscuro el primary se aclara para contrastar con superficies negras y deja de sostener un
 * glifo blanco encima (falla el 3:1 de WCAG 1.4.11). Ver [FinanZenTheme].
 */
val LocalAccentColor = staticCompositionLocalOf { FinanZenLightScheme.primary }
