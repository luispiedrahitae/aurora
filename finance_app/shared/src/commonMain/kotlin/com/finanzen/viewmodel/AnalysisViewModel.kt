package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.CategoryRepository
import com.finanzen.data.SubscriptionRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.db.Category
import com.finanzen.db.Subscription
import com.finanzen.db.TransactionRow
import com.finanzen.domain.RecurrenceSchedule
import com.finanzen.ui.format.formatDiaMes
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

data class CategorySlice(val name: String, val amountMinor: Long, val pct: Float, val color: Long = 0L)

/** Un día del gráfico de flujo (mes seleccionado): ingreso y gasto en paralelo. [dayNumber] es
 * solo el número de día, para el eje X del gráfico (mostrar "día + mes abreviado" ahí no cabe con
 * hasta 31 columnas); [label] ("26 jul") queda para el encabezado del día seleccionado, que sí es
 * un solo texto. */
data class DayPoint(val label: String, val dayNumber: Int, val incomeMinor: Long, val expenseMinor: Long)

/** Categoría de gasto ordenada por frecuencia (gasto hormiga). [amountMinor] es el total acumulado
 * de esos movimientos — la frecuencia dice qué compras seguido; el monto dice cuánto suma, que es
 * lo que habilita la decisión. [subcategoryName] solo si el movimiento está etiquetado con una
 * subcategoría (categoría con parentId); en ese caso [name] es la categoría padre. */
data class FrequentExpense(val name: String, val count: Int, val amountMinor: Long, val subcategoryName: String? = null)

/** Gasto total de una categoría padre en el periodo, con su desglose por subcategoría (vacío si
 * ninguna transacción de esta categoría está etiquetada con una subcategoría). [pct] es relativo
 * al total del periodo; el [pct] de cada [CategorySlice] en [subcategories] es relativo al total
 * de esta categoría, no al del periodo completo. */
data class CategorySpend(val name: String, val amountMinor: Long, val pct: Float, val color: Long, val subcategories: List<CategorySlice>)

data class AnalysisData(
    val totalIncomeMinor: Long,
    val totalExpenseMinor: Long,
    val savingsRate: Float,
    val currency: String,
    val cashflow: List<DayPoint>,
    val frequent: List<FrequentExpense>,
    val subscriptionMonthlyCostMinor: Long,
    val categoryBreakdown: List<CategorySpend>,
    // Totales del mes anterior, para los deltas "vs mes anterior"; con default para no romper
    // constructores posicionales (estado inicial y tests).
    val prevMonthIncomeMinor: Long = 0,
    val prevMonthExpenseMinor: Long = 0,
    // Gasto proyectado a fin de mes (regla de tres sobre los días transcurridos); null salvo que el
    // mes seleccionado sea el mes en curso.
    val projectedExpenseMinor: Long? = null,
)

