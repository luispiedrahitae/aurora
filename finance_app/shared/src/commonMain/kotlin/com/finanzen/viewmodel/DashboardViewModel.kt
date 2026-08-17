package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.AccountRepository
import com.finanzen.data.CURRENCY_LOCALE_INFO
import com.finanzen.data.CategoryRepository
import com.finanzen.data.CurrencyLocaleInfo
import com.finanzen.data.DEFAULT_LOCALE_INFO
import com.finanzen.data.SettingsRepository
import com.finanzen.data.SubscriptionRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.db.Account
import com.finanzen.db.Category
import com.finanzen.db.Subscription
import com.finanzen.db.TransactionRow
import com.finanzen.domain.RecurrenceSchedule
import com.finanzen.ui.format.PeriodMode
import com.finanzen.ui.format.formatDiaMes
import com.finanzen.ui.format.mesCorto
import com.finanzen.ui.format.monthPeriod
import com.finanzen.ui.format.periodOfEpochDay
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
    val periodIncomeMinor: Long,
    val periodExpenseMinor: Long,
    val currency: String,
    val netMinor: Long,
    val savingsRate: Float,
    val accounts: List<AccountBar>,
    val netWorthTrend: List<MonthNetWorth>,
    val incomeExpenseTrend: List<TrendPoint>,
    val subscriptionCostMinor: Long,
    val categoryBreakdown: List<CategorySpend>,
    val frequent: List<FrequentExpense>,
    // Totales del periodo anterior (mes o año, según el modo activo), para los deltas "vs
    // anterior" de los KPI. Con default para no romper los constructores posicionales existentes
    // (estado inicial y tests).
    val prevPeriodIncomeMinor: Long = 0,
    val prevPeriodExpenseMinor: Long = 0,
    // Gasto proyectado a fin de mes (regla de tres sobre los días transcurridos); solo se calcula
    // en modo MONTH cuando el mes seleccionado es el mes en curso, null en cualquier otro caso.
    val projectedExpenseMinor: Long? = null,
)

/** Una cuenta con su saldo actual, para la fila de barras del inicio. */
data class AccountBar(val name: String, val type: String, val balanceMinor: Long)

/** Un punto del gráfico de patrimonio neto: etiqueta corta + monto acumulado a esa fecha. Se
 * reutiliza tal cual para ambos modos — un punto por mes (modo AÑO) o un punto por día (modo MES,
 * ver [DashboardViewModel.computeDailyNetWorth]) — el tipo ya era genérico (label + valor), solo
 * el nombre viene del uso original mensual. */
data class MonthNetWorth(val label: String, val netWorthMinor: Long)

/** Un punto (día o mes, según el modo) del gráfico de ingresos/gastos: [axisLabel] es la etiqueta
 * corta del eje X ("15" en modo mes, "Jul" en modo año); [label] es la etiqueta completa que se
 * muestra como titular al seleccionar la columna ("15 jul" en modo mes, "Jul" en modo año). */
data class TrendPoint(val label: String, val axisLabel: String, val incomeMinor: Long, val expenseMinor: Long)

data class CategorySlice(val name: String, val amountMinor: Long, val pct: Float, val color: Long = 0L)

/** Categoría de gasto ordenada por frecuencia (gasto hormiga). [amountMinor] es el total acumulado
 * de esos movimientos — la frecuencia dice qué compras seguido; el monto dice cuánto suma, que es
 * lo que habilita la decisión. Solo incluye movimientos etiquetados con una subcategoría real
 * (categoría con parentId); [name] es la categoría padre y [subcategoryName] la subcategoría —
 * ver [DashboardViewModel.computeFrequentExpenses]. */
data class FrequentExpense(val name: String, val count: Int, val amountMinor: Long, val subcategoryName: String? = null)

/** Gasto total de una categoría padre en el periodo, con su desglose por subcategoría (vacío si
 * ninguna transacción de esta categoría está etiquetada con una subcategoría). [pct] es relativo
 * al total del periodo; el [pct] de cada [CategorySlice] en [subcategories] es relativo al total
 * de esta categoría, no al del periodo completo. */
data class CategorySpend(val name: String, val amountMinor: Long, val pct: Float, val color: Long, val subcategories: List<CategorySlice>)

