package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.AccountRepository
import com.finanzen.data.BudgetRepository
import com.finanzen.data.CURRENCY_LOCALE_INFO
import com.finanzen.data.CategoryRepository
import com.finanzen.data.CurrencyLocaleInfo
import com.finanzen.data.DEFAULT_LOCALE_INFO
import com.finanzen.data.SettingsRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.db.Account
import com.finanzen.db.Budget
import com.finanzen.db.Category
import com.finanzen.db.TransactionRow
import com.finanzen.ui.format.mesCorto
import com.finanzen.ui.format.monthPeriod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

data class DashboardData(
    val totalBalanceMinor: Long,
    val yearIncomeMinor: Long,
    val yearExpenseMinor: Long,
    val currency: String,
    val netMinor: Long,
    val savingsRate: Float,
    val accounts: List<AccountBar>,
    val donut: List<CategorySlice>,
    val budgets: List<BudgetRow>,
    val netWorthByMonth: List<MonthNetWorth>,
    val incomeByMonth: List<MonthAmount>,
    val expenseByMonth: List<MonthAmount>,
    val subscriptionSpendMinor: Long,
)

/** Una cuenta con su saldo actual, para la fila de barras del inicio. */
data class AccountBar(val name: String, val type: String, val balanceMinor: Long)

/** Un mes del año seleccionado: etiqueta corta + monto. Alimenta ambos gráficos de barra
 * (ingresos/gastos por mes) y comparte el mismo eje que [MonthNetWorth]. */
data class MonthAmount(val label: String, val amountMinor: Long)

/** Patrimonio neto acumulado al cierre de cada mes del año seleccionado. */
data class MonthNetWorth(val label: String, val netWorthMinor: Long)

/**
 * Pantalla de inicio: revisión anual. Balance general (saldos iniciales + ingresos − gastos de todo
 * el histórico, sin acotar al año), ingreso/gasto/tasa de ahorro/patrimonio neto/gasto en
 * suscripciones del año seleccionado, saldos por cuenta (foto actual) y rosca de gasto por
 * categoría del año. Reusa `CategorySlice` de [AnalysisViewModel]. El cálculo vive en
 * [computeDashboard] (testeable); nada se calcula en la UI.
 */
