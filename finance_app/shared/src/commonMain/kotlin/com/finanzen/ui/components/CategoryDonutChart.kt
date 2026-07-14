package com.finanzen.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.viewmodel.CategorySlice
import kotlin.math.roundToInt

/**
 * Anillo (donut) de gasto por categoría. Máximo 6 arcos: las 5 mayores más un "Otros" sintético
 * (regla de "no abusar del pastel"). Se dejan huecos de 2° entre arcos con el color del fondo para
 * garantizar la separación visual aunque dos categorías tengan tonos parecidos. La leyenda lista
 * cada porción con su color, nombre, porcentaje y monto, así el color nunca es la única señal.
 */
@Composable
fun CategoryDonutChart(slices: List<CategorySlice>, currency: String, modifier: Modifier = Modifier) {
    if (slices.isEmpty()) return
    val finance = LocalFinanceColors.current
    val fmt = LocalMoneyFormat.current
    val gapColor = MaterialTheme.colorScheme.surfaceContainer

    // Colapsa a <=6 porciones: 5 mayores + "Otros". otrosIndex marca la sintética para colorearla.
    val (display, otrosIndex) = remember(slices) {
        val sorted = slices.sortedByDescending { it.pct }
        if (sorted.size > 6) {
            val top = sorted.take(5)
            val rest = sorted.drop(5)
            val otros = CategorySlice(
                name = "Otros",
                amountMinor = rest.sumOf { it.amountMinor },
                pct = rest.map { it.pct }.sum(),
            )
            (top + otros) to 5
        } else {
            sorted to -1
        }
    }
    val colors = display.mapIndexed { i, s ->
        if (i == otrosIndex) finance.neutral else categoryColor(s.name, s.color)
    }

    FinanceCard(modifier = modifier.semantics { contentDescription = "Distribución de gastos por categoría" }) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth(0.62f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
                    val strokeW = 34f
                    val inset = strokeW / 2f
                    val topLeft = Offset(inset, inset)
                    val arcSize = Size(size.width - strokeW, size.height - strokeW)
                    val gap = 2f
                    var start = -90f
                    display.forEachIndexed { i, s ->
                        val sweep = s.pct * 360f
                        drawArc(
                            color = colors[i],
                            startAngle = start + gap / 2f,
                            sweepAngle = (sweep - gap).coerceAtLeast(0f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeW, cap = StrokeCap.Butt),
                        )
                        // Refuerza la separación pintando el hueco con el color del fondo de la tarjeta.
                        drawArc(
                            color = gapColor,
                            startAngle = start,
                            sweepAngle = gap / 2f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeW, cap = StrokeCap.Butt),
                        )
                        start += sweep
                    }
                }
            }
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                display.forEachIndexed { i, s ->
                    val pctInt = (s.pct * 100).roundToInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(colors[i]))
                        Text(
                            s.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                        )
                        Text(
                            "$pctInt%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            fmt.format(s.amountMinor, currency),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
