package com.finanzen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSageType
import com.finanzen.viewmodel.BudgetRow

/** A partir de qué porcentaje del límite se avisa (en ámbar) antes de llegar al tope. */
private const val NEAR_LIMIT_PCT = 85

/**
 * Presupuesto de una categoría: nombre y "gastado de límite" arriba, barra debajo y, según el estado,
 * una insignia ("85 % usado", "Límite alcanzado", "$ X por encima") o lo que queda. El estado nunca
 * depende solo del color: la insignia lo dice con palabras y un icono.
 */
@Composable
fun BudgetProgressItem(row: BudgetRow, currency: String, modifier: Modifier = Modifier) {
    val fmt = LocalMoneyFormat.current
    // Límite 0 (no debería guardarse, pero el Resumen no filtra): sin progreso en vez de dividir por cero.
    val hasLimit = row.limitMinor > 0
    val fraction = if (hasLimit) (row.spentMinor.toFloat() / row.limitMinor.toFloat()).coerceIn(0f, 1f) else 0f
    val pct = if (hasLimit) (row.spentMinor * 100 / row.limitMinor).toInt() else 0
    val over = hasLimit && row.spentMinor > row.limitMinor
    val atLimit = hasLimit && row.spentMinor == row.limitMinor
    val near = !over && !atLimit && pct >= NEAR_LIMIT_PCT
    val tone = when {
        over -> ProgressTone.Over
        atLimit || near -> ProgressTone.Warning
        else -> ProgressTone.Normal
    }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val ink = MaterialTheme.colorScheme.onSurface

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(row.categoryName, style = LocalSageType.current.bodySmall)
                row.parentName?.let { Text(it, style = LocalSageType.current.caption, color = muted) }
            }
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = ink, fontWeight = FontWeight.SemiBold)) { append(fmt.format(row.spentMinor, currency)) }
                    append(" de ${fmt.format(row.limitMinor, currency)}")
                },
                style = LocalSageType.current.bodySmall,
                color = muted,
            )
        }
        ProgressBar(fraction, color = progressColor(tone))
        when {
            over -> SageBadge("${fmt.format(row.spentMinor - row.limitMinor, currency)} por encima", tone = BadgeTone.Expense, icon = LucideIcons.AlertTriangle)
            atLimit -> SageBadge("Límite alcanzado", tone = BadgeTone.Warning, icon = LucideIcons.AlertTriangle)
            near -> SageBadge("$pct % usado", tone = BadgeTone.Warning, icon = LucideIcons.AlertTriangle)
            hasLimit -> Text("Te quedan ${fmt.format(row.limitMinor - row.spentMinor, currency)}", style = LocalSageType.current.caption, color = muted)
        }
    }
}

/** [BudgetProgressItem] dentro de su propia tarjeta (lo usa el Resumen). Con [onClick] la tarjeta edita. */
@Composable
fun BudgetProgressCard(row: BudgetRow, currency: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    FinanceCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        BudgetProgressItem(row, currency)
    }
}