class DashboardViewModel(
    txRepo: TransactionRepository,
    accountRepo: AccountRepository,
    categoryRepo: CategoryRepository,
    budgetRepo: BudgetRepository,
    settingsRepo: SettingsRepository,
) : ViewModel() {

    // Mes real actual, solo para el presupuesto (que sigue siendo mensual); no es reactivo porque
    // nada en la UI lo cambia — el selector de año (_year) es la única navegación temporal de esta
    // pantalla.
    private val currentFirstOfMonth: LocalDate = run {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        LocalDate(today.year, today.month, 1)
    }

    private val _year = MutableStateFlow(Clock.System.todayIn(TimeZone.currentSystemDefault()).year)
    val year: StateFlow<Int> = _year

    fun setYear(year: Int) {
        _year.value = year
    }

    val data: StateFlow<DashboardData> =
        combine(
            txRepo.observeAll(),
            accountRepo.observeAll(),
            categoryRepo.observeAll(),
            budgetRepo.observeAll(),
            _year,
        ) { txs, accounts, cats, budgets, year ->
            val baseCurrency = settingsRepo.baseCurrency()
            val locale = CURRENCY_LOCALE_INFO[baseCurrency] ?: DEFAULT_LOCALE_INFO
            computeDashboard(txs, accounts, cats, budgets, currentFirstOfMonth, year, baseCurrency, locale)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DashboardData(0, 0, 0, DEFAULT_CURRENCY, 0L, 0f, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), 0L),
        )

    companion object {
        private const val DEFAULT_CURRENCY = "USD"
        internal val NET_WORTH_TYPES = setOf("CASH", "DEBIT", "SAVINGS")

        private fun monthKey(year: Int, monthNumber: Int): Int = year * 100 + monthNumber
        private fun monthKeyOf(epochDay: Long): Int = LocalDate.fromEpochDays(epochDay.toInt()).let { monthKey(it.year, it.monthNumber) }

        /** (ingreso − gasto) ÷ ingreso, recortado a 0-100%; sin ingresos, 0% (no divide por cero). */
        internal fun computeSavingsRate(income: Long, expense: Long): Float = if (income == 0L) {
            0f
        } else {
            ((income - expense).toFloat() / income.toFloat()).coerceIn(0f, 1f)
        }

        /** Agregación pura. `firstOfMonth` alimenta solo el presupuesto (mensual); `year` alimenta
         * todo lo demás (inyectables para tests). */
        internal fun computeDashboard(
            txs: List<TransactionRow>,
            accounts: List<Account>,
            cats: List<Category>,
            budgets: List<Budget>,
            firstOfMonth: LocalDate,
            year: Int,
            baseCurrency: String,
            locale: CurrencyLocaleInfo = DEFAULT_LOCALE_INFO,
        ): DashboardData {
            // La app no maneja FX (ver SettingsRepository.setBaseCurrency): una cuenta o transacción
            // en una moneda distinta a la base no se convierte, se excluye de los totales para no
            // sumar monedas distintas como si fueran una sola.
            val currency = baseCurrency
            val baseAccounts = accounts.filter { it.currency == baseCurrency }
            val baseAccountIds = baseAccounts.map { it.id }.toSet()
            val baseTxs = txs.filter { it.accountId in baseAccountIds }

            val countedAccountIds = baseAccounts.filter { it.type in NET_WORTH_TYPES }.map { it.id }.toSet()
            val allIncome = baseTxs.filter { it.kind == "INCOME" && it.accountId in countedAccountIds }.sumOf { it.amountMinor }
            val allExpense = baseTxs.filter { it.kind == "EXPENSE" && it.accountId in countedAccountIds }.sumOf { it.amountMinor }
            val totalBalance = baseAccounts.filter { it.type in NET_WORTH_TYPES }.sumOf { it.openingBalanceMinor } + allIncome - allExpense

            val yearTx = baseTxs.filter { monthKeyOf(it.date) / 100 == year }
            val yearIncome = yearTx.filter { it.kind == "INCOME" }.sumOf { it.amountMinor }
            val yearExpense = yearTx.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor }

            val catNameById = cats.associate { it.id to it.name }
            val catColorById = cats.associate { it.id to it.color }

            val netMinor = yearIncome - yearExpense
            val savingsRate = computeSavingsRate(yearIncome, yearExpense)

            val balanceByAccountId = AccountsViewModel.computeBalances(baseAccounts, baseTxs)
            val accountBars = baseAccounts.map {
                AccountBar(name = it.name, type = it.type, balanceMinor = balanceByAccountId[it.id] ?: 0L)
            }

            val yearExpenseFloor = yearExpense.coerceAtLeast(1L).toFloat()
            val donut = yearTx.filter { it.kind == "EXPENSE" }
                .groupBy { it.categoryId }
                .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
                .entries
                .sortedByDescending { it.value }
                .map { (catId, amount) ->
                    CategorySlice(
                        name = catNameById[catId] ?: "Sin categoría",
                        amountMinor = amount,
                        pct = amount.toFloat() / yearExpenseFloor,
                        color = catColorById[catId] ?: 0L,
                    )
                }

            val budgetRows = BudgetsViewModel.computeBudgets(budgets, cats, baseTxs, monthPeriod(firstOfMonth))
                .rows.filter { it.limitMinor > 0 }

            val incomeByMonth = (1..12).map { m ->
                MonthAmount(mesCorto(m, locale), yearTx.filter { it.kind == "INCOME" && monthKeyOf(it.date) % 100 == m }.sumOf { it.amountMinor })
            }
            val expenseByMonth = (1..12).map { m ->
                MonthAmount(mesCorto(m, locale), yearTx.filter { it.kind == "EXPENSE" && monthKeyOf(it.date) % 100 == m }.sumOf { it.amountMinor })
            }
            val subscriptionSpend = yearTx.filter { it.kind == "EXPENSE" && it.subscriptionId != null }.sumOf { it.amountMinor }
            val netWorthByMonth = computeMonthlyNetWorth(baseAccounts, baseTxs, year, locale)

            return DashboardData(
                totalBalance, yearIncome, yearExpense, currency,
                netMinor, savingsRate, accountBars, donut, budgetRows,
                netWorthByMonth, incomeByMonth, expenseByMonth, subscriptionSpend,
            )
        }

        /** Patrimonio neto acumulado al cierre de cada mes del año [year]: saldo inicial de las
         * cuentas de patrimonio + (ingresos − gastos) de esas cuentas hasta el último día de ese mes,
         * inclusive. */
        internal fun computeMonthlyNetWorth(
            accounts: List<Account>,
            txs: List<TransactionRow>,
            year: Int,
            locale: CurrencyLocaleInfo = DEFAULT_LOCALE_INFO,
        ): List<MonthNetWorth> {
            val countedAccountIds = accounts.filter { it.type in NET_WORTH_TYPES }.map { it.id }.toSet()
            val opening = accounts.filter { it.type in NET_WORTH_TYPES }.sumOf { it.openingBalanceMinor }
            val countedTxs = txs.filter { it.accountId in countedAccountIds }
            return (1..12).map { m ->
                val lastDayEpoch = LocalDate(year, m, 1).plus(DatePeriod(months = 1)).toEpochDays().toLong() - 1
                val upTo = countedTxs.filter { it.date <= lastDayEpoch }
                val income = upTo.filter { it.kind == "INCOME" }.sumOf { it.amountMinor }
                val expense = upTo.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor }
                MonthNetWorth(mesCorto(m, locale), opening + income - expense)
            }
        }
    }
}
