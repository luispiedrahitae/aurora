package com.finanzen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.viewmodel.BudgetRow

/**
 * Fila de presupuesto: avatar + nombre + % + barra de progreso + "Gastado X de Y". Compartida por
 * la pestaña Presupuesto y la sección de presupuestos del Resumen. El sobregiro usa Warning Amber
 * con icono de triángulo y el % en color de advertencia — nunca color solo ni rojo de alarma.
 */
@Composable
fun BudgetProgressCard(row: BudgetRow, currency: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val fraction = (row.spentMinor.toFloat() / row.limitMinor.toFloat()).coerceIn(0f, 1f)
    val pct = (row.spentMinor * 100 / row.limitMinor).toInt()
    val over = row.spentMinor > row.limitMinor
    val finance = LocalFinanceColors.current
    val spacing = LocalSpacing.current
    val fmt = LocalMoneyFormat.current
    val barColor = if (over) finance.warning else MaterialTheme.colorScheme.primary

    FinanceCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                CategoryAvatar(icon = row.icon, size = 36.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(row.categoryName, fontWeight = FontWeight.SemiBold)
                    row.parentName?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (over) {
                    Icon(
                        Icons.Outlined.WarningAmber,
                        contentDescription = "Presupuesto superado",
                        tint = finance.warning,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    "$pct%",
                    fontWeight = FontWeight.SemiBold,
                    color = if (over) finance.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth(),
                color = barColor,
                drawStopIndicator = {},
            )
            Text(
                "Gastado ${fmt.format(row.spentMinor, currency)} de ${fmt.format(row.limitMinor, currency)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (over) finance.warning else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
