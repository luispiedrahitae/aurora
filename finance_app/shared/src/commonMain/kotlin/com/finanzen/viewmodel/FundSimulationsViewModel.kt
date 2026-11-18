package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.FundSimulationRepository
import com.finanzen.data.SettingsRepository
import com.finanzen.domain.FundPlan
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** Simulaciones de fondos: lista guardada, alta/edición y borrado. El cálculo vive en domain/. */
class FundSimulationsViewModel(
    private val repo: FundSimulationRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    val simulations: StateFlow<List<FundPlan>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val baseCurrency: String get() = settings.baseCurrency()

    fun byId(id: Long): FundPlan? = repo.byId(id)

    /** Plan vacío con valores de partida razonables para el formulario. */
    fun newPlan(): FundPlan = FundPlan(name = "", currency = baseCurrency, initialMinor = 0, monthlyMinor = 0, months = DEFAULT_MONTHS)

    /** Guarda y devuelve el id; nulo si la simulación no es válida. */
    fun save(plan: FundPlan): Long? {
        if (plan.validate().isNotEmpty()) return null
        return repo.save(plan, Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays())
    }

    fun delete(id: Long) = repo.delete(id)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val DEFAULT_MONTHS = 120
    }
}
