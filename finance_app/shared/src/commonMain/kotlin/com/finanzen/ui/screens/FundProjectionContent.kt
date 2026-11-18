package com.finanzen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.domain.ComparedFund
import com.finanzen.domain.FundPlan
import com.finanzen.domain.FundProjection
import com.finanzen.domain.FundProjector
import com.finanzen.domain.ProjectionInput
import com.finanzen.domain.Scenario
import com.finanzen.ui.components.AxisTick
import com.finanzen.ui.components.BarSegment
import com.finanzen.ui.components.BentoTileSize
import com.finanzen.ui.components.ChartSeries
import com.finanzen.ui.components.DataTable
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.GroupCard
import com.finanzen.ui.components.LineChart
import com.finanzen.ui.components.ListRow
import com.finanzen.ui.components.MoneyText
import com.finanzen.ui.components.Overline
import com.finanzen.ui.components.RowDivider
import com.finanzen.ui.components.SageButton
import com.finanzen.ui.components.SageButtonKind
import com.finanzen.ui.components.ScreenList
import com.finanzen.ui.components.SectionHeader
import com.finanzen.ui.components.StackedBar
import com.finanzen.ui.components.niceTicks
import com.finanzen.ui.format.formatDecimal
import com.finanzen.ui.format.formatPercent
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSageType
import kotlin.math.max
import kotlin.math.pow

/** Un escenario proyectado: cuál es, con qué entradas y su serie. */
private data class ScenarioResult(val scenario: Scenario, val input: ProjectionInput, val projection: FundProjection)

/** Escala de los ejes en unidades enteras de la moneda: millones, miles o unidades. */
private data class AxisScale(val divisor: Double, val unit: String)

@Composable
internal fun FundProjectionContent(plan: FundPlan, isSaved: Boolean, onCompare: () -> Unit, onSave: () -> Unit) {
    val results = remember(plan) { plan.scenarios().map { (s, input) -> ScenarioResult(s, input, FundProjector.project(input)) } }
    val main = results.first { it.scenario == plan.mainScenario().first }
    val feeCost = remember(plan) { FundProjector.feeCostMinor(main.input) }
    ScreenList {
        item(key = "hero") { ProjectionHero(plan, main) }
        item(key = "chart") { ProjectionChartCard(plan, results) }
        if (plan.adminFee > 0 || plan.contributionFee > 0) item(key = "fees") { FeeCostCard(plan, main.projection, feeCost) }
        item(key = "yearly") { YearlyTable(plan, main.projection) }
        item(key = "actions") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SageButton("Comparar con otro fondo", onClick = onCompare, kind = SageButtonKind.Secondary, modifier = Modifier.fillMaxWidth())
                SageButton(
                    if (isSaved) "Simulación guardada" else "Guardar simulación",
                    onClick = onSave,
                    enabled = !isSaved,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ProjectionHero(plan: FundPlan, main: ScenarioResult) {
    val scenarioText = if (main.scenario == Scenario.FIXED) "tasa fija del ${formatPercent(main.input.annualRate)}" else "escenario moderado"
    FinanceCard(Modifier.fillMaxWidth(), size = BentoTileSize.Hero) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Overline("En ${termLabel(plan.months)} tendrías · $scenarioText")
            MoneyText(main.projection.finalBalanceMinor, plan.currency, style = LocalSageType.current.amountHero)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Stat("Aportado", Modifier.weight(1f)) { MoneyText(main.projection.contributedMinor, plan.currency, style = LocalSageType.current.amountMd) }
                Stat("Rendimiento", Modifier.weight(1f)) {
                    MoneyText(main.projection.yieldMinor, plan.currency, style = LocalSageType.current.amountMd, signed = true)
                }
            }
        }
    }
}