class AnalysisViewModel(
    txRepo: TransactionRepository,
    categoryRepo: CategoryRepository,
    subscriptionRepo: SubscriptionRepository,
) : ViewModel() {

    private val _month = MutableStateFlow(
        run {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            LocalDate(today.year, today.month, 1)
        },
    )
    val month: StateFlow<LocalDate> = _month

    fun setMonth(month: LocalDate) {
        _month.value = month
    }

    val data: StateFlow<AnalysisData> =
        combine(txRepo.observeAll(), categoryRepo.observeAll(), subscriptionRepo.observeActive(), _month) { txs, cats, subscriptions, month ->
            val period = monthPeriod(month)
            val periodTxs = txs.filter { periodOfEpochDay(it.date) == period }
            val incomes = periodTxs.filter { it.kind == "INCOME" }.sumOf { it.amountMinor }
            val expenses = periodTxs.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor }
            val currency = periodTxs.firstOrNull()?.currency ?: txs.firstOrNull()?.currency ?: "USD"

            val cashflow = computeCashflow(txs, month)
            val frequent = computeFrequentExpenses(periodTxs.filter { it.kind == "EXPENSE" }, cats)
            val savingsRate = DashboardViewModel.computeSavingsRate(incomes, expenses)
            val subscriptionMonthlyCost = computeSubscriptionMonthlyCost(subscriptions)

            val categoryBreakdown = computeCategoryBreakdown(periodTxs.filter { it.kind == "EXPENSE" }, cats)

            // Mes anterior, para los deltas "vs mes anterior" de los tiles de ingreso/gasto.
            val prevPeriod = monthPeriod(month.plus(DatePeriod(months = -1)))
            val prevTxs = txs.filter { periodOfEpochDay(it.date) == prevPeriod }
            val prevIncome = prevTxs.filter { it.kind == "INCOME" }.sumOf { it.amountMinor }
            val prevExpense = prevTxs.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor }

            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val projected = projectMonthEndExpense(expenses, today, month)

            AnalysisData(
                incomes, expenses, savingsRate, currency, cashflow, frequent, subscriptionMonthlyCost, categoryBreakdown,
                prevMonthIncomeMinor = prevIncome, prevMonthExpenseMinor = prevExpense, projectedExpenseMinor = projected,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AnalysisData(0, 0, 0f, "USD", emptyList(), emptyList(), 0, emptyList()),
        )

    companion object {
        private const val FREQUENT_TOP = 5

        /** Fusiona las categorías mayores con la porción sintética "Otros" (ver `CategoryDonutChart`),
         * reordenando el conjunto final por pct descendente — "Otros" es la suma de todo lo que no
         * entró en el top y puede ser la porción más grande, así que no debe quedar fija al final. */
        internal fun mergeOthersByPct(top: List<CategorySlice>, otros: CategorySlice?): List<CategorySlice> = if (otros == null) top else (top + otros).sortedByDescending { it.pct }

        private fun daysInMonth(firstOfMonth: LocalDate): Int = firstOfMonth.plus(DatePeriod(months = 1)).toEpochDays() - firstOfMonth.toEpochDays()

        /** Gasto proyectado a fin de mes: regla de tres sobre los días transcurridos. Solo aplica
         * al mes en curso (null para meses pasados o futuros — allí no hay nada que proyectar). */
        internal fun projectMonthEndExpense(expenseSoFarMinor: Long, today: LocalDate, month: LocalDate): Long? {
            if (today.year != month.year || today.month != month.month) return null
            return expenseSoFarMinor * daysInMonth(month) / today.dayOfMonth
        }

        /** Ingreso y gasto de cada día del mes de [referenceMonth] (equivalente táctil de "hover": cada
         * día es un punto tocable, no hay hover real en Android/iOS). */
        internal fun computeCashflow(txs: List<TransactionRow>, referenceMonth: LocalDate): List<DayPoint> {
            val periodTxs = txs.filter { periodOfEpochDay(it.date) == monthPeriod(referenceMonth) }
            val byDay = periodTxs.groupBy { it.date }
            val firstEpochDay = referenceMonth.toEpochDays()
            return (0 until daysInMonth(referenceMonth)).map { offset ->
                val epochDay = (firstEpochDay + offset).toLong()
                val dayTxs = byDay[epochDay].orEmpty()
                DayPoint(
                    label = formatDiaMes(epochDay),
                    dayNumber = LocalDate.fromEpochDays(epochDay.toInt()).dayOfMonth,
                    incomeMinor = dayTxs.filter { it.kind == "INCOME" }.sumOf { it.amountMinor },
                    expenseMinor = dayTxs.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor },
                )
            }
        }

        /** Gasto hormiga: categorías de gasto ordenadas por número de movimientos. [expenseTxs]
         * debe venir ya filtrado a gasto y al periodo deseado (mensual o anual), igual que
         * [computeCategoryBreakdown] — así ambas se reutilizan desde Análisis (mes) y Resumen (año)
         * sin duplicar el filtrado de periodo. */
        internal fun computeFrequentExpenses(expenseTxs: List<TransactionRow>, cats: List<Category>, topN: Int = FREQUENT_TOP): List<FrequentExpense> {
            val catById = cats.associateBy { it.id }
            return expenseTxs
                .groupBy { it.categoryId }
                .map { (catId, list) ->
                    val leaf = catById[catId]
                    val parent = leaf?.parentId?.let { catById[it] }
                    FrequentExpense(
                        name = parent?.name ?: leaf?.name ?: "Sin categoría",
                        count = list.size,
                        amountMinor = list.sumOf { it.amountMinor },
                        subcategoryName = if (parent != null) leaf?.name else null,
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
         * periodo deseado (mensual o anual); esta función no filtra por fecha. */
        internal fun computeCategoryBreakdown(expenseTxs: List<TransactionRow>, cats: List<Category>): List<CategorySpend> {
            val catById = cats.associateBy { it.id }
            val total = expenseTxs.sumOf { it.amountMinor }.coerceAtLeast(1L).toFloat()
            return expenseTxs.groupBy { tx ->
                val leaf = catById[tx.categoryId]
                leaf?.parentId?.let { catById[it] } ?: leaf
            }.map { (parent, list) ->
                val parentAmount = list.sumOf { it.amountMinor }
                val parentTotal = parentAmount.coerceAtLeast(1L).toFloat()
                val subcategories = list.groupBy { catById[it.categoryId] }
                    .filterKeys { it != null && it.id != parent?.id }
                    .map { (leaf, subList) ->
                        val subAmount = subList.sumOf { it.amountMinor }
                        CategorySlice(name = leaf!!.name, amountMinor = subAmount, pct = subAmount / parentTotal, color = leaf.color)
                    }
                    .sortedByDescending { it.amountMinor }
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
