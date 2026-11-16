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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finanzen.viewmodel.FrequentExpense

/**
 * Una tarjeta con N filas de gastos más frecuentes. Cada fila: subcategoría (nombre principal) +
 * su categoría padre como caption debajo (a qué categoría pertenece) + "×N" con el número de veces,
 * en el formato de los montos (bodyMedium SemiBold) pero en color de texto normal. Al ir al final de
 * una fila de ancho completo, los "×N" alinean su borde derecho entre filas sin medir nada.
 * [DashboardViewModel.computeFrequentExpenses] ya descarta cualquier movimiento sin subcategoría, así
 * que [FrequentExpense.subcategoryName] siempre viene poblado aquí.
 */
@Composable
fun TopFrequentExpensesList(items: List<FrequentExpense>, modifier: Modifier = Modifier) {
    if (items.isEmpty()) return

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
                            fontWeight = FontWeight.SemiBold,
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
                    Text(
                        if (item.count == 1) "1 vez" else "${item.count} veces",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