@Composable
private fun ProjectionChartCard(plan: FundPlan, results: List<ScenarioResult>) {
    val chart = LocalFinanceColors.current.chart
    val decimals = currencyDecimals(plan.currency)
    val maxMajor = results.maxOf { it.projection.finalBalanceMinor } / 10.0.pow(decimals)
    val scale = axisScale(maxMajor)
    val toAxis = { minor: Long -> minor / 10.0.pow(decimals) / scale.divisor }
    val reference = results.first().projection
    val series = results.map { r ->
        ChartSeries(r.scenario.shortLabel, r.projection.points.map { toAxis(it.balanceMinor) }, scenarioColor(r.scenario, chart))
    } + ChartSeries("Aportado", reference.points.map { toAxis(it.contributedMinor) }, chart.last(), dashed = true)
    val yTicks = niceTicks(maxMajor / scale.divisor).map { AxisTick(it, formatTick(it)) }
    val money = LocalMoneyFormat.current
    val description = "Saldo proyectado a ${termLabel(plan.months)}: " +
        results.joinToString(", ") { "${it.scenario.label.lowercase()} ${money.format(it.projection.finalBalanceMinor, plan.currency)}" } +
        ", frente a ${money.format(reference.contributedMinor, plan.currency)} aportados."
    FinanceCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CardHead("Saldo proyectado", "${scale.unit} ${plan.currency}".trim())
            LineChart(series = series, xTicks = timeTicks(plan.months), yTicks = yTicks, contentDescription = description)
            Caption(
                (if (plan.months >= MONTHS_IN_YEAR * 2) "Eje horizontal: años. " else "Eje horizontal: meses. ") +
                    "Línea punteada: lo que aportas, sin rendimientos.",
            )
            Column {
                results.sortedByDescending { it.projection.finalBalanceMinor }.forEachIndexed { i, r ->
                    if (i > 0) RowDivider()
                    ScenarioRow(r, plan.currency, scenarioColor(r.scenario, chart))
                }
            }
        }
    }
}

@Composable
private fun ScenarioRow(r: ScenarioResult, currency: String, color: Color) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(r.scenario.label, style = LocalSageType.current.body, fontWeight = FontWeight.SemiBold, color = color)
            Caption("${formatPercent(r.input.annualRate)} anual")
        }
        MoneyText(r.projection.finalBalanceMinor, currency, style = LocalSageType.current.amountMd)
    }
}

