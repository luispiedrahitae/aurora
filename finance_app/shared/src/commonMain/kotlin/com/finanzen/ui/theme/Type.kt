package com.finanzen.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import finanzen.shared.generated.resources.Res
import finanzen.shared.generated.resources.inter_bold
import finanzen.shared.generated.resources.inter_medium
import finanzen.shared.generated.resources.inter_regular
import finanzen.shared.generated.resources.inter_semibold
import org.jetbrains.compose.resources.Font

// Cifras tabulares ("tnum") para que los montos no "salten" durante los contadores animados.
private const val TNUM = "tnum"

/**
 * Escala tipográfica de marca sobre Inter (empaquetada en composeResources/font). `displayLarge` se
 * hace más expresivo para el saldo principal; todas las familias llevan cifras tabulares. Es
 * `@Composable` porque [Font] de Compose Resources solo se resuelve en composición.
 */
@Composable
fun finanZenTypography(): Typography {
    val inter = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
        Font(Res.font.inter_bold, FontWeight.Bold),
    )
    val b = Typography()
    return Typography(
        displayLarge = b.displayLarge.copy(
            fontFamily = inter,
            fontSize = 52.sp,
            lineHeight = 56.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
            fontFeatureSettings = TNUM,
        ),
        displayMedium = b.displayMedium.copy(fontFamily = inter, fontFeatureSettings = TNUM),
        displaySmall = b.displaySmall.copy(fontFamily = inter, fontFeatureSettings = TNUM),
        headlineLarge = b.headlineLarge.copy(fontFamily = inter, fontFeatureSettings = TNUM),
        headlineMedium = b.headlineMedium.copy(fontFamily = inter, fontFeatureSettings = TNUM),
        headlineSmall = b.headlineSmall.copy(fontFamily = inter, fontFeatureSettings = TNUM),
        titleLarge = b.titleLarge.copy(fontFamily = inter, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM),
        titleMedium = b.titleMedium.copy(fontFamily = inter, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM),
        titleSmall = b.titleSmall.copy(fontFamily = inter, fontFeatureSettings = TNUM),
        bodyLarge = b.bodyLarge.copy(fontFamily = inter, fontFeatureSettings = TNUM),
        bodyMedium = b.bodyMedium.copy(fontFamily = inter, fontFeatureSettings = TNUM),
        bodySmall = b.bodySmall.copy(fontFamily = inter, fontFeatureSettings = TNUM),
        labelLarge = b.labelLarge.copy(fontFamily = inter, fontWeight = FontWeight.Medium),
        labelMedium = b.labelMedium.copy(fontFamily = inter),
        labelSmall = b.labelSmall.copy(fontFamily = inter),
    )
}
