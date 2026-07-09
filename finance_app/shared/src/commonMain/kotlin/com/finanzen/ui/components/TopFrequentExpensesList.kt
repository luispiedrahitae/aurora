package com.finanzen.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.PillShape
import com.finanzen.viewmodel.FrequentExpense

/**
 * Una tarjeta con N filas de gastos más frecuentes. Cada fila: nombre + chip "×N" con el número
 * de veces (el conteo va como texto, no solo por longitud de barra), monto formateado como
 * secundario, y una mini barra de frecuencia relativa (color neutro). El número dentro del chip y
 * el monto hacen que ninguna señal dependa solo del color/longitud.
 */
@Composable
fun TopFrequentExpensesList(items: List<FrequentExpense>, currency: String, modifier: Modifier = Modifier) {
    if (items.isEmpty()) return
    val finance = LocalFinanceColors.current
    val fmt = LocalMoneyFormat.current
    val maxCount = items.maxOf { it.count }.coerceAtLeast(1)

    FinanceCard(modifier = modifier.semantics { contentDescription = "Gastos más frecuentes" }) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items.forEach { item ->
                val fraction = item.count.toFloat() / maxCount.toFloat()
                val animated by animateFloatAsState(targetValue = fraction.coerceIn(0f, 1f), animationSpec = tween(400))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            item.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Surface(shape = PillShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Text(
                                "×${item.count}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            fmt.format(item.amountMinor, currency),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animated.coerceAtLeast(0.02f))
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(finance.neutral),
                        )
                    }
                }
            }
        }
    }
}
