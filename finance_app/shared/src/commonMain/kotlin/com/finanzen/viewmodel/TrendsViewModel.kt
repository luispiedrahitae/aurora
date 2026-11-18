package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.CategoryRepository
import com.finanzen.data.SettingsRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.db.Category
import com.finanzen.db.TransactionRow
import com.finanzen.domain.Movement
import com.finanzen.domain.MovementKind
import com.finanzen.domain.PeriodTotal
import com.finanzen.domain.RecurringSplit
import com.finanzen.domain.SpendingAnalytics
import com.finanzen.domain.daysInMonth
import com.finanzen.domain.firstOfMonth
import com.finanzen.ui.format.mesCorto
import com.finanzen.ui.format.mesLargo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** Cómo se agrupa el gráfico de Tendencias. */
enum class TrendGrouping(val label: String) {
    DAY("Día"),
    WEEK("Semana"),
    MONTH("Mes"),
}

/** Una barra del gráfico: [axisLabel] es la marca corta del eje (nula = sin rótulo); [label] la completa. */
data class TrendBar(val axisLabel: String?, val label: String, val expenseMinor: Long)

/** Una categoría principal (o "Otros") con su gasto y su parte del total. */
data class CategoryTrend(val name: String, val amountMinor: Long, val fraction: Float, val isOther: Boolean)

/**
 * Todo lo que muestra Tendencias para un mes. El promedio diario y las barras cuentan solo el gasto
 * puntual (sin suscripciones, aportes periódicos ni cuotas), que es el que uno controla día a día;
 * el reparto recurrente/puntual y las categorías sí cuentan todo el gasto.
 */
data class TrendsData(
    val currency: String,
    val month: LocalDate,
    val isCurrentMonth: Boolean,
    val grouping: TrendGrouping,
    val averageDailyMinor: Long,
    val previousAverageMinor: Long,
    val previousMonthName: String,
    val bars: List<TrendBar>,
    val barAverageMinor: Long,
    val split: RecurringSplit,
    val categories: List<CategoryTrend>,
)

class TrendsViewModel(
    txRepo: TransactionRepository,
    categoryRepo: CategoryRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val today: LocalDate get() = Clock.System.todayIn(TimeZone.currentSystemDefault())

    private val _grouping = MutableStateFlow(TrendGrouping.DAY)
    val grouping: StateFlow<TrendGrouping> = _grouping

    private val _month = MutableStateFlow(today.firstOfMonth())
    val month: StateFlow<LocalDate> = _month

    fun setGrouping(grouping: TrendGrouping) {
        _grouping.value = grouping
    }

    fun shiftMonth(months: Int) {
        _month.value = _month.value.plus(DatePeriod(months = months))
    }

    fun goToCurrentMonth() {
        _month.value = today.firstOfMonth()
    }

    val data: StateFlow<TrendsData?> =
        combine(txRepo.observeAll(), categoryRepo.observeAll(), _grouping, _month) { txs, cats, grouping, month ->
            computeTrends(txs, cats, settings.baseCurrency(), grouping, month, today)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Calcula [TrendsData] sin tocar la BD ni el reloj (para poder probarlo). Solo la moneda base. */
internal fun computeTrends(
    txs: List<TransactionRow>,
    categories: List<Category>,
    currency: String,
    grouping: TrendGrouping,
    month: LocalDate,
    today: LocalDate,
): TrendsData {
    val movements = txs.filter { it.currency == currency }.map { it.toMovement() }
    val oneOff = movements.filter { !it.recurring }
    val first = month.firstOfMonth()
    val isCurrent = today.firstOfMonth() == first
    val lastOfMonth = first.plus(DatePeriod(days = first.daysInMonth() - 1))
    val end = if (isCurrent) today else lastOfMonth
    val bars = when (grouping) {
        TrendGrouping.DAY -> dayBars(SpendingAnalytics.dailyTotals(oneOff, first).filter { it.start <= end }, isCurrent)

        TrendGrouping.WEEK -> SpendingAnalytics.weeklyTotals(oneOff, first).filter { it.start <= end }.map { weekBar(it) }

        TrendGrouping.MONTH -> SpendingAnalytics.monthlyTotals(oneOff, first, MONTHS_SHOWN).map {
            TrendBar(shortMonth(it.start.month.number).replaceFirstChar(Char::uppercase), "${mesLargo(it.start.month.number)} ${it.start.year}", it.expenseMinor)
        }
    }
    val parentOf = categories.associate { it.id to (it.parentId ?: it.id) }
    val names = categories.associate { it.id to it.name }
    return TrendsData(
        currency = currency,
        month = first,
        isCurrentMonth = isCurrent,
        grouping = grouping,
        averageDailyMinor = SpendingAnalytics.averageDailyExpense(oneOff, first, today),
        previousAverageMinor = SpendingAnalytics.previousMonthAverageSameDays(oneOff, first, today),
        previousMonthName = mesLargo(first.minus(DatePeriod(months = 1)).month.number),
        bars = bars,
        barAverageMinor = if (bars.isEmpty()) 0 else bars.sumOf { it.expenseMinor } / bars.size,
        split = SpendingAnalytics.recurringSplit(movements, first, end),
        categories = SpendingAnalytics.categoryBreakdown(movements, first, end, TOP_CATEGORIES) { id -> id?.let { parentOf[it] } }
            .map { share -> CategoryTrend(share.categoryId?.let { names[it] } ?: "Otros", share.amountMinor, share.fraction, share.categoryId == null) },
    )
}

private fun TransactionRow.toMovement() = Movement(
    date = LocalDate.fromEpochDays(date.toInt()),
    amountMinor = amountMinor,
    kind = MovementKind.fromDb(kind),
    categoryId = categoryId,
    recurring = subscriptionId != null || investmentId != null || installmentPlanId != null,
)

/** Rótulos del eje cada 5 días (y el día 1), sin pisar el último, que dice "Hoy" en el mes en curso. */
private fun dayBars(days: List<PeriodTotal>, isCurrent: Boolean): List<TrendBar> = days.mapIndexed { i, total ->
    val day = total.start.day
    val isLast = i == days.lastIndex
    val regular = (day == 1 || day % LABEL_EVERY_DAYS == 0) && days.size - 1 - i >= MIN_GAP_TO_LAST
    val axis = when {
        isLast -> if (isCurrent) "Hoy" else day.toString()
        regular -> day.toString()
        else -> null
    }
    TrendBar(axis, "$day ${shortMonth(total.start.month.number)}", total.expenseMinor)
}

private fun weekBar(total: PeriodTotal): TrendBar {
    val range = if (total.start == total.end) "${total.start.day}" else "${total.start.day}–${total.end.day}"
    return TrendBar(range, "$range ${shortMonth(total.end.month.number)}", total.expenseMinor)
}

private const val MONTHS_SHOWN = 6
private const val TOP_CATEGORIES = 4
private const val LABEL_EVERY_DAYS = 5
private const val MIN_GAP_TO_LAST = 3

/** Mes corto sin el punto de abreviatura ("nov"), como en los rótulos del diseño. */
private fun shortMonth(monthNumber: Int): String = mesCorto(monthNumber).removeSuffix(".")
