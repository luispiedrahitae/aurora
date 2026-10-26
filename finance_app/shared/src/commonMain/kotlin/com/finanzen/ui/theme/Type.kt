package com.finanzen.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import sage.shared.generated.resources.Res
import sage.shared.generated.resources.caprasimo_regular
import sage.shared.generated.resources.figtree_bold
import sage.shared.generated.resources.figtree_medium
import sage.shared.generated.resources.figtree_regular
import sage.shared.generated.resources.figtree_semibold

// Cifras tabulares ("tnum") para que los montos no "salten" durante los contadores animados.
private const val TNUM = "tnum"

/**
 * Fuente display (Caprasimo): el wordmark "Sage", todo título de pantalla — las 5 pestañas
 * principales ([com.finanzen.ui.components.MainTabHeader]) y las sub-pantallas
 * ([com.finanzen.ui.components.SubScreenHeader], [com.finanzen.ui.components.SheetHeader]) — los
 * subtítulos de sección dentro de pantalla ([com.finanzen.ui.components.SectionHeader], a 15sp en
 * vez del rol `label`/`title` — ver su propio comentario) y las etiquetas de CTA/FAB primario
 * ([com.finanzen.ui.components.AddFab], a 14sp en vez del rol `body`). El resto de la UI (cuerpo,
 * formularios, datos) se queda en Figtree; las capturas del handoff (Sage.dc.html) muestran una
 * serif/slab bold clara en encabezados y CTAs, distinta del sans usado en el resto de la interfaz.
 */
@Composable
fun displayFontFamily(): FontFamily = FontFamily(Font(Res.font.caprasimo_regular, FontWeight.Normal))

/**
 * Escala tipográfica de marca sobre Figtree (única familia del cuerpo de la UI; Inter sigue
 * definida para casos legacy pero ya no es la por defecto), empaquetada en composeResources/font.
 * Todas las familias llevan cifras tabulares. Es `@Composable` porque [Font] de Compose Resources
 * solo se resuelve en composición.
 */
@Composable
fun finanZenTypography(): Typography {
    val figtree = FontFamily(
        Font(Res.font.figtree_regular, FontWeight.Normal),
        Font(Res.font.figtree_medium, FontWeight.Medium),
        Font(Res.font.figtree_semibold, FontWeight.SemiBold),
        Font(Res.font.figtree_bold, FontWeight.Bold),
    )
    val b = Typography()

    // Escala fija (DESIGN.md §3): un tamaño por rol, replicado en los 3 slots Large/Medium/Small
    // de cada familia Material3 — ningún pantalla debe recortar/agrandar con `.copy(fontSize = ...)`.
    val display = b.displayLarge.copy(
        fontFamily = figtree,
        fontSize = 52.sp,
        lineHeight = 56.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = TNUM,
    )
    val headline = b.headlineLarge.copy(
        fontFamily = figtree,
        fontSize = 24.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = TNUM,
    )
    val title = b.titleLarge.copy(
        fontFamily = figtree,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = TNUM,
    )
    // Body Regular es el rol documentado para dinero/prosa. [MoneyText] ya aporta el énfasis del
    // dinero en listas vía su propio default `fontWeight = FontWeight.SemiBold` -- no dupliques
    // esa excepción aquí encima del rol Body, o los textos de prosa que usan bodyMedium/Large/Small
    // heredarían el peso equivocado.
    val body = b.bodyLarge.copy(
        fontFamily = figtree,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
        fontFeatureSettings = TNUM,
    )
    val label = b.labelLarge.copy(
        fontFamily = figtree,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.02.em,
        fontFeatureSettings = TNUM,
    )
    return Typography(
        displayLarge = display,
        displayMedium = display,
        displaySmall = display,
        headlineLarge = headline,
        headlineMedium = headline,
        headlineSmall = headline,
        titleLarge = title,
        titleMedium = title,
        titleSmall = title,
        bodyLarge = body,
        bodyMedium = body,
        bodySmall = body,
        labelLarge = label,
        labelMedium = label,
        labelSmall = label,
    )
}
