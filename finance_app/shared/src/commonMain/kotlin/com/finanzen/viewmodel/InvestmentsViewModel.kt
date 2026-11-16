package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.AccountRepository
import com.finanzen.data.CategoryRepository
import com.finanzen.data.InvestmentRepository
import com.finanzen.data.SettingsRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.db.Account
import com.finanzen.db.Investment
import com.finanzen.db.TransactionRow
import com.finanzen.domain.RecurrenceSchedule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class InvestmentsViewModel(
    private val investRepo: InvestmentRepository,
    private val accountRepo: AccountRepository,
    private val txRepo: TransactionRepository,
    private val settingsRepo: SettingsRepository,
    private val categoryRepo: CategoryRepository,
) : ViewModel() {

    val openInvestments: StateFlow<List<Investment>> =
        investRepo.observeOpen().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val closedInvestments: StateFlow<List<Investment>> =
        investRepo.observeClosed().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val archivedInvestments: StateFlow<List<Investment>> =
        investRepo.observeArchived().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val accounts: StateFlow<List<Account>> =
        accountRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Movimientos (aportes = EXPENSE, retiros = INCOME) de cada inversión, agrupados desde el flujo ya suscrito por Movimientos — igual que
     * `AccountsViewModel.transactionsByAccount`, evita una suscripción nueva por fila expandida. */
    val contributionsByInvestment: StateFlow<Map<Long, List<TransactionRow>>> =
        txRepo.observeAll().map { groupByInvestment(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val baseCurrency: String get() = settingsRepo.baseCurrency()

    init {
        // ponytail: catch-up al abrir Inversiones; subir a app-start si se requiere puntualidad estricta
        // (mismo enfoque que SubscriptionsViewModel.postDueCharges()).
        postDueContributions()
    }

    /**
     * Crea la inversión y registra el aporte de **hoy** como gasto en Movimientos. Si es periódica,
     * programa el siguiente aporte (reutiliza el catch-up de abajo en vez de duplicar el "aporta hoy").
     */
    fun addInvestment(
        name: String,
        amountMinor: Long,
        accountId: Long,
        periodic: Boolean,
        frequency: String?,
        intervalCount: Long?,
        startEpochDay: Long,
    ) {
        val account = accountRepo.all().firstOrNull { it.id == accountId } ?: return
        val categoryId = categoryRepo.systemLeaf("EXPENSE", "Inversiones", "Inversión")
        val id = investRepo.add(
            name = name,
            amountMinor = amountMinor,
            currency = account.currency,
            accountId = account.id,
            categoryId = categoryId,
            periodic = periodic,
            frequency = if (periodic) frequency else null,
            intervalCount = if (periodic) intervalCount else null,
            nextContributionDate = if (periodic) startEpochDay else null,
            startDate = startEpochDay,
        )
        if (periodic) {
            postDueContributions()
        } else {
            txRepo.add(
                accountId = account.id,
                categoryId = categoryId,
                amountMinor = amountMinor,
                currency = account.currency,
                epochDay = startEpochDay,
                note = "Aporte: $name",
                kind = "EXPENSE",
                investmentId = id,
            )
        }
    }

    /** Registra como gasto cada aporte vencido y avanza la fecha hasta dejarla en el futuro. Idempotente. */
    private fun postDueContributions() {
        val today = todayEpochDay()
        investRepo.openNow().forEach { inv ->
            val frequency = inv.frequency
            val interval = inv.intervalCount
            val next = inv.nextContributionDate
            if (frequency == null || interval == null || next == null) return@forEach
            val account = accountRepo.all().firstOrNull { it.id == inv.accountId } ?: return@forEach
            val due = RecurrenceSchedule.occurrencesDueUpTo(next, today, frequency, interval)
            due.dates.forEach { contributionDay ->
                txRepo.add(
                    accountId = account.id,
                    categoryId = inv.categoryId,
                    amountMinor = inv.amountMinor,
                    currency = inv.currency,
                    epochDay = contributionDay,
                    note = "Aporte: ${inv.name}",
                    kind = "EXPENSE",
                    investmentId = inv.id,
                )
            }
            if (due.next != next) {
                investRepo.updateNextContribution(inv.id, due.next)
            }
        }
    }

    /** Aporte extra (fuera del calendario periódico): gasto vinculado a la inversión, desde [accountId]
     * (por defecto la cuenta del fondo). Una cuenta inexistente o de otra moneda cae a la del fondo. */
    fun contribute(id: Long, amountMinor: Long, accountId: Long? = null, epochDay: Long = todayEpochDay()) {
        val inv = investRepo.byId(id) ?: return
        val accounts = accountRepo.all()
        val account = accounts.firstOrNull { it.id == accountId && it.currency == inv.currency }
            ?: accounts.firstOrNull { it.id == inv.accountId } ?: return
        txRepo.add(
            accountId = account.id,
            categoryId = inv.categoryId,
            amountMinor = amountMinor,
            currency = account.currency,
            epochDay = epochDay,
            note = "Aporte: ${inv.name}",
            kind = "EXPENSE",
            investmentId = inv.id,
        )
    }

    /**
     * Retiro parcial: devuelve el monto a la cuenta como ingreso vinculado a la inversión (queda en
     * su historial) sin cerrarla. La nota "Retiro: <nombre>" se conserva porque
     * `reassignWithdrawalCategory` la usa para los retiros antiguos que no tenían `investmentId`.
     */
    fun withdraw(id: Long, amountMinor: Long, epochDay: Long = todayEpochDay()) {
        val inv = investRepo.byId(id) ?: return
        val account = accountRepo.all().firstOrNull { it.id == inv.accountId } ?: return
        txRepo.add(
            accountId = account.id,
            categoryId = inv.categoryId,
            amountMinor = amountMinor,
            currency = account.currency,
            epochDay = epochDay,
            note = "Retiro: ${inv.name}",
            kind = "INCOME",
            investmentId = inv.id,
        )
    }

    /**
     * Cierra la inversión: registra el retiro final (0 = ya se retiró todo antes) y calcula el
     * rendimiento sobre todo su historial — Σ retiros − Σ aportes, leído directo de la BD para evitar
     * un off-by-one justo después de postear. `withdrawnAmountMinor` guarda el total retirado, así
     * que aportado = retirado − rendimiento sigue valiendo también para las cerradas antiguas.
     */
    fun closeInvestment(id: Long, finalWithdrawalMinor: Long, closedDate: Long = todayEpochDay()) {
        if (finalWithdrawalMinor > 0) withdraw(id, finalWithdrawalMinor)
        val totals = fundTotals(txRepo.contributionsForInvestment(id))
        investRepo.close(id, totals.withdrawnMinor, closedDate, yieldMinor(totals.withdrawnMinor, totals.contributedMinor))
    }

    /** Reabre una cerrada. Si es periódica, el próximo aporte queda en la siguiente fecha posterior a
     * hoy (no se postea uno inmediato al reabrir). Deshacer = `closeInvestment(id, 0, fechaOriginal)`, que
     * recalcula lo mismo desde los movimientos. */
    fun reopen(id: Long) {
        val inv = investRepo.byId(id) ?: return
        val frequency = inv.frequency
        val interval = inv.intervalCount
        val next = if (inv.periodic == 1L && frequency != null && interval != null) {
            RecurrenceSchedule.nextOccurrenceAfter(todayEpochDay(), frequency, interval)
        } else {
            null
        }
        investRepo.reopen(id, next)
    }

    /** Borra un aporte o retiro del historial (corrección de un registro manual mal ingresado). */
    fun deleteMovement(txId: Long) = txRepo.delete(txId)

    fun investmentById(id: Long): Investment? = investRepo.byId(id)

    /** Cambia nombre/monto/cuenta/periodicidad/recurrencia hacia adelante — no reescribe aportes ya
     * posteados. Si se activa periodicidad sobre una inversión que no tenía próxima fecha de aporte
     * (puntual → periódica), hay que sembrarla o `postDueContributions` la ignora para siempre. */
    fun updateInvestment(id: Long, name: String, amountMinor: Long, accountId: Long, periodic: Boolean, frequency: String?, intervalCount: Long?) {
        val account = accountRepo.all().firstOrNull { it.id == accountId } ?: return
        val current = investRepo.byId(id) ?: return
        investRepo.update(
            id = id,
            name = name,
            amountMinor = amountMinor,
            currency = account.currency,
            accountId = account.id,
            periodic = periodic,
            frequency = if (periodic) frequency else null,
            intervalCount = if (periodic) intervalCount else null,
        )
        if (periodic && current.nextContributionDate == null) {
            investRepo.updateNextContribution(id, todayEpochDay())
        }
    }

    fun deleteInvestment(id: Long) = investRepo.delete(id)

    /** Archiva (abierta o cerrada). Una periódica archivada no recibe aportes automáticos; al
     * restaurarla, el catch-up registra los que faltaron. */
    fun archive(id: Long) = investRepo.archive(id)

    fun unarchive(id: Long) = investRepo.unarchive(id)

    /** Borrado definitivo desde Archivadas: la inversión y todos sus aportes/retiros (mismo criterio
     * que AccountsViewModel.delete con los movimientos de la cuenta). */
    fun deletePermanently(id: Long) {
        txRepo.deleteByInvestment(id)
        investRepo.delete(id)
    }

    private fun todayEpochDay(): Long = Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays()

    /** [initialMinor] = aporte más antiguo; [contributedMinor] = total aportado (incluye el inicial). */
    data class FundTotals(val initialMinor: Long, val contributedMinor: Long, val withdrawnMinor: Long)

    companion object {
        /** Agrupación pura y testeable (mismo criterio que AccountsViewModel.groupByAccount). */
        fun groupByInvestment(txs: List<TransactionRow>): Map<Long, List<TransactionRow>> = txs.filter { it.investmentId != null }
            .groupBy { it.investmentId!! }

        /** Movimientos de una inversión → aportado (EXPENSE) y retirado acumulado (INCOME). */
        fun fundTotals(txs: List<TransactionRow>): FundTotals = FundTotals(
            initialMinor = txs.filter { it.kind == "EXPENSE" }.minWithOrNull(compareBy({ it.date }, { it.id }))?.amountMinor ?: 0L,
            contributedMinor = txs.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor },
            withdrawnMinor = txs.filter { it.kind == "INCOME" }.sumOf { it.amountMinor },
        )

        /** Rendimiento total: retirado − aportado. Puede ser negativo (pérdida). */
        fun yieldMinor(withdrawnAmountMinor: Long, contributedAmountMinor: Long): Long = withdrawnAmountMinor - contributedAmountMinor
    }
}
