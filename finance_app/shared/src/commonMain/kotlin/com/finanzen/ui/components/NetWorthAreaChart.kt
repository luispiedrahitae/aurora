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
import com.finanzen.viewmodel.MonthNetWorth

/**
 * Área rellena bajo la línea de patrimonio neto. Un único color para toda la serie según la
 * tendencia global (verde si el último punto >= el primero, rojo si cayó). El valor del punto
 * seleccionado va como titular arriba; por defecto es el más reciente y tocar otra columna lo
 * cambia (mismo mecanismo que ExpenseBarChart / IncomeExpenseLineChart).
 */
@Composable
fun NetWorthAreaChart(points: List<MonthNetWorth>, currency: String, modifier: Modifier = Modifier) {
    if (points.isEmpty()) return
    val finance = LocalFinanceColors.current
    val fmt = LocalMoneyFormat.current
    var selectedIndex by remember(points) { mutableStateOf(points.lastIndex) }
    val maxValue = points.maxOf { it.netWorthMinor }.coerceAtLeast(1L)
    val rising = points.last().netWorthMinor - points.first().netWorthMinor >= 0
    val lineColor = if (rising) finance.income else finance.expense
    val n = points.size
    val sel = points[selectedIndex]
    val markerLineColor = MaterialTheme.colorScheme.outlineVariant

    FinanceCard(modifier = modifier.semantics { contentDescription = "Evolución del patrimonio neto" }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Titular: valor del punto seleccionado (por defecto el más reciente).
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    fmt.format(sel.netWorthMinor, currency),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = lineColor,
                )
                Text(
                    "Patrimonio neto · ${sel.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Leyenda (serie única).
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(lineColor))
                Text(
                    if (rising) "Patrimonio neto (al alza)" else "Patrimonio neto (a la baja)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    val bottom = size.height - padBottom
                    val line = Path()
                    val fill = Path()
                    points.forEachIndexed { i, p ->
                        val x = cx(i)
                        val y = cy(p.netWorthMinor)
                        if (i == 0) {
                            line.moveTo(x, y)
                            fill.moveTo(x, bottom)
                            fill.lineTo(x, y)
                        } else {
                            line.lineTo(x, y)
                            fill.lineTo(x, y)
                        }
                    }
                    fill.lineTo(cx(n - 1), bottom)
                    fill.close()
                    drawPath(fill, lineColor.copy(alpha = 0.22f))
                    drawPath(line, lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    // Línea vertical + punto del mes seleccionado.
                    val selX = cx(selectedIndex)
                    drawLine(
                        color = markerLineColor,
                        start = Offset(selX, padTop),
                        end = Offset(selX, bottom),
                        strokeWidth = 2f,
                    )
                    points.forEachIndexed { i, p ->
                        val r = if (i == selectedIndex) 6f else 3.5f
                        drawCircle(lineColor, radius = r, center = Offset(cx(i), cy(p.netWorthMinor)))
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
