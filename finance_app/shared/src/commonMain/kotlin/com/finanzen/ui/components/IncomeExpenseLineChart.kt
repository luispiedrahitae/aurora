package com.finanzen.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.viewmodel.MonthPoint

/**
 * Dos líneas (ingresos en verde, gastos en rojo) sobre un eje de meses compartido. Tocar la
 * columna de un mes lo selecciona y muestra sus valores exactos arriba (mismo mecanismo de
 * "tap para seleccionar" de ExpenseBarChart; no hay hover en táctil). El mes más reciente empieza
 * seleccionado. Leyenda con etiquetas de texto para que el color no sea la única señal.
 */
@Composable
fun IncomeExpenseLineChart(points: List<MonthPoint>, currency: String, modifier: Modifier = Modifier) {
    if (points.isEmpty()) return
    val finance = LocalFinanceColors.current
    val fmt = LocalMoneyFormat.current
    var selectedIndex by remember(points) { mutableStateOf(points.lastIndex) }
    val maxValue = points.maxOf { maxOf(it.incomeMinor, it.expenseMinor) }.coerceAtLeast(1L)
    val n = points.size
    val sel = points[selectedIndex]
    val markerLineColor = MaterialTheme.colorScheme.outlineVariant

    FinanceCard(modifier = modifier.semantics { contentDescription = "Ingresos y gastos por mes" }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Encabezado: valores exactos del mes seleccionado.
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(sel.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Ingresos ${fmt.format(sel.incomeMinor, currency)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = finance.income,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "Gastos ${fmt.format(sel.expenseMinor, currency)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = finance.expense,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            // Leyenda.
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendMark(finance.income, "Ingresos")
                LegendMark(finance.expense, "Gastos")
            }
            // Área del gráfico con overlay táctil por mes.
            Box(Modifier.fillMaxWidth().height(160.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    val padTop = 12f
                    val padBottom = 12f
                    val usableH = (size.height - padTop - padBottom).coerceAtLeast(1f)
                    val cw = size.width / n
                    fun cx(i: Int) = (i + 0.5f) * cw
                    fun cy(value: Long): Float {
                        val frac = (value.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
                        return padTop + (1f - frac) * usableH
                    }
                    // Línea vertical del mes seleccionado.
                    val selX = cx(selectedIndex)
                    drawLine(
                        color = markerLineColor,
                        start = Offset(selX, padTop),
                        end = Offset(selX, size.height - padBottom),
                        strokeWidth = 2f,
                    )
                    val incomePath = Path()
                    val expensePath = Path()
                    points.forEachIndexed { i, p ->
                        val x = cx(i)
                        val yi = cy(p.incomeMinor)
                        val ye = cy(p.expenseMinor)
                        if (i == 0) {
                            incomePath.moveTo(x, yi)
                            expensePath.moveTo(x, ye)
                        } else {
                            incomePath.lineTo(x, yi)
                            expensePath.lineTo(x, ye)
                        }
                    }
                    val stroke = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    drawPath(incomePath, finance.income, style = stroke)
                    drawPath(expensePath, finance.expense, style = stroke)
                    points.forEachIndexed { i, p ->
                        val x = cx(i)
                        val r = if (i == selectedIndex) 6f else 3.5f
                        drawCircle(finance.income, radius = r, center = Offset(x, cy(p.incomeMinor)))
                        drawCircle(finance.expense, radius = r, center = Offset(x, cy(p.expenseMinor)))
                    }
                }
                Row(Modifier.fillMaxSize()) {
                    points.forEachIndexed { i, _ ->
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { selectedIndex = i },
                        )
                    }
                }
            }
            // Eje de meses.
            Row(Modifier.fillMaxWidth()) {
                points.forEachIndexed { i, p ->
                    Text(
                        p.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (i == selectedIndex) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (i == selectedIndex) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Marca de leyenda: punto de color + etiqueta de texto. */
@Composable
private fun LegendMark(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
