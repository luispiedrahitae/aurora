package com.finanzen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.finanzen.domain.Money
import com.finanzen.viewmodel.CategorySlice
import io.github.koalaplot.core.pie.DefaultSlice
import io.github.koalaplot.core.pie.PieChart
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi

/**
 * Pie de "gasto por categoría" con koalaplot + una leyenda (color · nombre · monto · %) debajo.
 * Sustituye a las barras proporcionales manuales para la composición del gasto.
 */
@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun CategoryPieChart(slices: List<CategorySlice>, currency: String, modifier: Modifier = Modifier) {
    if (slices.isEmpty()) return
    val values = slices.map { it.amountMinor.toFloat() }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
            PieChart(
                values = values,
                modifier = Modifier.fillMaxWidth(),
                slice = { index -> DefaultSlice(color = Color(slices[index].colorHex)) },
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            slices.forEach { slice -> LegendRow(slice, currency) }
        }
    }
}

@Composable
private fun LegendRow(slice: CategorySlice, currency: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(slice.colorHex)),
        )
        Text(slice.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(
            "${Money(slice.amountMinor, currency).format()} · ${(slice.pct * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
