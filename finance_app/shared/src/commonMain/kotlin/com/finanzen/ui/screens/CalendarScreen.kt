package com.finanzen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finanzen.db.Category
import com.finanzen.db.TransactionRow
import com.finanzen.domain.daysInMonth
import com.finanzen.ui.components.AvatarTone
import com.finanzen.ui.components.CategoryAvatar
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.GroupCard
import com.finanzen.ui.components.KindAvatar
import com.finanzen.ui.components.ListRow
import com.finanzen.ui.components.MoneyText
import com.finanzen.ui.components.RowDivider
import com.finanzen.ui.components.ScreenList
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.components.TitledSection
import com.finanzen.ui.components.kindLabel
import com.finanzen.ui.components.signedAmountAndColor
import com.finanzen.ui.format.formatFechaLarga
import com.finanzen.ui.format.formatMesAnio
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalDateLocale
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSageType
import com.finanzen.viewmodel.TransactionsViewModel
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

private val WEEKDAYS = listOf("Lu", "Ma", "Mi", "Ju", "Vi", "Sá", "Do")

private fun LocalDate.epochDay(): Long = toEpochDays()

private fun daysInMonth(firstOfMonth: LocalDate): Int = firstOfMonth.daysInMonth()

@Composable
fun CalendarScreen(
    onEdit: (Long) -> Unit,
    onBack: () -> Unit,
    vm: TransactionsViewModel = koinViewModel(),
) {
    val rows by vm.transactions.collectAsState()
    val categories by vm.categories.collectAsState()
    val dateLocale = LocalDateLocale.current
    val categoriesById = remember(categories) { categories.associateBy { it.id } }
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    // Guardados como epochDay: sobreviven a rotación/proceso (LocalDate no es Saveable por sí solo).
    var monthDay by rememberSaveable { mutableStateOf(LocalDate(today.year, today.month, 1).toEpochDays()) }
    var selectedDay by rememberSaveable { mutableStateOf(today.toEpochDays()) }
    val month = LocalDate.fromEpochDays(monthDay)
    val selected = LocalDate.fromEpochDays(selectedDay)

    // Al cambiar de mes, la selección se muda con él (hoy si es el mes actual, si no el día 1): antes
    // la lista seguía mostrando un día de un mes que ya no estaba en la grilla.
    fun goToMonth(m: LocalDate) {
        monthDay = m.toEpochDays()
        selectedDay = (if (m.year == today.year && m.month == today.month) today else m).toEpochDays()
    }

    val byDay = remember(rows) { rows.groupBy { it.date } }
    // Neto por día (ingreso − gasto; las transferencias no cuentan) para el color del punto.
    val netByDay = remember(rows) {
        rows.groupBy { it.date }.mapValues { (_, list) ->
            list.sumOf {
                when (it.kind) {
                    "INCOME" -> it.amountMinor
                    "EXPENSE" -> -it.amountMinor
                    else -> 0L
                }
            }
        }
    }
    val selectedRows = byDay[selected.epochDay()].orEmpty()

    // Qué tipos de movimiento tuvo cada día: un punto salvia (ingreso) y/o uno terracota (gasto).
    val kindsByDay = remember(rows) { rows.groupBy { it.date }.mapValues { (_, list) -> list.map { it.kind }.toSet() } }

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader(title = "Calendario", onBack = onBack)
        ScreenList {
            item {
                FinanceCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { goToMonth(month.plus(DatePeriod(months = -1))) }) {
                                Icon(LucideIcons.ChevronLeft, contentDescription = "Mes anterior")
                            }
                            Text(
                                formatMesAnio(month, dateLocale),
                                style = LocalSageType.current.titleSection,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable(onClickLabel = "Volver al mes actual", role = Role.Button) {
                                        goToMonth(LocalDate(today.year, today.month, 1))
                                    }
                                    .semantics { heading() },
                            )
                            IconButton(onClick = { goToMonth(month.plus(DatePeriod(months = 1))) }) {
                                Icon(LucideIcons.ChevronRight, contentDescription = "Mes siguiente")
                            }
                        }
                        Row(Modifier.fillMaxWidth()) {
                            WEEKDAYS.forEach { d ->
                                Text(
                                    d.uppercase(),
                                    modifier = Modifier.weight(1f).clearAndSetSemantics { },
                                    textAlign = TextAlign.Center,
                                    style = LocalSageType.current.overline,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        MonthGrid(
                            month = month,
                            today = today,
                            selected = selected,
                            kindsByDay = kindsByDay,
                            onSelect = { selectedDay = it.toEpochDays() },
                        )
                        HorizontalDivider(Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            Modifier.padding(start = 8.dp, top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LegendDot(LocalFinanceColors.current.income, "Ingreso")
                            LegendDot(LocalFinanceColors.current.expense, "Gasto")
                        }
                    }
                }
            }

            item {
                TitledSection(formatFechaLarga(selected, dateLocale).replaceFirstChar { it.uppercase() }) {
                    netByDay[selected.epochDay()]?.let { net ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Neto del día", style = LocalSageType.current.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            MoneyText(net, currency = selectedRows.firstOrNull()?.currency ?: "", style = LocalSageType.current.amountSm, signed = true)
                        }
                    }
                    if (selectedRows.isEmpty()) {
                        EmptyState(
                            icon = LucideIcons.Receipt,
                            title = "Sin movimientos este día",
                            subtitle = "Toca otro día del calendario o registra uno con el botón + de Movimientos.",
                        )
                    } else {
                        GroupCard {
                            selectedRows.forEachIndexed { index, row ->
                                if (index > 0) RowDivider()
                                val subcategory = categoriesById[row.categoryId]
                                DayTxRow(
                                    row,
                                    category = subcategory,
                                    parentCategory = subcategory?.parentId?.let { categoriesById[it] },
                                    onClick = { onEdit(row.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = LocalSageType.current.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MonthGrid(
    month: LocalDate,
    today: LocalDate,
    selected: LocalDate,
    kindsByDay: Map<Long, Set<String>>,
    onSelect: (LocalDate) -> Unit,
) {
    val leadingBlanks = month.dayOfWeek.isoDayNumber - 1 // lunes = 0
    val total = daysInMonth(month)
    // Lista de celdas: nulls iniciales para alinear el primer día a su columna.
    val cells: List<Int?> = List(leadingBlanks) { null } + (1..total).toList()
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        cells.chunked(DAYS_PER_WEEK).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                // Rellena la última semana para que las celdas conserven el ancho.
                val padded = week + List(DAYS_PER_WEEK - week.size) { null }
                padded.forEach { day ->
                    if (day == null) {
                        Box(Modifier.weight(1f).height(48.dp))
                    } else {
                        val date = LocalDate(month.year, month.month, day)
                        DayCell(
                            date = date,
                            isSelected = date == selected,
                            isToday = date == today,
                            isFuture = date > today,
                            kinds = kindsByDay[date.epochDay()].orEmpty(),
                            onSelect = { onSelect(date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** Celda de un día: número y un punto por ingreso y otro por gasto. Anuncia qué tuvo el día. */
@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    kinds: Set<String>,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val finance = LocalFinanceColors.current
    val scheme = MaterialTheme.colorScheme
    val dateLocale = LocalDateLocale.current
    val hasIncome = "INCOME" in kinds
    val hasExpense = "EXPENSE" in kinds
    // Los puntos no son la única codificación: la celda anuncia qué tuvo el día.
    val dayState = when {
        hasIncome && hasExpense -> "con ingresos y gastos"
        hasIncome -> "con ingresos"
        hasExpense -> "con gastos"
        kinds.isNotEmpty() -> "con movimientos"
        else -> null
    }
    val decoration = when {
        isSelected -> Modifier.background(scheme.primary)
        isToday -> Modifier.border(1.dp, scheme.primary, MaterialTheme.shapes.small)
        else -> Modifier
    }
    val textColor = when {
        isSelected -> scheme.onPrimary
        isFuture -> scheme.onSurfaceVariant
        else -> scheme.onSurface
    }
    Column(
        modifier = modifier
            .height(48.dp)
            .clip(MaterialTheme.shapes.small)
            .then(decoration)
            .selectable(selected = isSelected, role = Role.Button, onClick = onSelect)
            .semantics(mergeDescendants = true) {
                contentDescription = listOfNotNull(formatFechaLarga(date, dateLocale), "hoy".takeIf { isToday }, dayState).joinToString(", ")
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        Text(
            date.day.toString(),
            style = LocalSageType.current.label.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
            color = textColor,
        )
        Row(Modifier.height(6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            if (hasIncome) Dot(if (isSelected) scheme.onPrimary else finance.income)
            if (hasExpense) Dot(if (isSelected) scheme.onPrimary else finance.expense)
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(6.dp).clip(CircleShape).background(color))
}

private const val DAYS_PER_WEEK = 7

@Composable
private fun DayTxRow(row: TransactionRow, category: Category?, parentCategory: Category?, onClick: () -> Unit) {
    val finance = LocalFinanceColors.current
    val fmt = LocalMoneyFormat.current
    val isTransfer = row.kind == "TRANSFER"
    val (signedAmount, amountColor) = signedAmountAndColor(row.kind, row.amountMinor, finance)
    val tone = when (row.kind) {
        "INCOME" -> AvatarTone.Income
        "EXPENSE" -> AvatarTone.Expense
        else -> AvatarTone.Accent
    }
    ListRow(
        // Título = subcategoría (lo que el usuario eligió), como en Movimientos; nunca "(sin nota)".
        title = if (isTransfer) "Transferencia" else category?.name ?: row.note.ifBlank { kindLabel(row.kind) },
        subtitle = parentCategory?.name ?: kindLabel(row.kind),
        leading = {
            if (category != null) CategoryAvatar(name = category.name, tone = tone) else KindAvatar(kind = row.kind)
        },
        onClick = onClick,
        trailing = {
            if (isTransfer) {
                Text(fmt.format(row.amountMinor, row.currency), style = LocalSageType.current.amountMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(fmt.format(signedAmount, row.currency, signed = true), style = LocalSageType.current.amountMd, color = amountColor)
            }
        },
    )
}
