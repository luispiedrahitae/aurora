package com.finanzen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.ui.theme.PillShape
import com.finanzen.viewmodel.BudgetRow

/**
 * Fila de presupuesto: nombre + "(%) gastado / de límite" + barra de progreso. Compartida por
 * la pestaña Presupuesto y la sección de presupuestos del Resumen. El sobregiro pasa a rojo de gasto
 * (monto, %, barra) acompañado de la pill "Excedido", para no depender solo del color.
 */
@Composable
fun BudgetProgressCard(row: BudgetRow, currency: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    // Límite 0 (no debería guardarse, pero el Resumen no filtra): sin progreso en vez de dividir por cero.
    val hasLimit = row.limitMinor > 0
    val fraction = if (hasLimit) (row.spentMinor.toFloat() / row.limitMinor.toFloat()).coerceIn(0f, 1f) else 0f
    val pct = if (hasLimit) (row.spentMinor * 100 / row.limitMinor).toInt() else 0
    val over = hasLimit && row.spentMinor > row.limitMinor
    // Aviso temprano en ámbar desde el 90%: llegar al tope no debe ser una sorpresa.
    val near = !over && pct >= NEAR_LIMIT_PCT
    val finance = LocalFinanceColors.current
    val spacing = LocalSpacing.current
    val fmt = LocalMoneyFormat.current
    val barColor = when {
        over -> finance.expense
        near -> finance.warning
        else -> MaterialTheme.colorScheme.primary
    }

    FinanceCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(row.categoryName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    row.parentName?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (over || near) {
                    Text(
                        if (over) "Excedido" else "Cerca del límite",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (over) finance.expense else finance.warning,
                        softWrap = false,
                        modifier = Modifier
                            .background(if (over) finance.expenseContainer else finance.warningContainer, PillShape)
                            .padding(horizontal = spacing.sm, vertical = spacing.xs),
                    )
                }
                // "(45%) $13.500 de $30.000" en un solo renglón, al tamaño de los montos de Cuentas
                // (bodyMedium) y en negrita. Gastado: verde dentro del límite, rojo al excederse.
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "($pct%)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (over) finance.expense else MaterialTheme.colorScheme.onSurface,
                        softWrap = false,
                    )
                    MoneyText(
                        amountMinor = row.spentMinor,
                        currency = currency,
                        style = MaterialTheme.typography.bodyMedium,
                        colorOverride = if (over) finance.expense else finance.income,
                    )
                    Text(
                        "de ${fmt.format(row.limitMinor, currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = false,
                    )
                }
            }
            ProgressBar(fraction, color = barColor)
        }
    }
}

private const val NEAR_LIMIT_PCT = 90
