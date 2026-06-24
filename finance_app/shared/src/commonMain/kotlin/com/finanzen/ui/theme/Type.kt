package com.finanzen.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ponytail: cifras tabulares ("tnum") sobre la fuente del sistema en vez de empaquetar Inter.
// Cubre el objetivo real ("los montos no bailan al animar") sin añadir un asset de fuente variable
// + accesor generado por composeResources. Upgrade: registrar Inter en composeResources/font/ si la
// tipografía de marca llega a importar.
private const val TNUM = "tnum"

/**
 * Escala tipográfica moderna. `displayLarge` se hace más expresivo para el saldo principal; las
 * familias usadas para montos llevan cifras tabulares para que no "salten" durante los contadores
 * animados.
 */
private val base = Typography()

val FinanZenTypography = base.copy(
    displayLarge = base.displayLarge.copy(
        fontSize = 52.sp,
        lineHeight = 56.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = TNUM,
    ),
    displayMedium = base.displayMedium.copy(fontFeatureSettings = TNUM),
    displaySmall = base.displaySmall.copy(fontFeatureSettings = TNUM),
    headlineLarge = base.headlineLarge.copy(fontFeatureSettings = TNUM),
    headlineMedium = base.headlineMedium.copy(fontFeatureSettings = TNUM),
    headlineSmall = base.headlineSmall.copy(fontFeatureSettings = TNUM),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM),
    titleSmall = base.titleSmall.copy(fontFeatureSettings = TNUM),
    bodyLarge = base.bodyLarge.copy(fontFeatureSettings = TNUM),
    bodyMedium = base.bodyMedium.copy(fontFeatureSettings = TNUM),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium),
)

/** Estilo dedicado al monto del saldo hero (cifras tabulares ya incluidas vía displayLarge). */
val MoneyHeroStyle: TextStyle get() = FinanZenTypography.displayLarge
