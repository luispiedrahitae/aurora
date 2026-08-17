package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import com.finanzen.data.AccountRepository
import com.finanzen.data.BudgetSummaryRow
import com.finanzen.data.CategoryRepository
import com.finanzen.data.InvestmentRepository
import com.finanzen.data.ReportBuilder
import com.finanzen.db.Budget
import com.finanzen.db.Category
import com.finanzen.db.FinanzenDb
import com.finanzen.db.TransactionRow
import com.finanzen.platform.ReportExporter
import com.finanzen.ui.format.PeriodMode
import com.finanzen.ui.format.formatMesAnio
import com.finanzen.ui.format.monthPeriod
import com.finanzen.ui.format.periodOfEpochDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

data class ExportStatus(val message: String, val isError: Boolean)

/**
 * Exportación de reportes (CSV/PDF), filtrada por mes o año. La agregación de presupuestos/
 * patrimonio/inversiones reutiliza [BudgetsViewModel.computeBudgets] y
 * [DashboardViewModel.computeMonthlyNetWorth] tal cual — nada se recalcula aquí.
 */
class ReportsViewModel(
    private val db: FinanzenDb,
    private val accountRepo: AccountRepository,
    private val categoryRepo: CategoryRepository,
    private val investRepo: InvestmentRepository,
    private val exporter: ReportExporter,
) : ViewModel() {

    private val _mode = MutableStateFlow(PeriodMode.MONTH)
    val mode: StateFlow<PeriodMode> = _mode.asStateFlow()

    private val _month = MutableStateFlow(
        run {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            LocalDate(today.year, today.month, 1)
        },
    )
    val month: StateFlow<LocalDate> = _month.asStateFlow()

    private val _year = MutableStateFlow(Clock.System.todayIn(TimeZone.currentSystemDefault()).year)
    val year: StateFlow<Int> = _year.asStateFlow()

    private val _status = MutableStateFlow<ExportStatus?>(null)
    val status: StateFlow<ExportStatus?> = _status.asStateFlow()

    fun setMode(mode: PeriodMode) {
        _mode.value = mode
    }

    fun setMonth(month: LocalDate) {
        _month.value = month
    }

    fun setYear(year: Int) {
        _year.value = year
    }

    /** Etiqueta legible del periodo activo (encabezado del PDF, subtítulos de las tarjetas). */
    fun periodLabel(): String = when (_mode.value) {
        PeriodMode.MONTH -> formatMesAnio(_month.value)
        PeriodMode.YEAR -> _year.value.toString()
    }

    fun exportTransactionsCsv() {
        val allTxs = db.transactionQueries.selectAll().executeAsList()
        val rows = filterByPeriod(allTxs, _mode.value, _month.value, _year.value)
        val cats = db.categoryQueries.selectAll().executeAsList()
        val accs = accountRepo.allIncludingArchived().associate { it.id to it.name }
        val csv = ReportBuilder.transactionsCsv(rows, cats, accs)
        val result = exporter.saveCsv("finanzen-movimientos-${periodSuffix()}", csv)
        _status.value = ExportStatus(result, isError = result.startsWith("error") || result.startsWith("stub"))
    }

    fun exportPeriodSummaryPdf() {
        val allTxs = db.transactionQueries.selectAll().executeAsList()
        val rows = filterByPeriod(allTxs, _mode.value, _month.value, _year.value)
        val categories = db.categoryQueries.selectAll().executeAsList()
        val currency = rows.firstOrNull()?.currency ?: allTxs.firstOrNull()?.currency ?: "USD"

        val budgets = db.budgetQueries.selectAll().executeAsList()
        val budgetRows = aggregateBudgets(budgets, categories, allTxs, _mode.value, _month.value, _year.value)

        val netWorthAccounts = accountRepo.all().filter { it.currency == currency }
        val netWorthAccountIds = netWorthAccounts.map { it.id }.toSet()
        val netWorthTxs = allTxs.filter { it.accountId in netWorthAccountIds }
        val netWorthByMonth = DashboardViewModel.computeMonthlyNetWorth(netWorthAccounts, netWorthTxs, periodYear())
        val netWorthMinor = when (_mode.value) {
            PeriodMode.MONTH -> netWorthByMonth.getOrNull(_month.value.monthNumber - 1)?.netWorthMinor ?: 0L
            PeriodMode.YEAR -> netWorthByMonth.lastOrNull()?.netWorthMinor ?: 0L
        }

        val lines = ReportBuilder.periodSummaryPdfLines(
            periodLabel = periodLabel(),
            rows = rows,
            categories = categories,
            currency = currency,
            budgetRows = budgetRows,
            netWorthMinor = netWorthMinor,
            openInvestments = investRepo.openNow(),
        )
        val result = exporter.savePdf("finanzen-resumen-${periodSuffix()}", lines)
        _status.value = ExportStatus(result, isError = result.startsWith("error") || result.startsWith("stub"))
    }

    fun clearStatus() {
        _status.value = null
    }

    private fun periodYear(): Int = when (_mode.value) {
        PeriodMode.MONTH -> _month.value.year
        PeriodMode.YEAR -> _year.value
    }

    private fun periodSuffix(): String = when (_mode.value) {
        PeriodMode.MONTH -> monthPeriod(_month.value).toString()
        PeriodMode.YEAR -> _year.value.toString()
    }

    companion object {
        /** Filtra transacciones al mes o año activo. */
        internal fun filterByPeriod(rows: List<TransactionRow>, mode: PeriodMode, month: LocalDate, year: Int): List<TransactionRow> = when (mode) {
            PeriodMode.MONTH -> rows.filter { periodOfEpochDay(it.date) == monthPeriod(month) }
            PeriodMode.YEAR -> rows.filter { periodOfEpochDay(it.date) / 100 == year.toLong() }
        }

        /** Une los presupuestos mes a mes (uno solo en modo Mensual, los 12 sumados en modo Anual)
         * por categoría — [BudgetsViewModel.computeBudgets] no cambia, solo se llama varias veces. */
        internal fun aggregateBudgets(
            budgets: List<Budget>,
            cats: List<Category>,
            allTxs: List<TransactionRow>,
            mode: PeriodMode,
            month: LocalDate,
            year: Int,
        ): List<BudgetSummaryRow> {
            val periods = when (mode) {
                PeriodMode.MONTH -> listOf(monthPeriod(month))
                PeriodMode.YEAR -> (1..12).map { year * 100L + it }
            }
            val totals = LinkedHashMap<Long, BudgetSummaryRow>()
            periods.forEach { period ->
                BudgetsViewModel.computeBudgets(budgets, cats, allTxs, period).rows.forEach { row ->
                    val existing = totals[row.categoryId]
                    totals[row.categoryId] = BudgetSummaryRow(
                        categoryName = row.categoryName,
                        parentName = row.parentName,
                        limitMinor = (existing?.limitMinor ?: 0L) + row.limitMinor,
                        spentMinor = (existing?.spentMinor ?: 0L) + row.spentMinor,
                    )
                }
            }
            return totals.values.sortedByDescending { it.spentMinor }
        }
    }
}
