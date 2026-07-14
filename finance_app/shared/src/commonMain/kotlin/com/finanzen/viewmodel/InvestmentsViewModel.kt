package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.AccountRepository
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
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class InvestmentsViewModel(
    private val investRepo: InvestmentRepository,
    private val accountRepo: AccountRepository,
    private val txRepo: TransactionRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    val openInvestments: StateFlow<List<Investment>> =
        investRepo.observeOpen().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val closedInvestments: StateFlow<List<Investment>> =
        investRepo.observeClosed().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val accounts: StateFlow<List<Account>> =
        accountRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Aportes de cada inversión, agrupados desde el flujo ya suscrito por Movimientos — igual que
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
        val id = investRepo.add(
            name = name,
            amountMinor = amountMinor,
            currency = account.currency,
            accountId = account.id,
            categoryId = null,
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
                categoryId = null,
                amountMinor = amountMinor,
                currency = account.currency,
                epochDay = startEpochDay,
                note = name,
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
                    note = inv.name,
                    kind = "EXPENSE",
                    investmentId = inv.id,
                )
            }
            if (due.next != next) {
                investRepo.updateNextContribution(inv.id, due.next)
            }
        }
    }

    /**
     * Retira/cierra la inversión: calcula el rendimiento (retirado − aportado, leído directo de la
     * BD para evitar un off-by-one justo después de postear un aporte), devuelve el monto retirado a
     * la cuenta como ingreso (sin `investmentId`, para no aparecer en el historial de aportes), y
     * marca la inversión como cerrada con ese rendimiento.
     */
    fun closeInvestment(id: Long, withdrawnAmountMinor: Long) {
        val inv = investRepo.byId(id) ?: return
        val account = accountRepo.all().firstOrNull { it.id == inv.accountId } ?: return
        val contributed = txRepo.contributionsForInvestment(id).sumOf { it.amountMinor }
        val yieldValue = yieldMinor(withdrawnAmountMinor, contributed)
        txRepo.add(
            accountId = account.id,
            categoryId = null,
            amountMinor = withdrawnAmountMinor,
            currency = account.currency,
            epochDay = todayEpochDay(),
            note = "Retiro: ${inv.name}",
            kind = "INCOME",
        )
        investRepo.close(id, withdrawnAmountMinor, todayEpochDay(), yieldValue)
    }

    fun deleteInvestment(id: Long) = investRepo.delete(id)

    private fun todayEpochDay(): Long = Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays().toLong()

    companion object {
        /** Agrupación pura y testeable (mismo criterio que AccountsViewModel.groupByAccount). */
        fun groupByInvestment(txs: List<TransactionRow>): Map<Long, List<TransactionRow>> = txs.filter { it.investmentId != null }
            .groupBy { it.investmentId!! }

        /** Rendimiento total: retirado − aportado. Puede ser negativo (pérdida). */
        fun yieldMinor(withdrawnAmountMinor: Long, contributedAmountMinor: Long): Long = withdrawnAmountMinor - contributedAmountMinor
    }
}
