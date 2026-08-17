package com.finanzen.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
 * Área rellena bajo la línea de patrimonio neto: un punto por mes (modo año) o por día (modo mes,
 * ver [com.finanzen.viewmodel.DashboardViewModel.computeDailyNetWorth]) del periodo seleccionado.
 * Un único color para toda la serie según la tendencia global (verde si el último punto >= el
 * primero, rojo si cayó). La línea/área/puntos solo se dibujan hasta [lastDataIndex] — [points]
 * suele incluir meses o días futuros del periodo (arrastran el último saldo conocido, no son datos
 * reales); esos quedan visibles solo como etiquetas en el eje X, sin línea continua encima. El valor
 * del punto seleccionado va como titular arriba; por defecto es el más reciente con datos reales, no
 * el último índice del array, y tocar otra columna lo cambia (mismo mecanismo que
 * [IncomeExpenseBarChart]).
 */
@Composable
fun NetWorthAreaChart(
    points: List<MonthNetWorth>,
    currency: String,
    modifier: Modifier = Modifier,
    lastDataIndex: Int = points.lastIndex,
    initialSelectedIndex: Int = lastDataIndex,
) {
    if (points.isEmpty()) return
    val finance = LocalFinanceColors.current
    val fmt = LocalMoneyFormat.current
    val lastData = lastDataIndex.coerceIn(0, points.lastIndex)
    var selectedIndex by remember(points) { mutableStateOf(initialSelectedIndex.coerceIn(0, points.lastIndex)) }
    // El rango incluye 0 siempre (coerceAtLeast/coerceAtMost) para poder dibujar la línea base y para
    // que un patrimonio negativo no quede indistinguible de un mes en $0 (antes cy() recortaba frac a
    // 0f para cualquier valor negativo, aplastando ambos casos al fondo del gráfico).
    val maxValue = points.maxOf { it.netWorthMinor }.coerceAtLeast(0L)
    val minValue = points.minOf { it.netWorthMinor }.coerceAtMost(0L)
    val range = (maxValue - minValue).coerceAtLeast(1L)
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
            // Área del gráfico con overlay táctil por día.
            Box(Modifier.fillMaxWidth().height(160.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    val padTop = 12f
                    val padBottom = 12f
                    val usableH = (size.height - padTop - padBottom).coerceAtLeast(1f)
                    val cw = size.width / n
                    fun cx(i: Int) = (i + 0.5f) * cw
                    fun cy(value: Long): Float {
                        val frac = (value - minValue).toFloat() / range.toFloat()
                        return padTop + (1f - frac) * usableH
                    }
                    val bottom = size.height - padBottom
                    if (minValue < 0L) {
                        // Línea base en 0: sin ella, un patrimonio muy negativo y uno en break-even se
                        // verían igual de "abajo" en el gráfico.
                        drawLine(
                            color = markerLineColor,
                            start = Offset(0f, cy(0L)),
                            end = Offset(size.width, cy(0L)),
                            strokeWidth = 1.5f,
                        )
                    }
                    // Solo se dibuja hasta [lastData]: más allá no hay información real, aunque el
                    // punto exista en la lista (arrastra el último saldo conocido). El eje de
                    // etiquetas más abajo sí sigue mostrando todos los puntos.
                    val line = Path()
                    val fill = Path()
                    points.take(lastData + 1).forEachIndexed { i, p ->
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
                    fill.lineTo(cx(lastData), bottom)
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
                    points.take(lastData + 1).forEachIndexed { i, p ->
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
                                .clickable { selectedIndex = i.coerceAtMost(lastData) },
                        )
                    }
                }
            }
            // Eje: 12 puntos (año) caben sin adelgazar etiquetas; hasta 31 (mes) se agrupan en
            // bloques de `labelEvery` columnas por etiqueta (axis-readability) — cada bloque muestra
            // la etiqueta del punto seleccionado si cae dentro, si no la de su primer índice. El peso
            // de cada Text es el tamaño del bloque (no 1 fijo): así una etiqueta de dos dígitos tiene
            // el ancho de varias columnas para no recortarse a un solo carácter.
            val labelEvery = if (n > 12) ((n + 11) / 12) else 1
            Row(Modifier.fillMaxWidth()) {
                var i = 0
                while (i < n) {
                    val chunkEnd = (i + labelEvery).coerceAtMost(n)
                    val labelIndex = if (selectedIndex in i until chunkEnd) selectedIndex else i
                    val p = points[labelIndex]
                    Text(
                        p.label,
                        modifier = Modifier.weight((chunkEnd - i).toFloat()),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (labelIndex == selectedIndex) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (labelIndex == selectedIndex) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    i = chunkEnd
                }
            }
        }
    }
}
