package com.finanzen.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.finanzen.db.FinanzenDb
import com.finanzen.db.FundSimulation
import com.finanzen.domain.FundPlan
import com.finanzen.domain.RateMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Simulaciones de fondos guardadas. La conversión fila ↔ [FundPlan] vive solo aquí. */
class FundSimulationRepository(private val db: FinanzenDb) {

    fun observeAll(): Flow<List<FundPlan>> = db.fundSimulationQueries.selectAll().asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map { it.toPlan() } }

    fun byId(id: Long): FundPlan? = db.fundSimulationQueries.selectById(id).executeAsOneOrNull()?.toPlan()

    /** Guarda una simulación nueva (devuelve su id) o actualiza la existente si [FundPlan.id] no es nulo. */
    fun save(plan: FundPlan, todayEpochDay: Long): Long {
        val q = db.fundSimulationQueries
        val id = plan.id
        if (id != null) {
            q.update(
                plan.name.trim(), plan.initialMinor, plan.monthlyMinor, plan.months.toLong(), plan.rateMode.name,
                plan.conservativeRate, plan.moderateRate, plan.aggressiveRate, plan.fixedRate, plan.adminFee, plan.contributionFee, id,
            )
            return id
        }
        return db.transactionWithResult {
            q.insert(
                plan.name.trim(), plan.currency, plan.initialMinor, plan.monthlyMinor, plan.months.toLong(), plan.rateMode.name,
                plan.conservativeRate, plan.moderateRate, plan.aggressiveRate, plan.fixedRate, plan.adminFee, plan.contributionFee, todayEpochDay,
            )
            q.lastInsertRowId().executeAsOne()
        }
    }

    fun delete(id: Long) = db.fundSimulationQueries.delete(id)

    private fun FundSimulation.toPlan() = FundPlan(
        id = id,
        name = name,
        currency = currency,
        initialMinor = initialMinor,
        monthlyMinor = monthlyMinor,
        months = months.toInt(),
        rateMode = RateMode.fromKey(rateMode),
        conservativeRate = rateConservative,
        moderateRate = rateModerate,
        aggressiveRate = rateAggressive,
        fixedRate = rateFixed,
        adminFee = adminFee,
        contributionFee = contributionFee,
    )
}
