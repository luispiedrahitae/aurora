package com.finanzen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.finanzen.domain.SpendingAnalytics
import com.finanzen.domain.daysInMonth
import com.finanzen.ui.components.AddFab
import com.finanzen.ui.components.BentoTileSize
import com.finanzen.ui.components.BudgetProgressItem
import com.finanzen.ui.components.CategoryAvatar
import com.finanzen.ui.components.ConfirmDialog
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.FabClearance
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.InlineNotice
import com.finanzen.ui.components.LoadingState
import com.finanzen.ui.components.MainTabHeader
import com.finanzen.ui.components.MoneyField
import com.finanzen.ui.components.MoneyText
import com.finanzen.ui.components.MonthSelector
import com.finanzen.ui.components.Overline
import com.finanzen.ui.components.PickerField
import com.finanzen.ui.components.ProgressBar
import com.finanzen.ui.components.ProgressTone
import com.finanzen.ui.components.SageButton
import com.finanzen.ui.components.SageButtonKind
import com.finanzen.ui.components.ScreenList
import com.finanzen.ui.components.SheetHeader
import com.finanzen.ui.components.TitledSection
import com.finanzen.ui.components.progressColor
import com.finanzen.ui.format.formatMesAnio
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalDateLocale
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSageType
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.viewmodel.BudgetRow
import com.finanzen.viewmodel.BudgetsViewModel
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

/**
 * Presupuestos del mes. Pestaña de nivel superior. Lista las categorías con presupuesto
 * (avatar + barra + % + monto) y un (+) para asignar/editar uno.
 */
