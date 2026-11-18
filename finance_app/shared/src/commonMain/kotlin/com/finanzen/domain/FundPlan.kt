package com.finanzen.domain

/**
 * Una simulación de fondo tal como la llena el usuario: aportes, plazo, rentabilidad (tres escenarios
 * o tasa fija) y comisiones. Tasas como fracción efectiva anual. [id] nulo = aún no guardada.
 */
data class FundPlan(
    val id: Long? = null,
    val name: String,
    val currency: String,
    val initialMinor: Long,
    val monthlyMinor: Long,
    val months: Int,
    val rateMode: RateMode = RateMode.SCENARIOS,
    val conservativeRate: Double = DEFAULT_CONSERVATIVE,
    val moderateRate: Double = DEFAULT_MODERATE,
    val aggressiveRate: Double = DEFAULT_AGGRESSIVE,
    val fixedRate: Double = DEFAULT_MODERATE,
    val adminFee: Double = 0.0,
    val contributionFee: Double = 0.0,
) {
    /** Escenarios a proyectar, del más prudente al más optimista (uno solo con tasa fija). */
    fun scenarios(): List<Pair<Scenario, ProjectionInput>> = when (rateMode) {
        RateMode.FIXED -> listOf(Scenario.FIXED to inputFor(fixedRate))

        RateMode.SCENARIOS -> listOf(
            Scenario.CONSERVATIVE to inputFor(conservativeRate),
            Scenario.MODERATE to inputFor(moderateRate),
            Scenario.AGGRESSIVE to inputFor(aggressiveRate),
        )
    }

    /** Escenario de referencia: el moderado con tres escenarios, o la tasa fija. */
    fun mainScenario(): Pair<Scenario, ProjectionInput> = scenarios().firstOrNull { it.first == Scenario.MODERATE || it.first == Scenario.FIXED } ?: scenarios().first()

    /** Tasa anual del escenario principal (la que se usa al comparar fondos). */
    val mainRate: Double get() = mainScenario().second.annualRate

    /** Errores de las entradas de todos los escenarios, sin repetir. Vacía si la simulación es válida. */
    fun validate(): List<String> = buildList {
        if (name.isBlank()) add("Ponle un nombre a la simulación.")
        scenarios().flatMap { FundProjector.validate(it.second) }.distinct().forEach(::add)
    }

    private fun inputFor(rate: Double) = ProjectionInput(initialMinor, monthlyMinor, months, rate, adminFee, contributionFee)

    companion object {
        const val DEFAULT_CONSERVATIVE = 0.06
        const val DEFAULT_MODERATE = 0.09
        const val DEFAULT_AGGRESSIVE = 0.12
    }
}
