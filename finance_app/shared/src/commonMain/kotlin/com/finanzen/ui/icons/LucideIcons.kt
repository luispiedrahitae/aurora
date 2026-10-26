package com.finanzen.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.painterResource
import sage.shared.generated.resources.Res
import sage.shared.generated.resources.ic_lucide_alert_triangle
import sage.shared.generated.resources.ic_lucide_archive
import sage.shared.generated.resources.ic_lucide_archive_restore
import sage.shared.generated.resources.ic_lucide_arrow_left
import sage.shared.generated.resources.ic_lucide_arrow_left_right
import sage.shared.generated.resources.ic_lucide_backspace
import sage.shared.generated.resources.ic_lucide_banknote
import sage.shared.generated.resources.ic_lucide_bell
import sage.shared.generated.resources.ic_lucide_calendar
import sage.shared.generated.resources.ic_lucide_check
import sage.shared.generated.resources.ic_lucide_chevron_down
import sage.shared.generated.resources.ic_lucide_chevron_left
import sage.shared.generated.resources.ic_lucide_chevron_right
import sage.shared.generated.resources.ic_lucide_chevron_up
import sage.shared.generated.resources.ic_lucide_chevrons_up_down
import sage.shared.generated.resources.ic_lucide_credit_card
import sage.shared.generated.resources.ic_lucide_download
import sage.shared.generated.resources.ic_lucide_eye
import sage.shared.generated.resources.ic_lucide_eye_off
import sage.shared.generated.resources.ic_lucide_file_text
import sage.shared.generated.resources.ic_lucide_info
import sage.shared.generated.resources.ic_lucide_landmark
import sage.shared.generated.resources.ic_lucide_layout_dashboard
import sage.shared.generated.resources.ic_lucide_lock
import sage.shared.generated.resources.ic_lucide_minus
import sage.shared.generated.resources.ic_lucide_more_horizontal
import sage.shared.generated.resources.ic_lucide_palette
import sage.shared.generated.resources.ic_lucide_pencil
import sage.shared.generated.resources.ic_lucide_pie_chart
import sage.shared.generated.resources.ic_lucide_plus
import sage.shared.generated.resources.ic_lucide_receipt
import sage.shared.generated.resources.ic_lucide_repeat
import sage.shared.generated.resources.ic_lucide_search
import sage.shared.generated.resources.ic_lucide_shield
import sage.shared.generated.resources.ic_lucide_tag
import sage.shared.generated.resources.ic_lucide_trash
import sage.shared.generated.resources.ic_lucide_trending_up
import sage.shared.generated.resources.ic_lucide_wallet
import sage.shared.generated.resources.ic_lucide_x

/**
 * Accesores `@Composable` para los 39 iconos Lucide-style empaquetados en
 * `composeResources/drawable/ic_lucide_*.xml` (Fase 1 del rediseño Sage). Son vectores monocromos
 * de solo trazo (fill transparente, strokeWidth 1.75) pensados para pintarse con `Icon(tint = ...)`,
 * igual que un ícono Material. Ningún call site se ha migrado todavía a este objeto — eso es una
 * fase posterior, pantalla por pantalla.
 *
 * No hay accesor para `fingerprint`: Lucide no tiene ese glifo, así que ese único punto de uso sigue
 * con `Icons.Filled.Fingerprint` de Material a propósito.
 */
object LucideIcons {
    val ArrowLeft: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_arrow_left)
    val ChevronLeft: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_chevron_left)
    val ChevronRight: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_chevron_right)
    val ChevronUp: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_chevron_up)
    val ChevronDown: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_chevron_down)
    val ChevronsUpDown: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_chevrons_up_down)
    val Plus: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_plus)
    val X: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_x)
    val Check: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_check)
    val Pencil: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_pencil)
    val Archive: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_archive)
    val ArchiveRestore: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_archive_restore)
    val Trash: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_trash)
    val Eye: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_eye)
    val EyeOff: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_eye_off)
    val Search: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_search)
    val Wallet: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_wallet)
    val Landmark: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_landmark)
    val CreditCard: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_credit_card)
    val PieChart: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_pie_chart)
    val ArrowLeftRight: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_arrow_left_right)
    val LayoutDashboard: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_layout_dashboard)
    val MoreHorizontal: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_more_horizontal)
    val Repeat: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_repeat)
    val TrendingUp: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_trending_up)
    val Calendar: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_calendar)
    val FileText: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_file_text)
    val Download: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_download)
    val Tag: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_tag)
    val Bell: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_bell)
    val Shield: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_shield)
    val Palette: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_palette)
    val Banknote: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_banknote)
    val Info: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_info)
    val AlertTriangle: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_alert_triangle)
    val Lock: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_lock)
    val Backspace: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_backspace)
    val Receipt: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_receipt)
    val Minus: Painter @Composable get() = painterResource(Res.drawable.ic_lucide_minus)
}
