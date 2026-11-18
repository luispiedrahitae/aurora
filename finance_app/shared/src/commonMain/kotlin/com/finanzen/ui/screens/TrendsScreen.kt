package com.finanzen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.AxisTick
import com.finanzen.ui.components.BadgeTone
import com.finanzen.ui.components.BarChart
import com.finanzen.ui.components.BarDatum
import com.finanzen.ui.components.BarSegment
import com.finanzen.ui.components.ConfirmDialog
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.LoadingState
import com.finanzen.ui.components.MagnitudeBar
import com.finanzen.ui.components.MoneyText
import com.finanzen.ui.components.MonthSelector
import com.finanzen.ui.components.Overline
import com.finanzen.ui.components.SageBadge
import com.finanzen.ui.components.ScreenList
import com.finanzen.ui.components.SegmentedControl
import com.finanzen.ui.components.StackedBar
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.components.axisScale
import com.finanzen.ui.components.currencyDecimals
import com.finanzen.ui.components.niceTicks
import com.finanzen.ui.format.formatDecimal
import com.finanzen.ui.format.formatMesAnio
import com.finanzen.ui.format.formatPercent
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSageType
import com.finanzen.viewmodel.DemoDataViewModel
import com.finanzen.viewmodel.TrendGrouping
import com.finanzen.viewmodel.TrendsData
import com.finanzen.viewmodel.TrendsViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs
import kotlin.math.pow

/** Tendencias de gasto: promedio diario, gráfico por día/semana/mes, recurrente vs. puntual y categorías. */
@Composable
fun TrendsScreen(onBack: () -> Unit, vm: TrendsViewModel = koinViewModel(), demoVm: DemoDataViewModel = koinViewModel()) {
    val data by vm.data.collectAsState()
    val grouping by vm.grouping.collectAsState()
    val month by vm.month.collectAsState()
    val noMovements by demoVm.canLoad.collectAsState()
    val loadingDemo by demoVm.loading.collectAsState()
    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("Tendencias de gasto", onBack = onBack)
        val current = data
        when {
            current == null || loadingDemo -> LoadingState(Modifier.fillMaxWidth())

            noMovements == true -> DemoDataEmptyState(onLoad = demoVm::load)

            else -> ScreenList {
                item(key = "controls") {
                    Column {
                        SegmentedControl(TrendGrouping.entries, grouping, vm::setGrouping, label = { it.label })
                        MonthSelector(
                            label = formatMesAnio(month),
                            onPrev = { vm.shiftMonth(-1) },
                            onNext = { vm.shiftMonth(1) },
                            onLabelClick = vm::goToCurrentMonth,
                        )
                    }
                }
                item(key = "average") { AverageCard(current) }
                item(key = "split") { SplitCard(current) }
                if (current.categories.isNotEmpty()) item(key = "categories") { CategoriesCard(current) }
            }
        }
    }
}

/**
 * Estado vacío con "Cargar datos de ejemplo": solo aparece sin ningún movimiento y pide confirmación,
 * porque crea cuentas, movimientos, una suscripción, un fondo y un plan de cuotas de prueba.
 */
@Composable
internal fun DemoDataEmptyState(
    onLoad: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter = LucideIcons.PieChart,
    subtitle: String = "Registra tus gastos para ver sus tendencias, o carga datos de ejemplo para explorar la app.",
) {
    var confirm by remember { mutableStateOf(false) }
    EmptyState(
        icon = icon,
        title = "Aún no hay movimientos",
        subtitle = subtitle,
        modifier = modifier,
        actionLabel = "Cargar datos de ejemplo",
        onAction = { confirm = true },
    )
    if (confirm) {
        ConfirmDialog(
            title = "¿Cargar datos de ejemplo?",
            body = "Se crean cuentas, cuatro años de movimientos, una suscripción, un fondo y un plan de cuotas de prueba. " +
                "Luego puedes borrarlos desde cada pantalla.",
            onConfirm = {
                confirm = false
                onLoad()
            },
            onDismiss = { confirm = false },
            confirmLabel = "Cargar",
            destructive = false,
        )
    }
}

