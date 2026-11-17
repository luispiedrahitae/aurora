package com.finanzen.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.finanzen.domain.Money
import com.finanzen.ui.theme.FinanceColors
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import kotlin.math.roundToInt

/**
 * Formatea [Money] con cifras tabulares y color por signo (verde ingreso / rojo gasto).
 * Centraliza el formateo de moneda — antes repetido en cada pantalla con interpolación a mano.
 */
@Composable
fun MoneyText(
    amountMinor: Long,
    currency: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    fontWeight: FontWeight? = FontWeight.SemiBold,
    signed: Boolean = false,
    colorOverride: Color? = null,
    textAlign: TextAlign? = null,
) {
    val color = colorOverride ?: if (signed) balanceColor(amountMinor) else MaterialTheme.colorScheme.onSurface
    // El símbolo y su posición ($ antes/después/ninguno) los aporta el formato global.
    val text = LocalMoneyFormat.current.format(amountMinor, currency, signed)
    Text(text, modifier = modifier, style = style, fontWeight = fontWeight, color = color, textAlign = textAlign)
}

/** Color de un saldo o monto con signo en toda la app: positivo = ingreso (verde), negativo =
 * gasto (rojo), cero = neutro. */
@Composable
fun balanceColor(amountMinor: Long): Color {
    val finance = LocalFinanceColors.current
    return when {
        amountMinor > 0 -> finance.income
        amountMinor < 0 -> finance.expense
        else -> MaterialTheme.colorScheme.onSurface
    }
}

/**
 * Texto que se encoge hasta caber en **una sola línea** dentro del ancho disponible. Mide con
 * [rememberTextMeasurer] bajando de [maxFontSize] a [minFontSize] hasta que entra. Útil para el balance
 * del Dashboard, que debe quedar en un renglón sin importar la magnitud.
 */
@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit = 40.sp,
    minFontSize: TextUnit = 16.sp,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    style: TextStyle = MaterialTheme.typography.displaySmall,
) {
    BoxWithConstraints(modifier) {
        val measurer = rememberTextMeasurer()
        val maxWidthPx = constraints.maxWidth
        val fitted = remember(text, maxWidthPx, maxFontSize, minFontSize, style) {
            var size = maxFontSize
            while (size.value > minFontSize.value) {
                val width = measurer.measure(
                    text = AnnotatedString(text),
                    style = style.copy(fontSize = size, fontWeight = fontWeight ?: style.fontWeight),
                    maxLines = 1,
                    softWrap = false,
                ).size.width
                if (width <= maxWidthPx) break
                size = (size.value - 1f).sp
            }
            size
        }
        Text(
            text,
            style = style,
            fontSize = fitted,
            fontWeight = fontWeight,
            color = color,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/**
 * Monto con signo correcto y color semántico según el tipo de movimiento, para filas de solo
 * lectura (Movimientos, Calendario, detalle de cuenta). ADJUSTMENT ya trae el signo correcto en
 * `amountMinor` (positivo = sube el saldo, negativo = lo baja) — a diferencia de EXPENSE, que se
 * niega aquí para mostrarse en rojo.
 */
fun signedAmountAndColor(kind: String, amountMinor: Long, finance: FinanceColors): Pair<Long, Color> {
    val isIncome = kind == "INCOME"
    val isAdjustment = kind == "ADJUSTMENT"
    val signedAmount = if (isIncome || isAdjustment) amountMinor else -amountMinor
    val color = when {
        isAdjustment -> if (amountMinor >= 0) finance.income else finance.expense
        isIncome -> finance.income
        else -> finance.expense
    }
    return signedAmount to color
}

/** Variación relativa de un KPI frente al período anterior ("+12% vs 2025"). [isGood] decide el
 * color semántico: subir es bueno en ingresos y malo en gastos. */
data class KpiDelta(val pct: Int, val isGood: Boolean, val vsLabel: String)

/** Delta porcentual [current] vs [previous]; null sin base de comparación (período anterior en 0). */
fun kpiDelta(current: Long, previous: Long, vsLabel: String, upIsGood: Boolean): KpiDelta? {
    if (previous <= 0L) return null
    val pct = ((current - previous) * 100.0 / previous).roundToInt()
    val isGood = if (pct >= 0) upIsGood else !upIsGood
    return KpiDelta(pct = pct, isGood = isGood, vsLabel = vsLabel)
}
