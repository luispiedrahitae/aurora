package com.finanzen.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.db.TransactionRow
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.MoneyText
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.viewmodel.TransactionsViewModel
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

private val MESES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

private fun dateLabel(epochDay: Long): String {
    val d = LocalDate.fromEpochDays(epochDay.toInt())
    return "${d.dayOfMonth} ${MESES[d.monthNumber - 1]} ${d.year}"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    onEdit: (Long) -> Unit,
    vm: TransactionsViewModel = koinViewModel(),
) {
    val rows by vm.transactions.collectAsState()
    val spacing = LocalSpacing.current
    var query by remember { mutableStateOf("") }

    val filtered = remember(rows, query) {
        if (query.isBlank()) {
            rows
        } else {
            rows.filter { it.note.contains(query, ignoreCase = true) }
        }
    }
    // Agrupado por día, preservando el orden (más reciente primero) que entrega el repo.
    val groups = remember(filtered) {
        filtered.groupBy { it.date }.entries.sortedByDescending { it.key }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = spacing.lg)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(vertical = spacing.md),
            placeholder = { Text("Buscar por nota") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
        )

        if (groups.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = if (query.isBlank()) "Sin transacciones" else "Sin resultados",
                    subtitle = if (query.isBlank()) "Usa el botón + para registrar tu primer movimiento." else "Prueba con otra búsqueda.",
                    modifier = Modifier.padding(spacing.xl),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp, top = spacing.xs),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                groups.forEach { (day, dayRows) ->
                    stickyHeader(key = day) {
                        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                dateLabel(day),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = spacing.sm),
                            )
                        }
                    }
                    items(dayRows, key = { it.id }) { row ->
                        TransactionItem(row, onClick = { onEdit(row.id) }, onDelete = { vm.delete(row.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionItem(row: TransactionRow, onClick: () -> Unit, onDelete: () -> Unit) {
    val isIncome = row.kind == "INCOME"
    val signedAmount = if (isIncome) row.amountMinor else -row.amountMinor
    val spacing = LocalSpacing.current
    FinanceCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(spacing.lg)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.note.ifBlank { "(sin nota)" },
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            MoneyText(
                amountMinor = signedAmount,
                currency = row.currency,
                style = MaterialTheme.typography.titleMedium,
                signed = true,
                showCurrency = false,
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