@Composable
private fun AverageCard(data: TrendsData) {
    val finance = LocalFinanceColors.current
    val money = LocalMoneyFormat.current
    val decimals = currencyDecimals(data.currency)
    val maxMajor = (data.bars.maxOfOrNull { it.expenseMinor } ?: 0L) / 10.0.pow(decimals)
    val scale = axisScale(maxMajor)
    val toAxis = { minor: Long -> minor / 10.0.pow(decimals) / scale.divisor }
    val per = when (data.grouping) {
        TrendGrouping.DAY -> "día"
        TrendGrouping.WEEK -> "semana"
        TrendGrouping.MONTH -> "mes"
    }
    val peak = data.bars.maxByOrNull { it.expenseMinor }
    val description = "Gasto puntual por $per. " +
        (peak?.takeIf { it.expenseMinor > 0 }?.let { "El más alto: ${it.label}, con ${money.format(it.expenseMinor, data.currency)}. " } ?: "") +
        "Promedio: ${money.format(data.barAverageMinor, data.currency)} por $per."
    FinanceCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Overline("Gasto diario promedio")
                MoneyText(data.averageDailyMinor, data.currency, style = LocalSageType.current.amountLg)
            }
            AverageDelta(data)
            BarChart(
                bars = data.bars.map { BarDatum(toAxis(it.expenseMinor), it.axisLabel) },
                yTicks = niceTicks(maxMajor / scale.divisor).map { AxisTick(it, if (it % 1.0 == 0.0) it.toLong().toString() else formatDecimal(it)) },
                contentDescription = description,
                color = finance.expense,
                reference = toAxis(data.barAverageMinor),
            )
            Caption("${scale.unit} ${data.currency} por $per, sin pagos recurrentes. Línea punteada: promedio.".trim())
        }
    }
}

@Composable
private fun AverageDelta(data: TrendsData) {
    if (data.previousAverageMinor == 0L || data.averageDailyMinor == 0L) return
    val diff = data.averageDailyMinor - data.previousAverageMinor
    val amount = LocalMoneyFormat.current.format(abs(diff), data.currency)
    when {
        diff < 0 -> SageBadge("$amount menos que en ${data.previousMonthName}", tone = BadgeTone.Income, icon = LucideIcons.ChevronDown)
        diff > 0 -> SageBadge("$amount más que en ${data.previousMonthName}", tone = BadgeTone.Expense, icon = LucideIcons.ChevronUp)
        else -> SageBadge("Igual que en ${data.previousMonthName}")
    }
}

@Composable
private fun SplitCard(data: TrendsData) {
    val finance = LocalFinanceColors.current
    val split = data.split
    val recurringColor = finance.chart[2]
    FinanceCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Recurrentes y puntuales", style = LocalSageType.current.titleCard)
            if (split.totalMinor == 0L) {
                Caption("Sin gastos en este mes.")
                return@Column
            }
            StackedBar(
                segments = listOf(BarSegment(split.recurringMinor.toFloat(), recurringColor), BarSegment(split.oneOffMinor.toFloat(), finance.expense)),
                contentDescription = "${formatPercent(split.recurringFraction.toDouble(), 0)} del gasto es recurrente y " +
                    "${formatPercent(1.0 - split.recurringFraction, 0)} puntual",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SplitStat(
                    "Recurrentes · ${countLabel(split.recurringCount, "pago", "pagos")}",
                    split.recurringMinor,
                    split.recurringFraction.toDouble(),
                    data.currency,
                    recurringColor,
                    Modifier.weight(1f),
                )
                SplitStat(
                    "Puntuales · ${countLabel(split.oneOffCount, "movimiento", "movimientos")}",
                    split.oneOffMinor,
                    1.0 - split.recurringFraction,
                    data.currency,
                    finance.expense,
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SplitStat(label: String, amountMinor: Long, fraction: Double, currency: String, color: Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.size(8.dp).background(color, MaterialTheme.shapes.extraSmall))
            Caption(label)
        }
        MoneyText(amountMinor, currency, style = LocalSageType.current.amountMd)
        Caption(formatPercent(fraction, 0))
    }
}

@Composable
private fun CategoriesCard(data: TrendsData) {
    val chart = LocalFinanceColors.current.chart
    val maxAmount = data.categories.maxOf { it.amountMinor }.coerceAtLeast(1).toFloat()
    FinanceCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("En qué se va", style = LocalSageType.current.titleCard)
            data.categories.forEach { c ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(c.name, Modifier.width(CATEGORY_LABEL_WIDTH.dp), style = LocalSageType.current.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    MagnitudeBar(c.amountMinor / maxAmount, if (c.isOther) chart.last() else chart.first(), Modifier.weight(1f))
                    MoneyText(c.amountMinor, data.currency, style = LocalSageType.current.amountSm)
                }
            }
        }
    }
}

@Composable
private fun Caption(text: String) {
    Text(text, style = LocalSageType.current.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun countLabel(count: Int, singular: String, plural: String) = "$count ${if (count == 1) singular else plural}"

private const val CATEGORY_LABEL_WIDTH = 96
