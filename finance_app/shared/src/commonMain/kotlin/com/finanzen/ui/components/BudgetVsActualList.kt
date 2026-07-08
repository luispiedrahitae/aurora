package com.finanzen.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import com.finanzen.viewmodel.BudgetRow
import kotlin.math.roundToInt

/**
 * Una tarjeta con N filas de "presupuesto vs gastado". Cada fila: avatar + nombre, una barra de
 * progreso redondeada (mismo lenguaje que CategoryProgressRow) con una **marca vertical de límite
 * al 100%** — el elemento que CategoryProgressRow no tiene y motiva este composable nuevo. El color
 * del relleno codifica el umbral (verde <80%, ámbar 80–100%, rojo >=100%) y SIEMPRE va acompañado
 * del texto "Gastado X de Y" y el porcentaje en el mismo color, así el color no es la única señal.
 */
@Composable
fun BudgetVsActualList(rows: List<BudgetRow>, currency: String, modifier: Modifier = Modifier) {
    if (rows.isEmpty()) return
    val fmt = LocalMoneyFormat.current

    FinanceCard(modifier = modifier.semantics { contentDescription = "Presupuesto contra gastado por categoría" }) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            // Leyenda de umbrales: explica el significado de cada color.
            Text(
                "Verde <80% · Ámbar 80–100% · Rojo >=100%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            rows.forEach { row -> BudgetRowItem(row, currency, fmt) }
        }
    }
}

@Composable
private fun BudgetRowItem(row: BudgetRow, currency: String, fmt: com.finanzen.ui.theme.MoneyFormat) {
    val finance = LocalFinanceColors.current
    val limit = row.limitMinor.coerceAtLeast(1L)
    val ratio = row.spentMinor.toFloat() / limit.toFloat()
    val pctInt = (ratio * 100).roundToInt()
    val barColor = when {
        ratio < 0.8f -> finance.income
        ratio < 1.0f -> finance.warning
        else -> finance.expense
    }
    val animated by animateFloatAsState(targetValue = ratio.coerceIn(0f, 1f), animationSpec = tween(400))

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CategoryAvatar(row.icon, categoryColor(row.categoryName, row.color), size = 32.dp)
            Text(
                row.categoryName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Barra: pista + relleno + marca vertical de límite al 100% (borde derecho).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(barColor),
            )
            // Marca de límite: línea vertical de 2dp a lo alto de la pista, en el 100%.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outline),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Gastado ${fmt.format(row.spentMinor, currency)} de ${fmt.format(row.limitMinor, currency)}",
                style = MaterialTheme.typography.labelMedium,
                color = barColor,
                fontWeight = FontWeight.Medium,
            )
            Text(
                "$pctInt%",
                style = MaterialTheme.typography.labelMedium,
                color = barColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
