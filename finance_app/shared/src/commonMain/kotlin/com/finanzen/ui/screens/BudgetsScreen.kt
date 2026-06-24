package com.finanzen.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finanzen.domain.Money
import com.finanzen.viewmodel.BudgetRow
import com.finanzen.viewmodel.BudgetsViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    onBack: () -> Unit,
    vm: BudgetsViewModel = koinViewModel(),
) {
    val data by vm.data.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Presupuestos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { inner ->
        if (data.rows.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text(
                    "Crea categorías de gasto para asignarles presupuesto.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(data.rows, key = { it.categoryId }) { row ->
                    BudgetRowCard(row, onSetLimit = vm::setLimit)
                }
            }
        }
    }
}

@Composable
private fun BudgetRowCard(row: BudgetRow, onSetLimit: (Long, Long) -> Unit) {
    var limitText by remember(row.categoryId) {
        mutableStateOf(if (row.limitMinor > 0) Money(row.limitMinor, "").format() else "")
    }
    val hasLimit = row.limitMinor > 0
    val fraction = if (hasLimit) (row.spentMinor.toFloat() / row.limitMinor.toFloat()).coerceIn(0f, 1f) else 0f
    val over = hasLimit && row.spentMinor > row.limitMinor
    val barColor = if (over) Color(0xFFB13E53) else MaterialTheme.colorScheme.primary

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(row.categoryName, fontWeight = FontWeight.SemiBold)
            Text(
                if (hasLimit) {
                    "Gastado ${Money(row.spentMinor, "").format()} de ${Money(row.limitMinor, "").format()}"
                } else {
                    "Gastado ${Money(row.spentMinor, "").format()} · sin presupuesto"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (over) Color(0xFFB13E53) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (hasLimit) {
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth(), color = barColor)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Límite del mes") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = { Money.parseToMinor(limitText)?.let { onSetLimit(row.categoryId, it) } }) {
                    Text("Guardar")
                }
            }
        }
    }
}
