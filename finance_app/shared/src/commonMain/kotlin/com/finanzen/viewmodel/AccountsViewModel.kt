package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.AccountRepository
import com.finanzen.data.CardRepository
import com.finanzen.data.InstallmentPlanRepository
import com.finanzen.data.SettingsRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.db.Account
import com.finanzen.db.Card
import com.finanzen.db.InstallmentPlan
import com.finanzen.db.TransactionRow
import com.finanzen.domain.InstallmentMath
import com.finanzen.platform.NotificationScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

class AccountsViewModel(
    private val accountRepo: AccountRepository,
    private val txRepo: TransactionRepository,
    private val cardRepo: CardRepository,
    private val planRepo: InstallmentPlanRepository,
    private val settingsRepo: SettingsRepository,
    private val scheduler: NotificationScheduler,
) : ViewModel() {

    init {
        // ponytail: catch-up al abrir Cuentas; subir a app-start si se requiere puntualidad estricta
        // (mismo enfoque que SubscriptionsViewModel.postDueCharges()).
        postDueInstallments()
    }

    val accounts: StateFlow<List<Account>> =
        accountRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val archivedAccounts: StateFlow<List<Account>> =
        accountRepo.observeArchived().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Saldo actual por cuenta (id → minor) = saldo inicial + ingresos − gastos ± transferencias. */
    val balances: StateFlow<Map<Long, Long>> =
        combine(accountRepo.observeAllIncludingArchived(), txRepo.observeAll()) { accts, txs -> computeBalances(accts, txs) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Movimientos de cada cuenta, agrupados desde el mismo flujo ya suscrito para [balances] —
     * evita abrir una suscripción nueva por cada fila expandida en la UI. El filtro por mes/tipo
     * se aplica en la pantalla (mismo patrón que TransactionsScreen/AnalysisViewModel). */
    val transactionsByAccount: StateFlow<Map<Long, List<TransactionRow>>> =
        txRepo.observeAll().map { groupByAccount(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Metadatos de tarjeta por cuenta (cupo/corte/pago/interés) para las cuentas de crédito. */
    val cards: StateFlow<Map<Long, Card>> =
        cardRepo.observeAll().map { list -> list.associateBy { it.accountId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Planes de cuotas agrupados por cuenta (vía su tarjeta de respaldo). */
    val plansByAccount: StateFlow<Map<Long, List<InstallmentPlan>>> =
        combine(cardRepo.observeAll(), planRepo.observeAll()) { cardsList, plans ->
            val accountByCard = cardsList.associate { it.id to it.accountId }
            plans.groupBy { accountByCard[it.cardId] ?: -1L }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /**
     * Compromiso aún no facturado de las compras a cuotas activas, por cuenta. Un banco real
     * reserva el cupo completo desde el día de la compra, no solo lo ya cobrado — por eso esto se
     * resta aparte de [balances] al calcular el cupo disponible (ver `CreditDetails`).
     */
    val unbilledCommitmentByAccount: StateFlow<Map<Long, Long>> =
        plansByAccount.map { byAccount ->
            val today = todayEpochDay()
            byAccount.mapValues { (_, plans) ->
                plans.sumOf {
                    InstallmentMath.remainingCommitmentMinor(it.totalAmountMinor, it.installments, it.interestRate, it.startDate, today, it.settled == 1L)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** La app es de moneda única; las cuentas nuevas heredan la moneda base (ver Ajustes). */
    val baseCurrency: String get() = settingsRepo.baseCurrency()

    val accountTypes: List<String> = listOf("CASH", "DEBIT", "SAVINGS", "CREDIT")

    /**
     * Crea una cuenta según su tipo. Para crédito guarda los metadatos de tarjeta (cupo, corte, pago,
     * interés) en la tabla Card; el resto usa el saldo inicial. Devuelve el id de la cuenta creada.
     */
    fun addAccount(
        type: String,
        name: String,
        amountMinor: Long = 0,
        creditLimitMinor: Long? = null,
        cutoffDay: Long? = null,
        dueDay: Long? = null,
        interestRate: Double? = null,
    ): Long? {
        if (name.isBlank()) return null
        return if (type == "CREDIT") {
            val accountId = accountRepo.add(name.trim(), "CREDIT", settingsRepo.baseCurrency(), openingBalanceMinor = 0)
            cardRepo.add(
                accountId = accountId,
                last4 = "",
                network = "OTRA",
                creditLimitMinor = creditLimitMinor,
                cutoffDay = cutoffDay,
                dueDay = dueDay,
                interestRate = interestRate,
            )
            scheduleCardReminders(accountId, name.trim(), cutoffDay, dueDay)
            accountId
        } else {
            accountRepo.add(name.trim(), type, settingsRepo.baseCurrency(), openingBalanceMinor = amountMinor)
        }
    }

    /**
     * Programa recordatorios de corte/pago si el usuario los habilitó en opciones.
     * `// ponytail:` one-shot a la próxima ocurrencia del día; la recurrencia mensual real se añade
     * reprogramando al abrir la app si hace falta.
     */
    private fun scheduleCardReminders(accountId: Long, name: String, cutoffDay: Long?, dueDay: Long?) {
        if (!settingsRepo.cardNotificationsEnabled()) return
        cutoffDay?.let { scheduler.scheduleReminder(CUTOFF_NOTIF_BASE + accountId, "Corte de $name", "Hoy es el corte de tu tarjeta.", nextDayOfMonthEpoch(it.toInt())) }
        dueDay?.let { scheduler.scheduleReminder(DUE_NOTIF_BASE + accountId, "Pago de $name", "Hoy vence el pago de tu tarjeta.", nextDayOfMonthEpoch(it.toInt())) }
    }

    /** Próxima fecha (epoch day) con ese día del mes, contando desde hoy. Acota a la duración del mes. */
    private fun nextDayOfMonthEpoch(day: Int): Long {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        fun candidate(base: LocalDate): LocalDate {
            val first = LocalDate(base.year, base.month, 1)
            val len = (first.plus(DatePeriod(months = 1)).toEpochDays() - first.toEpochDays())
            return LocalDate(base.year, base.month, day.coerceIn(1, len))
        }
        val thisMonth = candidate(today)
        val target = if (thisMonth >= today) thisMonth else candidate(today.plus(DatePeriod(months = 1)))
        return target.toEpochDays().toLong()
    }

    /**
     * Borra la cuenta y su tarjeta de respaldo (si es de crédito). Borramos la tarjeta a mano en vez
     * de depender de ON DELETE CASCADE porque el enforcement de FKs no está garantizado en SQLite.
     */
    fun delete(id: Long) {
        cardRepo.byAccount(id)?.let { cardRepo.delete(it.id) }
        accountRepo.delete(id)
    }

    fun archive(id: Long) = accountRepo.archive(id)
    fun unarchive(id: Long) = accountRepo.unarchive(id)

    fun updateAccount(id: Long, name: String, openingBalanceMinor: Long) = accountRepo.updateBasics(id, name.trim(), openingBalanceMinor)

    fun updateCard(cardId: Long, creditLimitMinor: Long?, cutoffDay: Long?, dueDay: Long?, interestRate: Double?) = cardRepo.updateCreditTerms(cardId, creditLimitMinor, cutoffDay, dueDay, interestRate)

    /**
     * Genera las cuotas 2..N pendientes de cada plan activo hasta el mes actual (mismo patrón que
     * `SubscriptionsViewModel.postDueCharges()`). La cuota 1 ya se registró al crear el plan
     * (`TransactionsViewModel.maybeCreatePlan`); esto solo pone al día los meses siguientes.
     */
    private fun postDueInstallments() {
        val today = todayEpochDay()
        val accountsById = accountRepo.allIncludingArchived().associateBy { it.id }
        cardRepo.all().forEach { card ->
            val account = accountsById[card.accountId] ?: return@forEach
            planRepo.activeByCard(card.id).forEach { plan ->
                val target = (InstallmentMath.elapsedInstallments(plan.startDate, today, plan.installments) + 1)
                    .coerceAtMost(plan.installments)
                val posted = txRepo.countForPlan(plan.id)
                if (posted < target) {
                    val monthly = InstallmentMath.monthlyPaymentMinor(plan.totalAmountMinor, plan.installments, plan.interestRate)
                    for (i in posted until target) {
                        txRepo.add(
                            accountId = account.id,
                            categoryId = plan.categoryId,
                            amountMinor = monthly,
                            currency = account.currency,
                            epochDay = InstallmentMath.installmentDueDate(plan.startDate, i),
                            note = plan.description,
                            kind = "EXPENSE",
                            installmentPlanId = plan.id,
                        )
                    }
                }
            }
        }
    }

    /**
     * Paga la tarjeta: transfiere [amountMinor] desde [sourceAccountId] a la cuenta de la tarjeta
     * (reutiliza la transferencia existente, no inventa un movimiento de dinero nuevo) y detiene la
     * generación futura de cuotas de sus planes activos, liberando el cupo comprometido.
     */
    fun payOffCard(cardId: Long, sourceAccountId: Long, amountMinor: Long) {
        val card = cardRepo.all().firstOrNull { it.id == cardId } ?: return
        val currency = accountRepo.allIncludingArchived().firstOrNull { it.id == sourceAccountId }?.currency ?: settingsRepo.baseCurrency()
        txRepo.addTransfer(sourceAccountId, card.accountId, amountMinor, currency, todayEpochDay(), "Pago de tarjeta")
        planRepo.settleAllForCard(cardId)
    }

    private fun todayEpochDay(): Long = Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays().toLong()

    companion object {
        // Offsets para que los ids de notificación de tarjeta no choquen entre sí ni con otros.
        private const val CUTOFF_NOTIF_BASE = 2_000_000L
        private const val DUE_NOTIF_BASE = 3_000_000L

        /** Agrupación pura y testeable (mismo criterio que Transaction.sq:selectByAccount). */
        fun groupByAccount(txs: List<TransactionRow>): Map<Long, List<TransactionRow>> = txs.groupBy { it.accountId }

        /** Nº de cuota (1-based) de cada transacción dentro de su plan, por fecha ascendente. */
        fun installmentIndexByTransaction(movements: List<TransactionRow>): Map<Long, Long> = movements.filter { it.installmentPlanId != null }
            .groupBy { it.installmentPlanId }
            .flatMap { (_, txs) ->
                txs.sortedWith(compareBy({ it.date }, { it.id })).mapIndexed { idx, tx -> tx.id to (idx + 1).toLong() }
            }
            .toMap()

        /** Agregación pura y testeable. Una TRANSFER resta del origen y suma al destino. */
        fun computeBalances(accounts: List<Account>, txs: List<TransactionRow>): Map<Long, Long> {
            val balance = accounts.associate { it.id to it.openingBalanceMinor }.toMutableMap()
            for (t in txs) {
                when (t.kind) {
                    "INCOME" -> balance[t.accountId] = (balance[t.accountId] ?: 0L) + t.amountMinor
                    "EXPENSE" -> balance[t.accountId] = (balance[t.accountId] ?: 0L) - t.amountMinor
                    "TRANSFER" -> {
                        balance[t.accountId] = (balance[t.accountId] ?: 0L) - t.amountMinor
                        t.transferAccountId?.let { to -> balance[to] = (balance[to] ?: 0L) + t.amountMinor }
                    }
                }
            }
            return balance
        }
    }
}