/**
 * Pantalla de inicio ("Resumen"): balance general (saldos iniciales + ingresos − gastos de todo el
 * histórico, sin acotar al periodo), ingreso/gasto/tasa de ahorro/patrimonio neto/costo de
 * suscripciones del periodo seleccionado, saldos por cuenta (foto actual) y gasto por categoría del
 * periodo (con subcategorías). El periodo se filtra por mes o por año según [mode] — todos los
 * tableros que dependen del periodo (KPIs, tendencia ingresos/gastos, patrimonio neto, categorías,
 * gastos frecuentes) se recalculan sobre esa ventana; el balance total y "saldos por cuenta" son
 * una foto actual y no dependen de [mode]. El cálculo vive en [computeDashboard] (testeable); nada
 * se calcula en la UI.
 */
class DashboardViewModel(
    txRepo: TransactionRepository,
    accountRepo: AccountRepository,
    categoryRepo: CategoryRepository,
    subscriptionRepo: SubscriptionRepository,
    settingsRepo: SettingsRepository,
) : ViewModel() {

    private val _mode = MutableStateFlow(PeriodMode.YEAR)
    val mode: StateFlow<PeriodMode> = _mode

    private val _month = MutableStateFlow(
        run {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            LocalDate(today.year, today.month, 1)
        },
    )
    val month: StateFlow<LocalDate> = _month

    private val _year = MutableStateFlow(Clock.System.todayIn(TimeZone.currentSystemDefault()).year)
    val year: StateFlow<Int> = _year

    fun setMode(mode: PeriodMode) {
        _mode.value = mode
    }

    fun setMonth(month: LocalDate) {
        _month.value = month
    }

    fun setYear(year: Int) {
        _year.value = year
    }

    val data: StateFlow<DashboardData> =
        combine(
            txRepo.observeAll(),
            accountRepo.observeAll(),
            categoryRepo.observeAll(),
            subscriptionRepo.observeActive(),
            combine(_mode, _month, _year) { mode, month, year -> Triple(mode, month, year) },
        ) { txs, accounts, cats, subscriptions, (mode, month, year) ->
            val baseCurrency = settingsRepo.baseCurrency()
            val locale = CURRENCY_LOCALE_INFO[baseCurrency] ?: DEFAULT_LOCALE_INFO
            computeDashboard(txs, accounts, cats, subscriptions, mode, month, year, baseCurrency, locale)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DashboardData(0, 0, 0, DEFAULT_CURRENCY, 0L, 0f, emptyList(), emptyList(), emptyList(), 0L, emptyList(), emptyList()),
        )

    companion object {
        private const val DEFAULT_CURRENCY = "USD"
        private const val FREQUENT_TOP = 5
        internal val NET_WORTH_TYPES = setOf("CASH", "DEBIT", "SAVINGS")

        private fun monthKey(year: Int, monthNumber: Int): Int = year * 100 + monthNumber
        private fun monthKeyOf(epochDay: Long): Int = LocalDate.fromEpochDays(epochDay.toInt()).let { monthKey(it.year, it.monthNumber) }
        private fun daysInMonth(firstOfMonth: LocalDate): Int = firstOfMonth.plus(DatePeriod(months = 1)).toEpochDays() - firstOfMonth.toEpochDays()

        /** (ingreso − gasto) ÷ ingreso, tope 100%; sin ingresos, 0% (no divide por cero). Puede ser
         * negativa: gastar más de lo ingresado es un déficit y la UI debe poder mostrarlo, no
         * aplanarlo a 0%. */
        internal fun computeSavingsRate(income: Long, expense: Long): Float = if (income == 0L) {
            0f
        } else {
            ((income - expense).toFloat() / income.toFloat()).coerceAtMost(1f)
        }

        /** Filtra transacciones al mes o año activo (mismo patrón que [ReportsViewModel.filterByPeriod]). */
        internal fun filterByPeriod(txs: List<TransactionRow>, mode: PeriodMode, month: LocalDate, year: Int): List<TransactionRow> = when (mode) {
            PeriodMode.MONTH -> txs.filter { periodOfEpochDay(it.date) == monthPeriod(month) }
            PeriodMode.YEAR -> txs.filter { periodOfEpochDay(it.date) / 100 == year.toLong() }
        }

        /** Agregación pura. `mode`/`month`/`year` alimentan todos los totales (inyectable para tests). */
        internal fun computeDashboard(
            txs: List<TransactionRow>,
            accounts: List<Account>,
            cats: List<Category>,
            subscriptions: List<Subscription> = emptyList(),
            mode: PeriodMode,
            month: LocalDate,
            year: Int,
            baseCurrency: String,
            locale: CurrencyLocaleInfo = DEFAULT_LOCALE_INFO,
            today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
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

            val periodTxs = filterByPeriod(baseTxs, mode, month, year)
            val periodIncome = periodTxs.filter { it.kind == "INCOME" }.sumOf { it.amountMinor }
            val periodExpense = periodTxs.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor }

            // Periodo anterior (mes previo o año previo, según el modo), para los deltas "vs
            // anterior" de los KPI de ingresos/gastos.
            val prevPeriodTxs = when (mode) {
                PeriodMode.MONTH -> filterByPeriod(baseTxs, mode, month.plus(DatePeriod(months = -1)), year)
                PeriodMode.YEAR -> filterByPeriod(baseTxs, mode, month, year - 1)
            }
            val prevIncome = prevPeriodTxs.filter { it.kind == "INCOME" }.sumOf { it.amountMinor }
            val prevExpense = prevPeriodTxs.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor }

            val netMinor = periodIncome - periodExpense
            val savingsRate = computeSavingsRate(periodIncome, periodExpense)

            val balanceByAccountId = AccountsViewModel.computeBalances(baseAccounts, baseTxs)
            val accountBars = baseAccounts.map {
                AccountBar(name = it.name, type = it.type, balanceMinor = balanceByAccountId[it.id] ?: 0L)
            }

            val periodExpenseTxs = periodTxs.filter { it.kind == "EXPENSE" }
            val categoryBreakdown = computeCategoryBreakdown(periodExpenseTxs, cats, subscriptions)
            val frequent = computeFrequentExpenses(periodExpenseTxs, cats)

            val incomeExpenseTrend = when (mode) {
                PeriodMode.MONTH -> computeDailyTrend(baseTxs, month)
                PeriodMode.YEAR -> computeMonthlyTrend(periodTxs, locale)
            }
            val netWorthTrend = when (mode) {
                PeriodMode.MONTH -> computeDailyNetWorth(baseAccounts, baseTxs, month)
                PeriodMode.YEAR -> computeMonthlyNetWorth(baseAccounts, baseTxs, year, locale)
            }

            // Costo recurrente mensual de las suscripciones activas: en modo AÑO se anualiza (× 12)
            // en vez de sumar el gasto histórico por transacción, para tener una sola fuente de
            // cálculo (el repositorio de suscripciones) en ambos modos.
            val subscriptionMonthlyCost = computeSubscriptionMonthlyCost(subscriptions)
            val subscriptionCost = when (mode) {
                PeriodMode.MONTH -> subscriptionMonthlyCost
                PeriodMode.YEAR -> subscriptionMonthlyCost * 12
            }

            val projected = if (mode == PeriodMode.MONTH) projectMonthEndExpense(periodExpense, today, month) else null

            return DashboardData(
                totalBalanceMinor = totalBalance,
                periodIncomeMinor = periodIncome,
                periodExpenseMinor = periodExpense,
                currency = currency,
                netMinor = netMinor,
                savingsRate = savingsRate,
                accounts = accountBars,
                netWorthTrend = netWorthTrend,
                incomeExpenseTrend = incomeExpenseTrend,
                subscriptionCostMinor = subscriptionCost,
                categoryBreakdown = categoryBreakdown,
                frequent = frequent,
                prevPeriodIncomeMinor = prevIncome,
                prevPeriodExpenseMinor = prevExpense,
                projectedExpenseMinor = projected,
            )
        }

        /** Ingreso y gasto de cada mes del año [periodTxs] (ya filtrado a ese año) — 12 puntos. */
        internal fun computeMonthlyTrend(periodTxs: List<TransactionRow>, locale: CurrencyLocaleInfo = DEFAULT_LOCALE_INFO): List<TrendPoint> = (1..12).map { m ->
            val label = mesCorto(m, locale)
            TrendPoint(
                label = label,
                axisLabel = label,
                incomeMinor = periodTxs.filter { it.kind == "INCOME" && monthKeyOf(it.date) % 100 == m }.sumOf { it.amountMinor },
                expenseMinor = periodTxs.filter { it.kind == "EXPENSE" && monthKeyOf(it.date) % 100 == m }.sumOf { it.amountMinor },
            )
        }

        /** Ingreso y gasto de cada día del mes de [referenceMonth] (equivalente táctil de "hover": cada
         * día es un punto tocable, no hay hover real en Android/iOS). */
        internal fun computeDailyTrend(txs: List<TransactionRow>, referenceMonth: LocalDate): List<TrendPoint> {
            val periodTxs = txs.filter { periodOfEpochDay(it.date) == monthPeriod(referenceMonth) }
            val byDay = periodTxs.groupBy { it.date }
            val firstEpochDay = referenceMonth.toEpochDays()
            return (0 until daysInMonth(referenceMonth)).map { offset ->
                val epochDay = (firstEpochDay + offset).toLong()
                val dayTxs = byDay[epochDay].orEmpty()
                TrendPoint(
                    label = formatDiaMes(epochDay),
                    axisLabel = LocalDate.fromEpochDays(epochDay.toInt()).dayOfMonth.toString(),
                    incomeMinor = dayTxs.filter { it.kind == "INCOME" }.sumOf { it.amountMinor },
                    expenseMinor = dayTxs.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor },
                )
            }
        }

        /** Patrimonio neto acumulado al cierre de cada mes del año [year]: saldo inicial de las
         * cuentas de patrimonio + (ingresos − gastos) de esas cuentas hasta el último día de ese mes,
         * inclusive. Usado también por [ReportsViewModel] — no cambiar su forma sin revisar ese uso. */
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

        /** Patrimonio neto acumulado al cierre de cada día del mes [month] — mismo cálculo que
         * [computeMonthlyNetWorth] pero con un punto por día en vez de por mes. */
        internal fun computeDailyNetWorth(accounts: List<Account>, txs: List<TransactionRow>, month: LocalDate): List<MonthNetWorth> {
            val countedAccountIds = accounts.filter { it.type in NET_WORTH_TYPES }.map { it.id }.toSet()
            val opening = accounts.filter { it.type in NET_WORTH_TYPES }.sumOf { it.openingBalanceMinor }
            val countedTxs = txs.filter { it.accountId in countedAccountIds }
            val firstEpochDay = month.toEpochDays()
            return (0 until daysInMonth(month)).map { offset ->
                val epochDay = (firstEpochDay + offset).toLong()
                val upTo = countedTxs.filter { it.date <= epochDay }
                val income = upTo.filter { it.kind == "INCOME" }.sumOf { it.amountMinor }
                val expense = upTo.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor }
                val dayNumber = LocalDate.fromEpochDays(epochDay.toInt()).dayOfMonth
                MonthNetWorth(dayNumber.toString(), opening + income - expense)
            }
        }

        /** Gasto proyectado a fin de mes: regla de tres sobre los días transcurridos. Solo aplica
         * al mes en curso (null para meses pasados o futuros — allí no hay nada que proyectar). */
        internal fun projectMonthEndExpense(expenseSoFarMinor: Long, today: LocalDate, month: LocalDate): Long? {
            if (today.year != month.year || today.month != month.month) return null
            return expenseSoFarMinor * daysInMonth(month) / today.dayOfMonth
        }

        /** Fusiona las categorías mayores con la porción sintética "Otros" (ver `CategoryDonutChart`),
         * reordenando el conjunto final por pct descendente — "Otros" es la suma de todo lo que no
         * entró en el top y puede ser la porción más grande, así que no debe quedar fija al final. */
        internal fun mergeOthersByPct(top: List<CategorySlice>, otros: CategorySlice?): List<CategorySlice> = if (otros == null) top else (top + otros).sortedByDescending { it.pct }

        /** Gasto hormiga: subcategorías de gasto ordenadas por número de movimientos. [expenseTxs]
         * debe venir ya filtrado a gasto y al periodo deseado (mensual o anual), igual que
         * [computeCategoryBreakdown] — así ambas se reutilizan desde el mismo cómputo sin duplicar el
         * filtrado de periodo. Descarta movimientos etiquetados directamente en una categoría padre
         * (sin subcategoría, p.ej. datos legados de antes de que la subcategoría fuera obligatoria) —
         * esta lista es de subcategorías, no de categorías. */
        internal fun computeFrequentExpenses(expenseTxs: List<TransactionRow>, cats: List<Category>, topN: Int = FREQUENT_TOP): List<FrequentExpense> {
            val catById = cats.associateBy { it.id }
            return expenseTxs
                .groupBy { it.categoryId }
                .mapNotNull { (catId, list) ->
                    val leaf = catById[catId] ?: return@mapNotNull null
                    val parent = leaf.parentId?.let { catById[it] } ?: return@mapNotNull null
                    FrequentExpense(
                        name = parent.name,
                        count = list.size,
                        amountMinor = list.sumOf { it.amountMinor },
                        subcategoryName = leaf.name,
                    )
                }
                .sortedByDescending { it.count }
                .take(topN)
        }

        /** Costo mensual equivalente sumado de todas las [subscriptions] recibidas (ya filtradas a
         * activas por el repositorio — ver [SubscriptionRepository.observeActive]). */
        internal fun computeSubscriptionMonthlyCost(subscriptions: List<Subscription>): Long = subscriptions.sumOf { RecurrenceSchedule.monthlyEquivalent(it.amountMinor, it.frequency, it.intervalCount) }

        /** Gasto por categoría (subiendo cada subcategoría a su padre, igual que [computeFrequentExpenses])
         * con el desglose por subcategoría anidado dentro de cada una. A diferencia de [computeFrequentExpenses]
         * (que ordena por número de movimientos), esto ordena por monto en ambos niveles — es la vista de
         * "cuánto gasté", no de "qué compro seguido". [expenseTxs] debe venir ya filtrado a gasto y al
         * periodo deseado (mensual o anual); esta función no filtra por fecha.
         *
         * Caso especial "Suscripciones": todas las suscripciones comparten una única subcategoría de
         * sistema (ver [DefaultSeed]), así que agruparían en una sola fila. En su lugar, dentro de esa
         * categoría se desglosa por suscripción individual (usando [subscriptions] para el nombre), para
         * que Netflix/Spotify/etc. aparezcan como si fueran subcategorías propias. */
        internal fun computeCategoryBreakdown(
            expenseTxs: List<TransactionRow>,
            cats: List<Category>,
            subscriptions: List<Subscription> = emptyList(),
        ): List<CategorySpend> {
            val catById = cats.associateBy { it.id }
            val subscriptionById = subscriptions.associateBy { it.id }
            val total = expenseTxs.sumOf { it.amountMinor }.coerceAtLeast(1L).toFloat()
            return expenseTxs.groupBy { tx ->
                val leaf = catById[tx.categoryId]
                leaf?.parentId?.let { catById[it] } ?: leaf
            }.map { (parent, list) ->
                val parentAmount = list.sumOf { it.amountMinor }
                val parentTotal = parentAmount.coerceAtLeast(1L).toFloat()
                val subcategories = if (parent?.name == "Suscripciones") {
                    list.groupBy { tx -> tx.subscriptionId?.let { subscriptionById[it] }?.name ?: catById[tx.categoryId]?.name ?: "Suscripción" }
                        .map { (name, subList) ->
                            val subAmount = subList.sumOf { it.amountMinor }
                            CategorySlice(name = name, amountMinor = subAmount, pct = subAmount / parentTotal, color = 0L)
                        }
                        .sortedByDescending { it.amountMinor }
                } else {
                    list.groupBy { catById[it.categoryId] }
                        .filterKeys { it != null && it.id != parent?.id }
                        .map { (leaf, subList) ->
                            val subAmount = subList.sumOf { it.amountMinor }
                            CategorySlice(name = leaf!!.name, amountMinor = subAmount, pct = subAmount / parentTotal, color = leaf.color)
                        }
                        .sortedByDescending { it.amountMinor }
                }
                CategorySpend(
                    name = parent?.name ?: "Sin categoría",
                    amountMinor = parentAmount,
                    pct = parentAmount / total,
                    color = parent?.color ?: 0L,
                    subcategories = subcategories,
                )
            }.sortedByDescending { it.amountMinor }
        }
    }
}
