package com.finanzen.domain

import kotlin.math.pow
import kotlin.math.roundToLong

/** Cómo se expresa la rentabilidad esperada de una simulación. */
enum class RateMode {
    /** Tres escenarios: conservador, moderado y agresivo. */
    SCENARIOS,

    /** Una sola tasa fija. */
    FIXED,
    ;

    companion object {
        fun fromKey(key: String): RateMode = entries.firstOrNull { it.name == key } ?: SCENARIOS
    }
}

/** Escenario de rentabilidad. [FIXED] se usa cuando la simulación es de tasa fija. */
enum class Scenario(val label: String) {
    CONSERVATIVE("Conservador"),
    MODERATE("Moderado"),
    AGGRESSIVE("Agresivo"),
    FIXED("Tasa fija"),
}

/**
 * Entradas de una proyección. Dinero en unidades mínimas; tasas como fracción efectiva anual
 * (0.09 = 9 %). [adminFee] es la comisión de administración anual sobre el saldo; [contributionFee]
 * la que se descuenta de cada aporte (incluido el inicial).
 */
data class ProjectionInput(
    val initialMinor: Long,
    val monthlyMinor: Long,
    val months: Int,
    val annualRate: Double,
    val adminFee: Double = 0.0,
    val contributionFee: Double = 0.0,
)

/** Estado al final de un mes: aportado acumulado (bruto), saldo y comisiones pagadas acumuladas. */
data class ProjectionPoint(
    val month: Int,
    val contributedMinor: Long,
    val balanceMinor: Long,
    val feesPaidMinor: Long,
)

/** Resultado de una proyección: la serie mes a mes (el mes 0 es el aporte inicial) y sus totales. */
data class FundProjection(val points: List<ProjectionPoint>) {
    private val last: ProjectionPoint get() = points.last()
    val finalBalanceMinor: Long get() = last.balanceMinor
    val contributedMinor: Long get() = last.contributedMinor
    val feesPaidMinor: Long get() = last.feesPaidMinor

    /** Lo que ganaste por encima de lo aportado (negativo si el fondo pierde). */
    val yieldMinor: Long get() = finalBalanceMinor - contributedMinor

    /** Un punto por año cumplido, más el último mes si el plazo no es un número entero de años. */
    fun yearly(): List<ProjectionPoint> = points.filter { it.month > 0 && (it.month % MONTHS_PER_YEAR == 0 || it.month == last.month) }
}

/** Fila de una comparación: nombre, entradas y su proyección, con lo que cuestan las comisiones. */
data class ComparedFund(val name: String, val input: ProjectionInput, val projection: FundProjection, val feeCostMinor: Long)

/** Límites razonables de las entradas; fuera de ellos la simulación no se calcula. */
object ProjectionLimits {
    const val MAX_MONTHS = 600 // 50 años
    const val MIN_RATE = -0.5
    const val MAX_RATE = 1.0
    const val MAX_FEE = 0.2
}

/**
 * Proyección mes a mes de un fondo con aporte al inicio de cada mes:
 *
 * - rendimiento mensual `m = (1 + r)^(1/12) − 1` (equivalente a la tasa efectiva anual r);
 * - comisión de administración mensual `fₘ = 1 − (1 − f)^(1/12)`, cobrada cada mes sobre el saldo
 *   (equivale a f al año, como la cobran los fondos colectivos);
 * - cada mes: `saldo = (saldo + aporte·(1 − c)) · (1 + m) · (1 − fₘ)`.
 *
 * El saldo se lleva en Double para no acumular redondeos; cada punto de la serie se redondea al
 * entero más cercano en unidades mínimas.
 */
object FundProjector {

    fun project(input: ProjectionInput): FundProjection {
        require(validate(input).isEmpty()) { "Entradas inválidas: ${validate(input)}" }
        val monthlyRate = (1 + input.annualRate).pow(1.0 / MONTHS_PER_YEAR) - 1
        val monthlyFee = 1 - (1 - input.adminFee).pow(1.0 / MONTHS_PER_YEAR)

        var balance = input.initialMinor * (1 - input.contributionFee)
        var contributed = input.initialMinor.toDouble()
        var fees = input.initialMinor * input.contributionFee
        val points = ArrayList<ProjectionPoint>(input.months + 1)
        points += point(0, contributed, balance, fees)
        for (month in 1..input.months) {
            val contributionFee = input.monthlyMinor * input.contributionFee
            balance += input.monthlyMinor - contributionFee
            contributed += input.monthlyMinor
            balance *= 1 + monthlyRate
            val adminFee = balance * monthlyFee
            balance -= adminFee
            fees += contributionFee + adminFee
            points += point(month, contributed, balance, fees)
        }
        return FundProjection(points)
    }

    /**
     * Lo que cuestan las comisiones: el saldo final que tendrías sin ninguna comisión menos el que
     * tienes con ellas. Es mayor que la suma de comisiones pagadas porque incluye el rendimiento que
     * ese dinero habría generado.
     */
    fun feeCostMinor(input: ProjectionInput): Long = project(input.copy(adminFee = 0.0, contributionFee = 0.0)).finalBalanceMinor - project(input).finalBalanceMinor

    /** Proyecta varios fondos y los ordena de mayor a menor saldo final. */
    fun compare(funds: List<Pair<String, ProjectionInput>>): List<ComparedFund> = funds
        .map { (name, input) -> ComparedFund(name, input, project(input), feeCostMinor(input)) }
        .sortedByDescending { it.projection.finalBalanceMinor }

    /** Mensajes de error de las entradas; vacía si son válidas. */
    fun validate(input: ProjectionInput): List<String> = buildList {
        if (input.initialMinor < 0 || input.monthlyMinor < 0) add("Los aportes no pueden ser negativos.")
        if (input.initialMinor == 0L && input.monthlyMinor == 0L) add("Pon un aporte inicial o un aporte mensual.")
        if (input.months !in 1..ProjectionLimits.MAX_MONTHS) add("El plazo debe ser de 1 mes a 50 años.")
        if (input.annualRate !in ProjectionLimits.MIN_RATE..ProjectionLimits.MAX_RATE) add("La rentabilidad debe estar entre −50 % y 100 % anual.")
        if (input.adminFee !in 0.0..ProjectionLimits.MAX_FEE) add("La comisión de administración debe estar entre 0 % y 20 %.")
        if (input.contributionFee !in 0.0..ProjectionLimits.MAX_FEE) add("La comisión por aporte debe estar entre 0 % y 20 %.")
    }

    private fun point(month: Int, contributed: Double, balance: Double, fees: Double) = ProjectionPoint(month, contributed.roundToLong(), balance.roundToLong(), fees.roundToLong())
}

private const val MONTHS_PER_YEAR = 12
