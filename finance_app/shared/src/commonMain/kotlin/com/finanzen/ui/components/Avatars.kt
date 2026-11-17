package com.finanzen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.PriceChange
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.SageRadius

/** Proporción del glifo y de la inicial respecto del lado del avatar. */
private const val GLYPH_SCALE = 0.5f
private const val INITIAL_SCALE = 0.42f

/** Tono del avatar: acento (por defecto), ingreso o gasto. */
enum class AvatarTone { Accent, Income, Expense }

/**
 * Avatar Sage: cuadrado de 40 dp con radio 10, fondo suave y el glifo en el color pleno del tono.
 * Ingreso y gasto usan su propio par de colores; todo lo demás, el acento de Apariencia.
 */
@Composable
private fun AvatarBox(tone: AvatarTone, size: Dp, modifier: Modifier, content: @Composable (Color) -> Unit) {
    val finance = LocalFinanceColors.current
    val scheme = MaterialTheme.colorScheme
    val (bg, fg) = when (tone) {
        AvatarTone.Accent -> scheme.primaryContainer to scheme.onPrimaryContainer
        AvatarTone.Income -> finance.incomeContainer to finance.income
        AvatarTone.Expense -> finance.expenseContainer to finance.expense
    }
    Box(
        modifier.size(size).clip(RoundedCornerShape(SageRadius.md)).background(bg),
        contentAlignment = Alignment.Center,
    ) { content(fg) }
}

/** Inicial de la categoría sobre el fondo suave del acento: la categoría se distingue por su nombre. */
@Composable
fun CategoryAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 40.dp, tone: AvatarTone = AvatarTone.Accent) {
    AvatarBox(tone, size, modifier) { fg ->
        Text(
            text = name.trim().take(1).uppercase(),
            color = fg,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * INITIAL_SCALE).sp,
        )
    }
}

/** Avatar por tipo de movimiento cuando no hay categoría: flecha arriba (ingreso), abajo (gasto), etc. */
@Composable
fun KindAvatar(kind: String, modifier: Modifier = Modifier, size: Dp = 40.dp, icon: ImageVector? = null) {
    val resolvedIcon = icon ?: when (kind) {
        "INCOME" -> Icons.Outlined.ArrowUpward
        "TRANSFER" -> Icons.Outlined.SwapHoriz
        "ADJUSTMENT" -> Icons.Outlined.PriceChange
        else -> Icons.Outlined.ArrowDownward
    }
    val tone = when (kind) {
        "INCOME" -> AvatarTone.Income
        "EXPENSE" -> AvatarTone.Expense
        else -> AvatarTone.Accent
    }
    IconAvatar(resolvedIcon, modifier, size, tone)
}

/** Avatar por tipo de cuenta (efectivo, débito, ahorros, crédito). */
@Composable
fun AccountTypeAvatar(type: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    IconAvatar(accountTypeIcon(type), modifier, size)
}

/** Avatar con un icono cualquiera (suscripciones, fondos, opciones de menú). */
@Composable
fun IconAvatar(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 40.dp, tone: AvatarTone = AvatarTone.Accent) {
    AvatarBox(tone, size, modifier) { fg ->
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(size * GLYPH_SCALE))
    }
}

/** Variante de [IconAvatar] para iconos Lucide ([Painter]). */
@Composable
fun IconAvatar(icon: Painter, modifier: Modifier = Modifier, size: Dp = 40.dp, tone: AvatarTone = AvatarTone.Accent) {
    AvatarBox(tone, size, modifier) { fg ->
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(size * GLYPH_SCALE))
    }
}
