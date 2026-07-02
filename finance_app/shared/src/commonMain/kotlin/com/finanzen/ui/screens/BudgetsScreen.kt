package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finanzen.domain.Money
import com.finanzen.ui.components.CategoryAvatar
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.LabeledDropdown
import com.finanzen.ui.components.MainTabHeader
import com.finanzen.ui.components.categoryColor
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.viewmodel.BudgetRow
import com.finanzen.viewmodel.BudgetsViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Presupuestos del mes. Pestaña de nivel superior. Lista las categorías con presupuesto
 * (avatar + barra + % + monto) y un (+) para asignar/editar uno.
 */
// ponytail: onBack se conserva por compatibilidad de firma; hoy solo se usa como pestaña (sin back).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    onBack: (() -> Unit)? = null,
    vm: BudgetsViewModel = koinViewModel(),
) {
    val data by vm.data.collectAsState()
    val spacing = LocalSpacing.current
    val budgeted = data.rows.filter { it.limitMinor > 0 }
    var editing by remember { mutableStateOf<BudgetRow?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    if (showAdd || editing != null) {
        BudgetFormDialog(
            categories = data.rows,
            preselected = editing,
            onDismiss = {
                showAdd = false
                editing = null
            },
            onConfirm = { categoryId, limitMinor ->
                vm.setLimit(categoryId, limitMinor)
                showAdd = false
                editing = null
            },
        )
    }

    Scaffold(
        // El tope lo aporta MainTabHeader; el Scaffold se queda solo por el FAB.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (data.rows.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showAdd = true },
                    icon = { Icon(Icons.Outlined.Add, null) },
                    text = { Text("Presupuesto") },
                )
            }
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            MainTabHeader(title = "Presupuesto")
            when {
                data.rows.isEmpty() ->
                    EmptyMessage("Sin presupuestos", "Crea categorías de gasto para asignarles presupuesto.")
                budgeted.isEmpty() ->
                    EmptyMessage("Sin presupuestos", "Usa el botón + para asignar uno a una categoría.")
                else ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(spacing.md),
                    ) {
                        items(budgeted, key = { it.categoryId }) { row ->
                            BudgetRowCard(row, data.currency, onClick = { editing = row })
                        }
                    }
            }
        }
    }
}

@Composable
private fun EmptyMessage(title: String, subtitle: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            icon = Icons.Outlined.PieChart,
            title = title,
            subtitle = subtitle,
            modifier = Modifier.padding(32.dp),
        )
    }
}

@Composable
private fun BudgetRowCard(row: BudgetRow, currency: String, onClick: () -> Unit) {
    val fraction = (row.spentMinor.toFloat() / row.limitMinor.toFloat()).coerceIn(0f, 1f)
    val pct = (row.spentMinor * 100 / row.limitMinor).toInt()
    val over = row.spentMinor > row.limitMinor
    val finance = LocalFinanceColors.current
    val spacing = LocalSpacing.current
    val barColor = if (over) finance.expense else MaterialTheme.colorScheme.primary
    val fmt = LocalMoneyFormat.current

    FinanceCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                CategoryAvatar(icon = row.icon, color = categoryColor(row.categoryName, row.color), size = 36.dp)
                Text(row.categoryName, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(
                    if (over) "$pct% · excedido" else "$pct%",
                    fontWeight = FontWeight.SemiBold,
                    color = if (over) finance.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth(), color = barColor)
            Text(
                "Gastado ${fmt.format(row.spentMinor, currency)} de ${fmt.format(row.limitMinor, currency)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (over) finance.expense else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Asigna/edita el presupuesto de una categoría de gasto. Límite 0 = quitar el presupuesto. */
@Composable
private fun BudgetFormDialog(
    categories: List<BudgetRow>,
    preselected: BudgetRow?,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: Long, limitMinor: Long) -> Unit,
) {
    var selected by remember { mutableStateOf(preselected ?: categories.first()) }
    var limitText by remember {
        mutableStateOf(if (preselected != null && preselected.limitMinor > 0) Money(preselected.limitMinor, "").format() else "")
    }
    val minor = Money.parseToMinor(limitText)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (preselected != null) "Editar presupuesto" else "Nuevo presupuesto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledDropdown(
                    label = "Categoría",
                    options = categories,
                    selected = selected,
                    optionLabel = { it.categoryName },
                    onSelect = { selected = it },
                )
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Límite del mes") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected.categoryId, minor ?: 0L) },
                enabled = minor != null,
            ) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (preselected != null) {
                    TextButton(onClick = { onConfirm(preselected.categoryId, 0L) }) {
                        Text("Quitar presupuesto", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )
}