// ponytail: onBack se conserva por compatibilidad de firma; hoy solo se usa como pestaña (sin back).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    onBack: (() -> Unit)? = null,
    onOpenCategories: () -> Unit = {},
    vm: BudgetsViewModel = koinViewModel(),
) {
    val data by vm.data.collectAsState()
    val month by vm.month.collectAsState()
    val dateLocale = LocalDateLocale.current
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

    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            MainTabHeader(
                title = "Presupuesto",
                action = {
                    IconButton(onClick = { showInfo = !showInfo }) {
                        Icon(
                            LucideIcons.Info,
                            contentDescription = "Cómo funcionan los presupuestos mensuales",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
            ScreenList(bottomPadding = FabClearance) {
                item(key = "month") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (showInfo) {
                            InlineNotice("Cada categoría mantiene el mismo límite mes a mes. Editarlo solo afecta desde ese mes en adelante.")
                        }
                        MonthSelector(
                            label = formatMesAnio(month, dateLocale),
                            onPrev = { vm.setMonth(month.plus(DatePeriod(months = -1))) },
                            onNext = { vm.setMonth(month.plus(DatePeriod(months = 1))) },
                            onLabelClick = { vm.setMonth(LocalDate(today.year, today.month, 1)) },
                        )
                    }
                }
                when {
                    !data.loaded -> item(key = "loading") { LoadingState() }

                    data.rows.isEmpty() -> item(key = "empty") {
                        // Sin categorías de gasto no hay FAB: el botón lleva al único sitio donde se resuelve.
                        EmptyState(
                            icon = LucideIcons.PieChart,
                            title = "Sin presupuestos",
                            subtitle = "Crea categorías de gasto para asignarles presupuesto.",
                            actionLabel = "Ir a Categorías",
                            onAction = onOpenCategories,
                        )
                    }

                    budgeted.isEmpty() -> item(key = "empty") {
                        EmptyState(
                            icon = LucideIcons.PieChart,
                            title = "Sin presupuestos",
                            subtitle = "Asigna un límite mensual a una categoría para ver cuánto te queda.",
                            actionLabel = "Asignar presupuesto",
                            onAction = { showAdd = true },
                        )
                    }

                    else -> {
                        item(key = "summary") { BudgetSummary(budgeted, data.currency, month, today) }
                        item(key = "rows") {
                            TitledSection("Por categoría") {
                                FinanceCard(Modifier.fillMaxWidth()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                        // Primero lo que necesita atención: excedidos y más cerca del límite.
                                        budgeted.sortedByDescending { it.spentMinor.toDouble() / it.limitMinor }.forEach { row ->
                                            BudgetProgressItem(
                                                row,
                                                data.currency,
                                                Modifier
                                                    .clip(MaterialTheme.shapes.small)
                                                    .clickable(onClickLabel = "Editar presupuesto", role = Role.Button) { editing = row },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (unbudgeted.isNotEmpty()) {
                            item(key = "unbudgeted") {
                                FinanceCard(Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                if (unbudgeted.size == 1) "1 categoría sin presupuesto" else "${unbudgeted.size} categorías sin presupuesto",
                                                style = LocalSageType.current.titleCard,
                                            )
                                            Text(
                                                unbudgeted.take(UNBUDGETED_PREVIEW).joinToString(", ") { it.categoryName } + if (unbudgeted.size > UNBUDGETED_PREVIEW) "…" else "",
                                                style = LocalSageType.current.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                            )
                                        }
                                        SageButton("Asignar", { showAdd = true }, kind = SageButtonKind.Secondary, small = true)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (unbudgeted.isNotEmpty()) {
            AddFab(
                label = "Nuevo presupuesto",
                onClick = { showAdd = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
    }
}

/**
 * Resumen del mes: gastado frente a lo presupuestado, lo que queda y, en el mes en curso, cuánto se
 * puede gastar por día para no pasarse.
 */
@Composable
private fun BudgetSummary(budgeted: List<BudgetRow>, currency: String, month: LocalDate, today: LocalDate) {
    val spent = budgeted.sumOf { it.spentMinor }
    val limit = budgeted.sumOf { it.limitMinor }
    val left = limit - spent
    // Cupo diario: lo que queda repartido entre los días que faltan (solo en el mes en curso).
    val allowance = SpendingAnalytics.dailyAllowance(limit, spent, today, month)
    val daysLeft = month.daysInMonth() - today.day + 1
    val fmt = LocalMoneyFormat.current
    FinanceCard(Modifier.fillMaxWidth(), size = BentoTileSize.Hero) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Overline("Gastado este mes")
            MoneyText(spent, currency, style = LocalSageType.current.amountHero)
            Text("de ${fmt.format(limit, currency)} presupuestados", style = LocalSageType.current.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val tone = when {
                spent > limit -> ProgressTone.Over
                limit > 0 && spent * 100 >= limit * BUDGET_WARNING_PCT -> ProgressTone.Warning
                else -> ProgressTone.Normal
            }
            ProgressBar(if (limit > 0) spent.toFloat() / limit else 0f, Modifier.padding(top = 4.dp), color = progressColor(tone))
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(if (left >= 0) "Te quedan" else "Te pasaste por", style = LocalSageType.current.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MoneyText(
                        if (left >= 0) left else -left,
                        currency,
                        style = LocalSageType.current.amountMd,
                        colorOverride = if (left < 0) LocalFinanceColors.current.expense else null,
                    )
                }
                if (allowance != null) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            if (daysLeft == 1) "Hoy, último día" else "Por día, $daysLeft días",
                            style = LocalSageType.current.caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        MoneyText(allowance, currency, style = LocalSageType.current.amountMd)
                    }
                }
            }
        }
    }
}

private const val BUDGET_WARNING_PCT = 85

/** Cuántas categorías sin presupuesto se nombran antes de cortar con "…". */
private const val UNBUDGETED_PREVIEW = 3

/**
 * Asigna/edita el presupuesto de una categoría de gasto. Límite 0 = quitar el presupuesto.
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
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
                // Al editar, la categoría queda fija: cambiarla escribía el límite sobre otra categoría.
                enabled = preselected == null,
            )
            MoneyField(
                amountMinor = limitMinor,
                onAmountChange = { limitMinor = it },
                currencyCode = currency,
                label = "Límite del mes",
                modifier = Modifier.fillMaxWidth(),
            )
            SageButton(
                "Guardar presupuesto",
                onClick = { onConfirm(selected.categoryId, limitMinor) },
                enabled = limitMinor > 0,
                modifier = Modifier.fillMaxWidth(),
            )
            if (preselected != null) {
                SageButton("Quitar presupuesto", { confirmRemove = true }, Modifier.fillMaxWidth(), kind = SageButtonKind.Danger)
            }
        }
    }
}
