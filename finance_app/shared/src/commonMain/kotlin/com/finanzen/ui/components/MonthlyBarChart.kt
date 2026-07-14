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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.viewmodel.MonthAmount

/**
 * Barras verticales, un mes por columna (12 puntos, año seleccionado). Tocar una barra la
 * selecciona y muestra su valor exacto como titular arriba; por defecto empieza en el mes más
 * reciente (mismo mecanismo de "tap para seleccionar" que [NetWorthAreaChart]/`IncomeExpenseBarChart`).
 * Un único [color] para toda la serie — la barra seleccionada va a color pleno, el resto atenuadas.
 */
@Composable
fun MonthlyBarChart(points: List<MonthAmount>, currency: String, color: Color, title: String, modifier: Modifier = Modifier) {
    if (points.isEmpty()) return
    val fmt = LocalMoneyFormat.current
    var selectedIndex by remember(points) { mutableStateOf(points.lastIndex) }
    val maxValue = points.maxOf { it.amountMinor }.coerceAtLeast(1L)
    val n = points.size
    val sel = points[selectedIndex]

    FinanceCard(modifier = modifier.semantics { contentDescription = "$title por mes" }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    fmt.format(sel.amountMinor, currency),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
                Text(
                    "$title · ${sel.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(Modifier.fillMaxWidth().height(160.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    val padTop = 12f
                    val padBottom = 12f
                    val usableH = (size.height - padTop - padBottom).coerceAtLeast(1f)
                    val cw = size.width / n
                    val barWidth = cw * 0.5f
                    val bottom = size.height - padBottom
                    points.forEachIndexed { i, p ->
                        val frac = (p.amountMinor.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
                        val barHeight = frac * usableH
                        val left = i * cw + (cw - barWidth) / 2f
                        val alpha = if (i == selectedIndex) 1f else 0.4f
                        drawRoundRect(
                            color = color.copy(alpha = alpha),
                            topLeft = Offset(left, bottom - barHeight),
                            size = Size(barWidth, barHeight.coerceAtLeast(2f)),
                            cornerRadius = CornerRadius(6f, 6f),
                        )
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
