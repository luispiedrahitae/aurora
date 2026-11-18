package com.finanzen.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSageType
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

/** Una serie de [LineChart]: valores en orden de x (índice), su color y si es línea de referencia punteada. */
data class ChartSeries(val label: String, val values: List<Double>, val color: Color, val dashed: Boolean = false)

/** Escala de un eje en unidades enteras de la moneda: [unit] es "Millones de", "Miles de" o vacío. */
data class AxisScale(val divisor: Double, val unit: String)

/** Escala legible para un máximo en unidades enteras de la moneda (millones, miles o unidades). */
fun axisScale(maxMajor: Double): AxisScale = when {
    maxMajor >= MILLION -> AxisScale(MILLION, "Millones de")
    maxMajor >= THOUSAND -> AxisScale(THOUSAND, "Miles de")
    else -> AxisScale(1.0, "")
}

/** Decimales de [currency] según el catálogo de monedas (2 si no se conoce). */
@Composable
fun currencyDecimals(currency: String): Int = LocalMoneyFormat.current.currencies[currency]?.decimals?.toInt() ?: 2

/** Marca del eje: posición (valor en x o en y) y su texto. */
data class AxisTick(val value: Double, val label: String)

/** Una barra de [BarChart]; [label] es la marca del eje x (nula para no rotular esa barra). */
data class BarDatum(val value: Double, val label: String? = null)

/** Un tramo de [StackedBar]: su peso (cualquier escala; se reparte en proporción) y su color. */
data class BarSegment(val fraction: Float, val color: Color)

/**
 * Gráfico de líneas Sage: rejilla horizontal en [yTicks], eje inferior, marcas de [xTicks] y, a la
 * derecha, el nombre de cada serie a la altura de su último punto (sin leyenda aparte). Las series
 * punteadas son referencias (gris, sin punto final). El [contentDescription] resume los datos para
 * lectores de pantalla: el dibujo en sí no es accesible.
 */
@Composable
fun LineChart(
    series: List<ChartSeries>,
    xTicks: List<AxisTick>,
    yTicks: List<AxisTick>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
) {
    val measurer = rememberTextMeasurer()
    val style = chartTextStyle()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val axisColor = MaterialTheme.colorScheme.outline
    Canvas(modifier.fillMaxWidth().height(height).semantics { this.contentDescription = contentDescription }) {
        val leftGutter = yTicks.maxOfOrNull { measurer.measure(it.label, style).size.width }?.plus(GUTTER_GAP.dp.toPx()) ?: 0f
        val rightGutter = series.maxOfOrNull { measurer.measure(it.label, style.copy(fontWeight = FontWeight.SemiBold)).size.width }
            ?.plus(GUTTER_GAP.dp.toPx()) ?: 0f
        val plot = plotArea(leftGutter, rightGutter, measurer.measure("0", style).size.height.toFloat())
        val yMax = yTicks.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1.0
        val lastIndex = (series.maxOfOrNull { it.values.size } ?: 1) - 1
        val xOf = { i: Double -> plot.left + (plot.width * (i / max(lastIndex, 1))).toFloat() }
        val yOf = { v: Double -> plot.bottom - (plot.height * (v / yMax)).toFloat() }

        drawYGrid(yTicks, yOf, plot, gridColor, measurer, style)
        drawLine(axisColor, Offset(plot.left, plot.bottom), Offset(plot.right, plot.bottom), 1.dp.toPx())
        xTicks.forEach { tick -> drawCentered(measurer, tick.label, style, xOf(tick.value), plot.bottom + LABEL_GAP.dp.toPx()) }

        val ends = mutableListOf<Pair<ChartSeries, Float>>()
        series.forEach { s ->
            if (s.values.isEmpty()) return@forEach
            val path = Path()
            s.values.forEachIndexed { i, v -> if (i == 0) path.moveTo(xOf(0.0), yOf(v)) else path.lineTo(xOf(i.toDouble()), yOf(v)) }
            drawPath(
                path,
                s.color,
                style = Stroke(
                    width = (if (s.dashed) REF_STROKE else LINE_STROKE).dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = if (s.dashed) PathEffect.dashPathEffect(floatArrayOf(DASH.dp.toPx(), DASH.dp.toPx())) else null,
                ),
            )
            val endY = yOf(s.values.last())
            if (!s.dashed) drawCircle(s.color, END_DOT.dp.toPx(), Offset(xOf(s.values.lastIndex.toDouble()), endY))
            ends += s to endY
        }
        drawEndLabels(ends, plot.right + GUTTER_GAP.dp.toPx(), measurer, style)
    }
}

