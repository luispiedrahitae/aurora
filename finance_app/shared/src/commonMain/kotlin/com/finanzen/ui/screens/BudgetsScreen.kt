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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.BudgetProgressCard
import com.finanzen.ui.components.CategoryAvatar
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.MainTabHeader
import com.finanzen.ui.components.MoneyField
import com.finanzen.ui.components.MonthSelector
import com.finanzen.ui.components.PickerField
import com.finanzen.ui.components.SectionHeader
import com.finanzen.ui.components.flatFabElevation
import com.finanzen.ui.format.formatMesAnio
import com.finanzen.ui.theme.LocalDateLocale
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.viewmodel.BudgetRow
import com.finanzen.viewmodel.BudgetsViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
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
    val month by vm.month.collectAsState()
    val dateLocale = LocalDateLocale.current
    val spacing = LocalSpacing.current
    val budgeted = data.rows.filter { it.limitMinor > 0 }
    val unbudgeted = data.rows.filter { it.limitMinor <= 0 }
    val categoryBudgets = budgeted.filter { it.parentName == null }
    val subcategoryBudgets = budgeted.filter { it.parentName != null }
    var editing by remember { mutableStateOf<BudgetRow?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }

    if (showAdd || editing != null) {
        BudgetFormDialog(
            // Al crear, solo categorías sin presupuesto todavía — para cambiar una ya asignada hay que
            // editarla (tocar su fila), no re-crearla desde acá.
            categories = if (editing != null) data.rows else unbudgeted,
            preselected = editing,
            currency = data.currency,
            monthLabel = formatMesAnio(month, dateLocale),
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

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("¿Cómo funcionan los presupuestos mensuales?") },
            text = {
                Text(
                    "Cada categoría mantiene el mismo límite mes a mes. Si no cambias el presupuesto de " +
                        "un mes, se usa el del mes anterior más reciente. Si lo editas, el cambio solo " +
                        "aplica desde ese mes en adelante — los meses ya guardados no se alteran.",
                )
            },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Entendido") } },
        )
    }

    Scaffold(
        // El tope lo aporta MainTabHeader; el Scaffold se queda solo por el FAB.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (unbudgeted.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showAdd = true },
                    icon = { Icon(Icons.Outlined.Add, null) },
                    text = { Text("Presupuesto") },
                    elevation = flatFabElevation(),
                )
            }
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            MainTabHeader(
                title = "Presupuesto",
                action = {
                    IconButton(onClick = { showInfo = true }) {
                        Icon(
                            Icons.Outlined.HelpOutline,
                            contentDescription = "Cómo funcionan los presupuestos mensuales",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
            MonthSelector(
                label = formatMesAnio(month, dateLocale),
                onPrev = { vm.setMonth(month.plus(DatePeriod(months = -1))) },
                onNext = { vm.setMonth(month.plus(DatePeriod(months = 1))) },
                modifier = Modifier.padding(horizontal = spacing.lg),
                onLabelClick = {
                    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                    vm.setMonth(LocalDate(today.year, today.month, 1))
                },
            )
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
                        if (categoryBudgets.isNotEmpty()) {
                            item { SectionHeader("Categorías") }
                            items(categoryBudgets, key = { it.categoryId }) { row ->
                                BudgetProgressCard(row, data.currency, onClick = { editing = row })
                            }
                        }
                        if (subcategoryBudgets.isNotEmpty()) {
                            item { SectionHeader("Subcategorías") }
                            items(subcategoryBudgets, key = { it.categoryId }) { row ->
                                BudgetProgressCard(row, data.currency, onClick = { editing = row })
                            }
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

/** Asigna/edita el presupuesto de una categoría de gasto. Límite 0 = quitar el presupuesto. */
@Composable
private fun BudgetFormDialog(
    categories: List<BudgetRow>,
    preselected: BudgetRow?,
    currency: String,
    monthLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: Long, limitMinor: Long) -> Unit,
) {
    var selected by remember { mutableStateOf(preselected ?: categories.first()) }
    var limitMinor by remember { mutableStateOf(preselected?.limitMinor ?: 0L) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(if (preselected != null) "Editar presupuesto" else "Nuevo presupuesto")
                Text(
                    monthLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PickerField(
                    label = "Categoría",
                    options = categories,
                    selected = selected,
                    optionLabel = { it.categoryName },
                    onSelect = { selected = it },
                    leadingContent = { row -> CategoryAvatar(row.icon, size = 32.dp) },
                    trailingLabel = { row -> if (row.parentName == null) "Categoría completa" else null },
                    sectionOf = { row -> row.parentName ?: "Categorías" },
                )
                MoneyField(
                    amountMinor = limitMinor,
                    onAmountChange = { limitMinor = it },
                    currencyCode = currency,
                    label = "Límite del mes",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected.categoryId, limitMinor) },
                enabled = limitMinor > 0,
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
