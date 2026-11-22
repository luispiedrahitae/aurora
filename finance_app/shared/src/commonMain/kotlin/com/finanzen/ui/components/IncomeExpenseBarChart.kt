package com.finanzen.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSageType
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.viewmodel.TrendPoint
import kotlinx.coroutines.flow.first

/**
 * Barras agrupadas, un punto por columna: ingreso (sage) y gasto (terracotta) como dos barras
 * delgadas lado a lado dentro de la misma columna, ambas creciendo desde el piso. En modo Año
 * (12 puntos) reparte el ancho disponible entre columnas, igual que `Sage.dc.html`. En modo Mes
 * (hasta 31 puntos) esas mismas columnas ya no caben sin aplastarse — en vez de encogerlas, se
 * mantiene el ancho de columna de referencia (el que usaría un año completo) y el gráfico se
 * desliza horizontalmente, así ambos modos quedan a la misma escala visual de barra.
 */
@Composable
fun IncomeExpenseBarChart(
    points: List<TrendPoint>,
    currency: String,
    modifier: Modifier = Modifier,
    description: String = "Ingresos y gastos",
    initialSelectedIndex: Int? = null,
    totalLabel: String = "Total del período",
) {
    if (points.isEmpty()) return
    val finance = LocalFinanceColors.current
    val fmt = LocalMoneyFormat.current
    val maxValue = points.maxOf { maxOf(it.incomeMinor, it.expenseMinor) }.coerceAtLeast(1L)
    val n = points.size
    var selectedIndex by remember(points) { mutableStateOf(initialSelectedIndex) }
    val highlightColor = MaterialTheme.colorScheme.surfaceVariant
    // Resumen hablado: el lector de pantalla no puede "ver" las barras ni tocarlas una a una.
    val spoken = "$description. Ingresos ${fmt.format(points.sumOf { it.incomeMinor }, currency)}, " +
        "gastos ${fmt.format(points.sumOf { it.expenseMinor }, currency)}"
    FinanceCard(modifier = modifier.semantics { contentDescription = spoken }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ChartHeader(selectedIndex?.let { points.getOrNull(it) }, points, currency, totalLabel)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columnWidth = maxWidth / REFERENCE_COLUMNS
                val contentWidth = maxOf(maxWidth, columnWidth * n)
                val scroll = rememberScrollState()
                val viewportPx = constraints.maxWidth
                ScrollToSelection(scroll, points, viewportPx, initialSelectedIndex)
                Box(Modifier.fillMaxWidth().horizontalScroll(scroll)) {
                    Column(Modifier.width(contentWidth), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Canvas(
                            Modifier.fillMaxWidth().height(150.dp).pointerInput(points) {
                                detectTapGestures { offset ->
                                    val tapped = (offset.x / (size.width.toFloat() / n)).toInt().coerceIn(0, n - 1)
                                    selectedIndex = if (selectedIndex == tapped) null else tapped
                                }
                            },
                        ) {
                            val pad = 12f
                            val baselineY = size.height - pad
                            val usableHeight = (baselineY - pad).coerceAtLeast(1f)
                            val cw = size.width / n
                            val barWidth = cw * 0.32f
                            val innerGap = cw * 0.06f
                            fun cx(i: Int) = (i + 0.5f) * cw

                            selectedIndex?.let { sel ->
                                drawRoundRect(
                                    color = highlightColor,
                                    topLeft = Offset(cx(sel) - cw / 2f, 0f),
                                    size = Size(cw, size.height),
                                    cornerRadius = CornerRadius(8f, 8f),
                                )
                            }

                            points.forEachIndexed { i, p ->
                                val centerX = cx(i)
                                val incomeLeft = centerX - innerGap / 2f - barWidth
                                val expenseLeft = centerX + innerGap / 2f
                                // .coerceAtLeast(2f): sin piso mínimo, un monto pequeño pero distinto de
                                // cero puede redondear a menos de 1px y desaparecer.
                                if (p.incomeMinor > 0L) {
                                    val incomeHeight = ((p.incomeMinor.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f) * usableHeight).coerceAtLeast(2f)
                                    drawRoundRect(
                                        color = finance.income,
                                        topLeft = Offset(incomeLeft, baselineY - incomeHeight),
                                        size = Size(barWidth, incomeHeight),
                                        cornerRadius = CornerRadius(6f, 6f),
                                    )
                                }
                                if (p.expenseMinor > 0L) {
                                    val expenseHeight = ((p.expenseMinor.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f) * usableHeight).coerceAtLeast(2f)
                                    drawRoundRect(
                                        color = finance.expense,
                                        topLeft = Offset(expenseLeft, baselineY - expenseHeight),
                                        size = Size(barWidth, expenseHeight),
                                        cornerRadius = CornerRadius(6f, 6f),
                                    )
                                }
                            }
                        }
                        AxisLabels(points, selectedIndex)
                    }
                }
            }
        }
    }
}

/** Al cambiar de mes, el gráfico desplazable vuelve al inicio, o se centra en la barra de hoy. */
@Composable
private fun ScrollToSelection(scroll: ScrollState, points: List<TrendPoint>, viewportPx: Int, selectedIndex: Int?) {
    LaunchedEffect(points, viewportPx) {
        if (points.size <= REFERENCE_COLUMNS) return@LaunchedEffect
        val maxScroll = snapshotFlow { scroll.maxValue }.first { it in 1 until Int.MAX_VALUE }
        val columnPx = viewportPx / REFERENCE_COLUMNS
        val target = selectedIndex?.let { it * columnPx - viewportPx / 2 + columnPx / 2 } ?: 0
        scroll.scrollTo(target.coerceIn(0, maxScroll))
    }
}

/**
 * Cabecera de alto fijo: sin barra elegida muestra el total del período; con una barra, sus cifras.
 * La leyenda es a la vez el rótulo de cada cifra, así "Ingresos" y "Gastos" no se repiten y nada se
 * reacomoda al cambiar de barra o de mes.
 */
@Composable
private fun ChartHeader(selected: TrendPoint?, points: List<TrendPoint>, currency: String, totalLabel: String) {
    val finance = LocalFinanceColors.current
    val fmt = LocalMoneyFormat.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            selected?.label?.let { "$it · toca de nuevo para ver el total" } ?: "$totalLabel · toca una barra para ver el detalle",
            style = LocalSageType.current.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ChartFigure(finance.income, "Ingresos", fmt.format(selected?.incomeMinor ?: points.sumOf { it.incomeMinor }, currency), Modifier.weight(1f))
            ChartFigure(finance.expense, "Gastos", fmt.format(selected?.expenseMinor ?: points.sumOf { it.expenseMinor }, currency), Modifier.weight(1f))
        }
    }
}

/** Rótulos del eje, uno por columna; el de la barra elegida va resaltado. */
@Composable
private fun AxisLabels(points: List<TrendPoint>, selectedIndex: Int?) {
    Row(Modifier.fillMaxWidth()) {
        points.forEachIndexed { i, p ->
            val isSelected = i == selectedIndex
            Text(
                p.axisLabel,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.SemiBold else null,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/** Rótulo con su punto de color (la leyenda) y la cifra debajo, en una línea que se encoge si no cabe. */
@Composable
private fun ChartFigure(color: Color, label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Text(label, style = LocalSageType.current.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FitText(value, style = LocalSageType.current.amountMd, color = color)
    }
}

/** Columnas que caben sin desplazar: las de un año. Un mes (hasta 31 días) se desliza. */
private const val REFERENCE_COLUMNS = 12
