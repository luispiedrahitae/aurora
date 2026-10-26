package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.AddFab
import com.finanzen.ui.components.BudgetProgressCard
import com.finanzen.ui.components.CategoryAvatar
import com.finanzen.ui.components.ConfirmDialog
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.MainTabHeader
import com.finanzen.ui.components.MoneyField
import com.finanzen.ui.components.MonthSelector
import com.finanzen.ui.components.PickerField
import com.finanzen.ui.components.SheetHeader
import com.finanzen.ui.format.formatMesAnio
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalDateLocale
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.ui.theme.displayFontFamily
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
            if (unbudgeted.isNotEmpty()) {
                AddFab(label = "Presupuesto", onClick = { showAdd = true })
            }
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            MainTabHeader(
                title = "Presupuesto",
                action = {
                    // IconButton (área táctil de 48dp): MainTabHeader ya tiene heightIn(min = 48.dp),
                    // así que no infla el header frente a Movimientos/Cuentas.
                    IconButton(onClick = { showInfo = !showInfo }) {
                        Icon(
                            LucideIcons.Info,
                            contentDescription = "Cómo funcionan los presupuestos mensuales",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )
            if (showInfo) {
                FinanceCard(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.sm),
                ) {
                    Text(
                        "Cada categoría mantiene el mismo límite mes a mes. Editarlo solo afecta desde ese mes en adelante.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
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
                        items(budgeted, key = { it.categoryId }) { row ->
                            BudgetProgressCard(row, data.currency, onClick = { editing = row })
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
            icon = LucideIcons.PieChart,
            title = title,
            subtitle = subtitle,
            modifier = Modifier.padding(32.dp),
        )
    }
}

/**
 * Asigna/edita el presupuesto de una categoría de gasto. Límite 0 = quitar el presupuesto.
 * ModalBottomSheet plano, mismo patrón que `AccountFormDialog` (AccountsTabScreen.kt) y
 * `TransactionFormScreen`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetFormDialog(
    categories: List<BudgetRow>,
    preselected: BudgetRow?,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: Long, limitMinor: Long) -> Unit,
) {
    var selected by remember { mutableStateOf(preselected ?: categories.first()) }
    var limitMinor by remember { mutableStateOf(preselected?.limitMinor ?: 0L) }
    val finance = LocalFinanceColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmRemove by remember { mutableStateOf(false) }
    if (confirmRemove && preselected != null) {
        ConfirmDialog(
            title = "¿Quitar presupuesto de \"${preselected.categoryName}\"?",
            body = "La categoría dejará de tener límite mensual desde este mes.",
            confirmLabel = "Quitar",
            onConfirm = {
                confirmRemove = false
                onConfirm(preselected.categoryId, 0L)
            },
            onDismiss = { confirmRemove = false },
        )
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, dragHandle = null) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SheetHeader(
                title = if (preselected != null) "Editar presupuesto" else "Nuevo presupuesto",
                onDismiss = onDismiss,
            )
            PickerField(
                label = "Categoría",
                options = categories,
                selected = selected,
                optionLabel = { row -> row.categoryName },
                onSelect = { selected = it },
                leadingContent = { row -> CategoryAvatar(row.categoryName, size = 32.dp) },
                sectionOf = { row -> row.parentName ?: "Categorías" },
                plainLabel = true,
            )
            MoneyField(
                amountMinor = limitMinor,
                onAmountChange = { limitMinor = it },
                currencyCode = currency,
                label = "Límite del mes",
                plainLabel = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onConfirm(selected.categoryId, limitMinor) },
                enabled = limitMinor > 0,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Guardar presupuesto", fontFamily = displayFontFamily()) }
            if (preselected != null) {
                TextButton(
                    onClick = { confirmRemove = true },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = finance.expense),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Quitar presupuesto") }
            }
        }
    }
}
