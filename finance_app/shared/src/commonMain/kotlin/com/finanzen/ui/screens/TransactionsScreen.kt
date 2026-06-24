package com.finanzen.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.db.TransactionRow
import com.finanzen.domain.Money
import com.finanzen.viewmodel.TransactionsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TransactionsScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    vm: TransactionsViewModel = koinViewModel(),
) {
    val rows by vm.transactions.collectAsState()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text("Añadir") },
            )
        },
    ) { inner ->
        if (rows.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text(
                    "Sin transacciones todavía.\nUsa + para registrar una.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(rows, key = { it.id }) { row ->
                    TransactionItem(row, onClick = { onEdit(row.id) }, onDelete = { vm.delete(row.id) })
                }
            }
        }
    }
}

@Composable
private fun TransactionItem(row: TransactionRow, onClick: () -> Unit, onDelete: () -> Unit) {
    val isIncome = row.kind == "INCOME"
    val amountColor = if (isIncome) MaterialTheme.colorScheme.primary else Color(0xFFB13E53)
    val sign = if (isIncome) "+" else "-"
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(row.note.ifBlank { "(sin nota)" }, fontWeight = FontWeight.SemiBold)
                Text(
                    "$sign${Money(row.amountMinor, row.currency).format()} ${row.currency}",
                    style = MaterialTheme.typography.bodySmall,
                    color = amountColor,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Eliminar")
            }
        }
    }
}
