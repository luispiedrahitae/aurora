package com.finanzen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.PillShape
import com.finanzen.viewmodel.FrequentExpense

/**
 * Una tarjeta con N filas de gastos más frecuentes. Cada fila: subcategoría (nombre principal) +
 * su categoría padre como caption debajo (a qué categoría pertenece) + chip "×N" con el número de
 * veces + el monto acumulado. Chip y monto se miden una vez con [rememberTextMeasurer] (mismo
 * patrón que [AutoSizeText]) para fijar el ancho de sus columnas — si no, "×13" y "×1" no alinean
 * su borde izquierdo entre filas, ni los montos su borde derecho. La frecuencia dice qué compras
 * seguido; el monto acumulado dice cuánto suma — sin él, "gasto hormiga" no habilita ninguna
 * decisión. [DashboardViewModel.computeFrequentExpenses] ya descarta cualquier movimiento sin
 * subcategoría, así que [FrequentExpense.subcategoryName] siempre viene poblado aquí.
 */
@Composable
fun TopFrequentExpensesList(items: List<FrequentExpense>, currency: String, modifier: Modifier = Modifier) {
    if (items.isEmpty()) return

    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val chipStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
    val amountStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
    val fmt = LocalMoneyFormat.current

    val chipColumnWidth = remember(items, chipStyle) {
        val maxTextWidth = items.maxOf { measurer.measure(AnnotatedString("×${it.count}"), style = chipStyle).size.width }
        with(density) { maxTextWidth.toDp() + 16.dp }
    }
    val amountColumnWidth = remember(items, currency, amountStyle) {
        val maxTextWidth = items.maxOf { measurer.measure(AnnotatedString(fmt.format(it.amountMinor, currency)), style = amountStyle).size.width }
        with(density) { maxTextWidth.toDp() }
    }

    FinanceCard(modifier = modifier.semantics { contentDescription = "Gastos más frecuentes" }) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            item.subcategoryName ?: item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (item.subcategoryName != null) {
                            Text(
                                item.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.width(chipColumnWidth),
                        shape = PillShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            "×${item.count}",
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                            style = chipStyle,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    MoneyText(
                        amountMinor = item.amountMinor,
                        currency = currency,
                        modifier = Modifier.width(amountColumnWidth),
                        style = amountStyle,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}