/**
 * Gráfico de barras verticales Sage, con rejilla en [yTicks] y una línea punteada opcional en
 * [reference] (p. ej. el promedio). Cada barra rotula su [BarDatum.label] debajo, si lo tiene.
 */
@Composable
fun BarChart(
    bars: List<BarDatum>,
    yTicks: List<AxisTick>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    reference: Double? = null,
    height: Dp = 160.dp,
) {
    val measurer = rememberTextMeasurer()
    val style = chartTextStyle()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val axisColor = MaterialTheme.colorScheme.outline
    val refColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier.fillMaxWidth().height(height).semantics { this.contentDescription = contentDescription }) {
        val leftGutter = yTicks.maxOfOrNull { measurer.measure(it.label, style).size.width }?.plus(GUTTER_GAP.dp.toPx()) ?: 0f
        val plot = plotArea(leftGutter, 0f, measurer.measure("0", style).size.height.toFloat())
        val yMax = yTicks.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1.0
        val yOf = { v: Double -> plot.bottom - (plot.height * (v.coerceIn(0.0, yMax) / yMax)).toFloat() }
        drawYGrid(yTicks, yOf, plot, gridColor, measurer, style)

        val slot = plot.width / max(bars.size, 1)
        val barWidth = (slot * BAR_FILL).coerceAtMost(MAX_BAR.dp.toPx())
        val radius = CornerRadius(BAR_RADIUS.dp.toPx())
        bars.forEachIndexed { i, bar ->
            val center = plot.left + slot * (i + HALF)
            if (bar.value > 0) {
                val top = yOf(bar.value)
                drawRoundRect(color, Offset(center - barWidth / 2, top), Size(barWidth, plot.bottom - top), radius)
            }
            bar.label?.let { drawCentered(measurer, it, style, center, plot.bottom + LABEL_GAP.dp.toPx()) }
        }
        reference?.takeIf { it > 0 }?.let { ref ->
            val y = yOf(ref)
            drawLine(
                refColor,
                Offset(plot.left, y),
                Offset(plot.right, y),
                REF_STROKE.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH.dp.toPx(), DASH.dp.toPx())),
            )
        }
        drawLine(axisColor, Offset(plot.left, plot.bottom), Offset(plot.right, plot.bottom), 1.dp.toPx())
    }
}

/** Barra horizontal partida en tramos (recurrente/puntual, aportado/rendimiento/comisiones). */
@Composable
fun StackedBar(segments: List<BarSegment>, contentDescription: String, modifier: Modifier = Modifier, height: Dp = 12.dp) {
    val visible = segments.filter { it.fraction > 0f }
    val radius = height / 2
    Row(
        modifier.fillMaxWidth().height(height).semantics { this.contentDescription = contentDescription },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        visible.forEachIndexed { i, segment ->
            val shape = RoundedCornerShape(
                topStart = if (i == 0) radius else 0.dp,
                bottomStart = if (i == 0) radius else 0.dp,
                topEnd = if (i == visible.lastIndex) radius else 0.dp,
                bottomEnd = if (i == visible.lastIndex) radius else 0.dp,
            )
            Box(Modifier.weight(segment.fraction).fillMaxHeight().background(segment.color, shape))
        }
    }
}

