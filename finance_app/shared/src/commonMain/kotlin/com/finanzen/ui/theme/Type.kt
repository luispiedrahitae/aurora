package com.finanzen.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import sage.shared.generated.resources.Res
import sage.shared.generated.resources.caprasimo_regular
import sage.shared.generated.resources.ibm_plex_mono_medium
import sage.shared.generated.resources.ibm_plex_mono_regular
import sage.shared.generated.resources.ibm_plex_sans_medium
import sage.shared.generated.resources.ibm_plex_sans_regular
import sage.shared.generated.resources.ibm_plex_sans_semibold

// Cifras tabulares ("tnum") para que los montos se alineen en columnas y no "salten" al animarse.
private const val TNUM = "tnum"

/**
 * Estilos de texto del design system Sage (ver `docs/DESIGN.md`). Son los únicos tamaños permitidos:
 * las pantallas eligen un rol, nunca un `fontSize` suelto.
 *
 * - Títulos de pantalla y de sección en Caprasimo (la identidad de Sage).
 * - Todo lo demás en IBM Plex Sans con cifras tabulares; datos de ejes en IBM Plex Mono.
 */
@Immutable
data class SageTextStyles(
    val titleScreen: TextStyle,
    val titleSection: TextStyle,
    val titleTopBar: TextStyle,
    val titleCard: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val overline: TextStyle,
    val amountHero: TextStyle,
    val amountLg: TextStyle,
    val amountMd: TextStyle,
    val amountSm: TextStyle,
    val data: TextStyle,
    val wordmark: TextStyle,
)

val LocalSageType = staticCompositionLocalOf<SageTextStyles> { error("SageTextStyles no provisto: envuelve en FinanZenTheme") }

/** Fuente de títulos (Caprasimo). `@Composable` porque las fuentes de Compose Resources se resuelven en composición. */
@Composable
fun displayFontFamily(): FontFamily = FontFamily(Font(Res.font.caprasimo_regular, FontWeight.Normal))

@Composable
private fun sansFontFamily(): FontFamily = FontFamily(
    Font(Res.font.ibm_plex_sans_regular, FontWeight.Normal),
    Font(Res.font.ibm_plex_sans_medium, FontWeight.Medium),
    Font(Res.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
)

@Composable
private fun monoFontFamily(): FontFamily = FontFamily(
    Font(Res.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(Res.font.ibm_plex_mono_medium, FontWeight.Medium),
)

@Composable
fun sageTextStyles(): SageTextStyles {
    val display = displayFontFamily()
    val sans = sansFontFamily()
    val mono = monoFontFamily()
    fun sans(size: Int, line: Int, weight: FontWeight) = TextStyle(
        fontFamily = sans,
        fontSize = size.sp,
        lineHeight = line.sp,
        fontWeight = weight,
        fontFeatureSettings = TNUM,
    )
    fun display(size: Int, line: Int) = TextStyle(
        fontFamily = display,
        fontSize = size.sp,
        lineHeight = line.sp,
        fontWeight = FontWeight.Normal,
    )
    return SageTextStyles(
        titleScreen = display(28, 34),
        titleSection = display(20, 26),
        titleTopBar = display(20, 26),
        titleCard = sans(16, 22, FontWeight.SemiBold),
        body = sans(16, 24, FontWeight.Normal),
        bodySmall = sans(14, 20, FontWeight.Normal),
        label = sans(14, 20, FontWeight.Medium),
        caption = sans(12, 16, FontWeight.Normal),
        overline = sans(11, 16, FontWeight.SemiBold).copy(letterSpacing = 0.06.em),
        amountHero = sans(40, 44, FontWeight.SemiBold),
        amountLg = sans(24, 28, FontWeight.SemiBold),
        amountMd = sans(16, 22, FontWeight.SemiBold),
        amountSm = sans(14, 20, FontWeight.Medium),
        data = TextStyle(
            fontFamily = mono,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Normal,
            fontFeatureSettings = TNUM,
        ),
        wordmark = display(40, 46),
    )
}

/**
 * Los roles de Material 3 apuntan a los estilos Sage para que los componentes de Material (botones,
 * campos, diálogos) hereden la misma escala sin configuración extra.
 */
fun SageTextStyles.toMaterialTypography(): Typography = Typography(
    displayLarge = amountHero,
    displayMedium = amountHero,
    displaySmall = amountLg,
    headlineLarge = titleScreen,
    headlineMedium = titleScreen,
    headlineSmall = titleSection,
    titleLarge = titleSection,
    titleMedium = titleCard,
    titleSmall = label.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = body,
    bodyMedium = bodySmall,
    bodySmall = caption,
    labelLarge = label,
    labelMedium = caption.copy(fontWeight = FontWeight.Medium),
    labelSmall = caption.copy(fontWeight = FontWeight.Medium),
)