@Composable
private fun FeeCostCard(plan: FundPlan, projection: FundProjection, feeCostMinor: Long) {
    val money = LocalMoneyFormat.current
    val fees = listOfNotNull(
        plan.adminFee.takeIf { it > 0 }?.let { "la comisión del ${formatPercent(it)} anual" },
        plan.contributionFee.takeIf { it > 0 }?.let { "la comisión del ${formatPercent(it)} por aporte" },
    ).joinToString(" y ")
    val withoutFees = projection.finalBalanceMinor + feeCostMinor
    val grossYield = projection.yieldMinor + feeCostMinor
    val share = if (grossYield > 0) " Las comisiones se llevan el ${formatPercent(feeCostMinor.toDouble() / grossYield, 0)} de tu rendimiento en ${termLabel(plan.months)}." else ""
    FinanceCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Lo que cuestan las comisiones", style = LocalSageType.current.titleCard)
            MoneyText(-feeCostMinor, plan.currency, style = LocalSageType.current.amountLg, colorOverride = LocalFinanceColors.current.expense)
            Text(
                "Sin $fees tendrías ${money.format(withoutFees, plan.currency)}.$share",
                style = LocalSageType.current.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun YearlyTable(plan: FundPlan, projection: FundProjection) {
    val money = LocalMoneyFormat.current
    val yearly = projection.yearly()
    var showAll by rememberSaveable { mutableStateOf(false) }
    val highlighted = HIGHLIGHT_YEARS.map { it * MONTHS_IN_YEAR } + yearly.last().month
    val visible = if (showAll || yearly.size <= COLLAPSED_ROWS) yearly else yearly.filter { it.month in highlighted }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(if (plan.months >= MONTHS_IN_YEAR) "Año por año" else "Al final del plazo")
        DataTable(
            header = listOf("Año", "Aportado", "Saldo"),
            rows = visible.map { listOf(yearLabel(it.month), money.format(it.contributedMinor, plan.currency), money.format(it.balanceMinor, plan.currency)) },
        )
        if (yearly.size > COLLAPSED_ROWS) {
            SageButton(
                if (showAll) "Ver menos" else "Ver los ${yearly.size} años",
                onClick = { showAll = !showAll },
                kind = SageButtonKind.Ghost,
                small = true,
            )
        }
    }
}

/** Comparación: primero se elige otra simulación guardada; luego se muestran las dos lado a lado. */
@Composable
internal fun FundCompareContent(plan: FundPlan, others: List<FundPlan>) {
    var otherId by rememberSaveable { mutableStateOf<Long?>(null) }
    val other = others.firstOrNull { it.id == otherId }
    if (other == null) {
        ComparePicker(plan, others) { otherId = it }
    } else {
        CompareResult(plan, other) { otherId = null }
    }
}

@Composable
private fun ComparePicker(plan: FundPlan, others: List<FundPlan>, onPick: (Long) -> Unit) {
    ScreenList {
        item(key = "intro") { Caption("Elige con qué simulación guardada comparar \"${plan.name}\". Se usa el escenario principal de cada una.") }
        if (others.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = LucideIcons.TrendingUp,
                    title = "No hay otra simulación guardada",
                    subtitle = "Guarda otra simulación, con otra rentabilidad o comisión, para compararlas.",
                )
            }
        } else {
            item(key = "list") {
                GroupCard {
                    others.forEachIndexed { i, candidate ->
                        if (i > 0) RowDivider()
                        ListRow(
                            title = candidate.name,
                            subtitle = "${formatPercent(candidate.mainRate)} anual · comisión ${formatPercent(candidate.adminFee)} · ${termLabel(candidate.months)}",
                            onClick = { candidate.id?.let(onPick) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareResult(plan: FundPlan, other: FundPlan, onChange: () -> Unit) {
    val funds = remember(plan, other) {
        listOf(plan, other).map { p ->
            val input = p.mainScenario().second
            ComparedFund(p.name, input, FundProjector.project(input), FundProjector.feeCostMinor(input))
        }
    }
    ScreenList {
        item(key = "summary") { CompareSummary(plan, other, funds) }
        item(key = "bars") { CompareBars(plan.currency, funds) }
        item(key = "table") { CompareTable(plan, funds) }
        item(key = "change") {
            SageButton("Cambiar el fondo a comparar", onClick = onChange, kind = SageButtonKind.Secondary, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CompareSummary(plan: FundPlan, other: FundPlan, funds: List<ComparedFund>) {
    val money = LocalMoneyFormat.current
    val winner = funds.maxBy { it.projection.finalBalanceMinor }
    val loser = funds.minBy { it.projection.finalBalanceMinor }
    val diff = winner.projection.finalBalanceMinor - loser.projection.finalBalanceMinor
    val sameContributions = plan.initialMinor == other.initialMinor && plan.monthlyMinor == other.monthlyMinor && plan.months == other.months
    FinanceCard(Modifier.fillMaxWidth(), size = BentoTileSize.Hero) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Overline(if (sameContributions) "Mismos aportes, ${termLabel(plan.months)}" else "Cada uno con sus aportes y plazo")
            Text(
                if (diff == 0L) "Los dos terminan igual" else "${winner.name} te deja ${money.format(diff, plan.currency)} más",
                style = LocalSageType.current.titleSection,
            )
            if (diff > 0 && loser.input.annualRate > winner.input.annualRate) {
                Text(
                    "${loser.name} promete más rentabilidad, pero su comisión se lleva la diferencia.",
                    style = LocalSageType.current.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CompareBars(currency: String, funds: List<ComparedFund>) {
    val finance = LocalFinanceColors.current
    val decimals = currencyDecimals(currency)
    val totals = funds.map { it.projection.contributedMinor + max(it.projection.yieldMinor, 0) + it.feeCostMinor }
    val maxTotal = totals.max().coerceAtLeast(1).toDouble()
    val scale = axisScale(maxTotal / 10.0.pow(decimals))
    val toAxis = { minor: Long -> formatDecimal(minor / 10.0.pow(decimals) / scale.divisor) }
    FinanceCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CardHead("A dónde va el dinero", "${scale.unit} $currency".trim())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendItem("Aportado", finance.chart.last())
                LegendItem("Rendimiento para ti", finance.income)
                LegendItem("Comisiones", finance.expense)
            }
            funds.forEachIndexed { i, f ->
                val p = f.projection
                val gain = max(p.yieldMinor, 0)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(f.name, Modifier.weight(1f), style = LocalSageType.current.label, fontWeight = FontWeight.SemiBold)
                        Caption("${formatPercent(f.input.annualRate)} anual · comisión ${formatPercent(f.input.adminFee)}")
                    }
                    // Cada barra mide su total frente al mayor de los dos, para comparar a ojo.
                    StackedBar(
                        segments = listOf(
                            BarSegment(p.contributedMinor.toFloat(), finance.chart.last()),
                            BarSegment(gain.toFloat(), finance.income),
                            BarSegment(f.feeCostMinor.toFloat(), finance.expense),
                        ),
                        modifier = Modifier.fillMaxWidth((totals[i] / maxTotal).toFloat()),
                        contentDescription = "${f.name}: ${toAxis(p.contributedMinor)} aportado, ${toAxis(gain)} de rendimiento para ti y " +
                            "${toAxis(f.feeCostMinor)} en comisiones, en ${scale.unit.lowercase().ifEmpty { "unidades" }} de $currency.",
                        height = 24.dp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Caption(toAxis(p.contributedMinor))
                        Caption("+ ${toAxis(gain)}", finance.income)
                        Caption("− ${toAxis(f.feeCostMinor)}", finance.expense)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareTable(plan: FundPlan, funds: List<ComparedFund>) {
    val money = LocalMoneyFormat.current
    val m = { minor: Long -> money.format(minor, plan.currency) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Cifra por cifra")
        DataTable(
            header = listOf("En ${termLabel(funds.first().input.months)}") + funds.map { it.name },
            rows = listOf(
                listOf("Rentabilidad") + funds.map { formatPercent(it.input.annualRate) },
                listOf("Comisión anual") + funds.map { formatPercent(it.input.adminFee) },
                listOf("Aportado") + funds.map { m(it.projection.contributedMinor) },
                listOf("Rendimiento") + funds.map { m(it.projection.yieldMinor) },
                listOf("Comisiones") + funds.map { m(it.feeCostMinor) },
            ),
            footer = listOf("Saldo final") + funds.map { m(it.projection.finalBalanceMinor) },
        )
        Caption("Escenario moderado (o tasa fija) de cada simulación. Son estimaciones, no rentabilidades garantizadas.")
    }
}

@Composable
private fun CardHead(title: String, unit: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = LocalSageType.current.titleCard)
        Caption(unit)
    }
}

@Composable
private fun Stat(label: String, modifier: Modifier, value: @Composable () -> Unit) {
    Column(modifier) {
        Caption(label)
        value()
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(10.dp).background(color, MaterialTheme.shapes.extraSmall))
        Caption(label)
    }
}

@Composable
private fun Caption(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, style = LocalSageType.current.caption, color = color)
}

@Composable
private fun currencyDecimals(currency: String): Int = LocalMoneyFormat.current.currencies[currency]?.decimals?.toInt() ?: 2

private val Scenario.shortLabel: String get() = if (this == Scenario.CONSERVATIVE) "Conserv." else label

private fun scenarioColor(scenario: Scenario, chart: List<Color>): Color = when (scenario) {
    Scenario.CONSERVATIVE -> chart[0]
    Scenario.AGGRESSIVE -> chart[1]
    Scenario.MODERATE, Scenario.FIXED -> chart[2]
}

private fun axisScale(maxMajor: Double): AxisScale = when {
    maxMajor >= MILLION -> AxisScale(MILLION, "Millones de")
    maxMajor >= THOUSAND -> AxisScale(THOUSAND, "Miles de")
    else -> AxisScale(1.0, "")
}

private fun formatTick(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else formatDecimal(value)

/** Marcas del eje x: años (cada 1, 2, 5 o 10 para no pasar de ~6 marcas) o meses si el plazo es corto. */
private fun timeTicks(months: Int): List<AxisTick> {
    if (months < MONTHS_IN_YEAR * 2) {
        val step = if (months <= MONTHS_IN_YEAR) QUARTER else HALF_YEAR
        return (0..months step step).map { AxisTick(it.toDouble(), it.toString()) }
    }
    val years = months / MONTHS_IN_YEAR
    val step = YEAR_STEPS.first { years / it <= MAX_X_TICKS }
    return (0..years step step).map { AxisTick((it * MONTHS_IN_YEAR).toDouble(), it.toString()) }
}

private fun termLabel(months: Int): String = when {
    months % MONTHS_IN_YEAR == 0 -> (months / MONTHS_IN_YEAR).let { if (it == 1) "1 año" else "$it años" }
    else -> if (months == 1) "1 mes" else "$months meses"
}

private fun yearLabel(month: Int): String = if (month % MONTHS_IN_YEAR == 0) (month / MONTHS_IN_YEAR).toString() else "${month / MONTHS_IN_YEAR} a ${month % MONTHS_IN_YEAR} m"

private const val MILLION = 1_000_000.0
private const val THOUSAND = 1_000.0
private const val COLLAPSED_ROWS = 4
private const val MAX_X_TICKS = 6
private const val QUARTER = 3
private const val HALF_YEAR = 6

/** Años que se muestran en la tabla plegada (más el último). */
@Suppress("MagicNumber")
private val HIGHLIGHT_YEARS = listOf(1, 3, 5)

@Suppress("MagicNumber")
private val YEAR_STEPS = listOf(1, 2, 5, 10, 20)