/** Barra de magnitud de una fila (p. ej. una categoría): [fraction] del ancho disponible. */
@Composable
fun MagnitudeBar(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 12.dp) {
    Box(modifier.fillMaxWidth().height(height)) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(MIN_VISIBLE_FRACTION, 1f))
                .fillMaxHeight()
                .background(color, RoundedCornerShape(height / 2)),
        )
    }
}

/**
 * Marcas "redondas" del eje y de 0 a un máximo que cubre [maxValue] (pasos de 1, 2, 2,5 o 5 × 10ⁿ),
 * con unas [targetCount] divisiones. Puro para poder probarlo.
 */
fun niceTicks(maxValue: Double, targetCount: Int = DEFAULT_TICKS): List<Double> {
    if (maxValue <= 0.0) return listOf(0.0, 1.0)
    val rough = maxValue / targetCount
    val magnitude = 10.0.pow(floor(log10(rough)))
    val step = NICE_STEPS.first { it * magnitude >= rough } * magnitude
    val top = ceil(maxValue / step) * step
    return generateSequence(0.0) { it + step }.takeWhile { it <= top + step / 2 }.toList()
}

@Composable
private fun chartTextStyle(): TextStyle = LocalSageType.current.caption.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)

private data class PlotArea(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top
}

private fun DrawScope.plotArea(leftGutter: Float, rightGutter: Float, labelHeight: Float): PlotArea {
    val top = labelHeight / 2
    return PlotArea(leftGutter, top, size.width - rightGutter, size.height - labelHeight - LABEL_GAP.dp.toPx())
}

private fun DrawScope.drawYGrid(
    ticks: List<AxisTick>,
    yOf: (Double) -> Float,
    plot: PlotArea,
    color: Color,
    measurer: TextMeasurer,
    style: TextStyle,
) {
    ticks.forEach { tick ->
        val y = yOf(tick.value)
        if (tick.value > 0) drawLine(color, Offset(plot.left, y), Offset(plot.right, y), 1.dp.toPx())
        val layout = measurer.measure(tick.label, style)
        drawText(layout, topLeft = Offset(plot.left - GUTTER_GAP.dp.toPx() - layout.size.width, y - layout.size.height / 2))
    }
}

private fun DrawScope.drawCentered(measurer: TextMeasurer, text: String, style: TextStyle, centerX: Float, top: Float) {
    val layout = measurer.measure(text, style)
    val x = (centerX - layout.size.width / 2).coerceIn(0f, size.width - layout.size.width)
    drawText(layout, topLeft = Offset(x, top))
}

/** Nombres de serie al final de cada línea, separados para que no se monten si los finales están cerca. */
private fun DrawScope.drawEndLabels(ends: List<Pair<ChartSeries, Float>>, x: Float, measurer: TextMeasurer, style: TextStyle) {
    var lastBottom = Float.NEGATIVE_INFINITY
    ends.sortedBy { it.second }.forEach { (s, y) ->
        val layout = measurer.measure(
            s.label,
            style.copy(color = if (s.dashed) style.color else s.color, fontWeight = if (s.dashed) null else FontWeight.SemiBold),
        )
        val top = max(y - layout.size.height / 2, lastBottom)
        drawText(layout, topLeft = Offset(x, top))
        lastBottom = top + layout.size.height
    }
}

@Suppress("MagicNumber") // la serie 1-2-2,5-5-10 es la definición de "paso redondo"
private val NICE_STEPS = listOf(1.0, 2.0, 2.5, 5.0, 10.0)
private const val HALF = 0.5f
private const val DEFAULT_TICKS = 4
private const val MILLION = 1_000_000.0
private const val THOUSAND = 1_000.0
private const val GUTTER_GAP = 6
private const val LABEL_GAP = 6
private const val LINE_STROKE = 2.5f
private const val REF_STROKE = 1.5f
private const val DASH = 4f
private const val END_DOT = 4f
private const val BAR_FILL = 0.68f
private const val MAX_BAR = 24
private const val BAR_RADIUS = 3
private const val MIN_VISIBLE_FRACTION = 0.02f
