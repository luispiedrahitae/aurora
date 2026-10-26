package com.finanzen.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.viewmodel.TrendPoint

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
) {
    if (points.isEmpty()) return
    val finance = LocalFinanceColors.current
    val fmt = LocalMoneyFormat.current
    val maxValue = points.maxOf { maxOf(it.incomeMinor, it.expenseMinor) }.coerceAtLeast(1L)
    val n = points.size
    var selectedIndex by remember(points) { mutableStateOf(initialSelectedIndex) }
    val highlightColor = MaterialTheme.colorScheme.surfaceVariant
    val selectedLabelColor = MaterialTheme.colorScheme.onSurface
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    FinanceCard(modifier = modifier.semantics { contentDescription = description }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val selected = selectedIndex?.let { points[it] }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (selected != null) {
                        Text(
                            fmt.format(selected.incomeMinor, currency),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = finance.income,
                        )
                        Text(
                            fmt.format(selected.expenseMinor, currency),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = finance.expense,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendMark(finance.income, "Ingresos")
                    LegendMark(finance.expense, "Gastos")
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val referenceColumns = 12
                val columnWidth = maxWidth / referenceColumns
                val contentWidth = maxOf(maxWidth, columnWidth * n)
                Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
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
                        Row(Modifier.fillMaxWidth()) {
                            points.forEachIndexed { i, p ->
                                val isSelected = i == selectedIndex
                                Text(
                                    p.axisLabel,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else null,
                                    color = if (isSelected) selectedLabelColor else labelColor,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Marca de leyenda: punto de color + etiqueta de texto. */
@Composable
private fun LegendMark(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
