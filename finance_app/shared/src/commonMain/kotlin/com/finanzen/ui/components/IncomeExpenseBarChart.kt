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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.viewmodel.DayPoint

/**
 * Barras divergentes, un día por columna (mes seleccionado): tramo verde hacia arriba (ingreso) y
 * tramo rosa hacia abajo (gasto) desde una línea base compartida. La mayoría de los días no tienen
 * movimiento, así que una línea continua conectando ceros con picos aislados se ve como un
 * electrocardiograma; con barras independientes por día, un día sin movimiento simplemente no
 * dibuja nada. Tocar una columna la selecciona y muestra sus valores exactos arriba (mismo
 * mecanismo de "tap para seleccionar" que [NetWorthAreaChart]/[MonthlyBarChart]).
 */
@Composable
fun IncomeExpenseBarChart(points: List<DayPoint>, currency: String, modifier: Modifier = Modifier) {
    if (points.isEmpty()) return
    val finance = LocalFinanceColors.current
    val fmt = LocalMoneyFormat.current
    var selectedIndex by remember(points) { mutableStateOf(points.lastIndex) }
    val maxValue = points.maxOf { maxOf(it.incomeMinor, it.expenseMinor) }.coerceAtLeast(1L)
    val n = points.size
    val sel = points[selectedIndex]
    val markerLineColor = MaterialTheme.colorScheme.outlineVariant

    FinanceCard(modifier = modifier.semantics { contentDescription = "Ingresos y gastos por día" }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Encabezado: valores exactos del día seleccionado.
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
            // Barras divergentes con overlay táctil por día.
            Box(Modifier.fillMaxWidth().height(160.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    val pad = 12f
                    val baselineY = size.height / 2f
                    val usableHalf = (baselineY - pad).coerceAtLeast(1f)
                    val cw = size.width / n
                    val barWidth = cw * 0.5f
                    fun cx(i: Int) = (i + 0.5f) * cw

                    drawLine(
                        color = markerLineColor,
                        start = Offset(0f, baselineY),
                        end = Offset(size.width, baselineY),
                        strokeWidth = 1f,
                    )

                    points.forEachIndexed { i, p ->
                        val left = i * cw + (cw - barWidth) / 2f
                        val alpha = if (i == selectedIndex) 1f else 0.4f
                        // .coerceAtLeast(2f) igual que MonthlyBarChart: sin piso mínimo, un monto
                        // pequeño pero distinto de cero puede redondear a menos de 1px y desaparecer.
                        if (p.incomeMinor > 0L) {
                            val incomeHeight = ((p.incomeMinor.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f) * usableHalf).coerceAtLeast(2f)
                            drawRoundRect(
                                color = finance.income.copy(alpha = alpha),
                                topLeft = Offset(left, baselineY - incomeHeight),
                                size = Size(barWidth, incomeHeight),
                                cornerRadius = CornerRadius(6f, 6f),
                            )
                        }
                        if (p.expenseMinor > 0L) {
                            val expenseHeight = ((p.expenseMinor.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f) * usableHalf).coerceAtLeast(2f)
                            drawRoundRect(
                                color = finance.expense.copy(alpha = alpha),
                                topLeft = Offset(left, baselineY),
                                size = Size(barWidth, expenseHeight),
                                cornerRadius = CornerRadius(6f, 6f),
                            )
                        }
                    }

                    // Línea vertical del día seleccionado.
                    val selX = cx(selectedIndex)
                    drawLine(
                        color = markerLineColor,
                        start = Offset(selX, pad),
                        end = Offset(selX, size.height - pad),
                        strokeWidth = 2f,
                    )
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
            // Eje de días: con hasta 31 puntos solo se rotula cada ~5 días (y el seleccionado) para
            // no amontonar texto, pero cada día sigue siendo su propia columna/objetivo táctil.
            Row(Modifier.fillMaxWidth()) {
                points.forEachIndexed { i, p ->
                    Text(
                        if (i % 5 == 0 || i == selectedIndex) p.dayNumber.toString() else "",
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
