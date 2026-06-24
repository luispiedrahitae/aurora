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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.domain.Money
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.viewmodel.CategorySlice
import io.github.koalaplot.core.pie.DefaultSlice
import io.github.koalaplot.core.pie.PieChart
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi

/**
 * Donut de "gasto por categoría" con koalaplot: agujero central con el total, separación entre
 * slices y leyenda (color · nombre · monto · %) debajo. koalaplot anima el barrido por defecto.
 */
@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun CategoryPieChart(slices: List<CategorySlice>, currency: String, modifier: Modifier = Modifier) {
    if (slices.isEmpty()) return
    val spacing = LocalSpacing.current
    val values = slices.map { it.amountMinor.toFloat() }
    val totalMinor = slices.sumOf { it.amountMinor }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
            PieChart(
                values = values,
                modifier = Modifier.fillMaxWidth(),
                slice = { index -> DefaultSlice(color = Color(slices[index].colorHex), gap = 2f) },
                holeSize = 0.62f,
                holeContent = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Total",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            Money(totalMinor, currency).format(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            slices.forEach { slice -> LegendRow(slice, currency) }
        }
    }
}

@Composable
private fun LegendRow(slice: CategorySlice, currency: String) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(slice.colorHex)),
        )
        Text(slice.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(
            Money(slice.amountMinor, currency).format(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "${(slice.pct * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
