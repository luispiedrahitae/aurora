package com.finanzen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.viewmodel.CategorySlice
import com.finanzen.viewmodel.CategorySpend
import kotlin.math.roundToInt

/**
 * Sección "Gastos por categoría": donut de vistazo rápido (máx. 6 porciones, ver
 * [CategoryDonutChart]) y lista completa expandible con desglose por subcategoría, para el
 * período que ya tenga resuelto la pantalla que la aloja (el mes seleccionado en Análisis, el año
 * seleccionado en Resumen — sin un selector de periodo propio y duplicado). Mismo componente en
 * ambas pantallas, para que lean igual (DESIGN.md, "Trust through consistency").
 */
@Composable
fun CategoryBreakdownSection(spend: List<CategorySpend>, currency: String, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        if (spend.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.PieChart,
                title = "Sin gastos en este período",
                subtitle = "Añade movimientos desde la pestaña Movimientos.",
                modifier = Modifier.padding(spacing.xl),
            )
        } else {
            CategoryDonutChart(spend.map { CategorySlice(it.name, it.amountMinor, it.pct, it.color) }, currency)
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                spend.forEach { CategoryBreakdownRow(it, currency) }
            }
        }
    }
}

/** Fila de categoría (reusa [CategoryProgressRow]); si tiene subcategorías, es tocable y revela su
 * desglose indentado. Sin subcategorías, es una fila plana sin chevron. */
@Composable
private fun CategoryBreakdownRow(spend: CategorySpend, currency: String) {
    var expanded by remember(spend.name) { mutableStateOf(false) }
    val expandable = spend.subcategories.isNotEmpty()
    val spacing = LocalSpacing.current

    FinanceCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (expandable) ({ expanded = !expanded }) else null,
        size = BentoTileSize.Small,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                CategoryProgressRow(
                    name = spend.name,
                    amountMinor = spend.amountMinor,
                    currency = currency,
                    pct = spend.pct,
                    color = categoryColor(spend.name, spend.color),
                    modifier = Modifier.weight(1f),
                )
                if (expandable) {
                    Icon(
                        if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (expanded) "Contraer" else "Expandir",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (expanded) {
                Column(
                    modifier = Modifier.padding(start = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    spend.subcategories.forEach { sub -> SubcategoryRow(sub, currency) }
                }
            }
        }
    }
}

/** Fila liviana de subcategoría: punto de color + nombre + % + monto, igual patrón visual que la
 * leyenda del donut pero más chica — la jerarquía se marca con tamaño e indentación, no con color. */
@Composable
private fun SubcategoryRow(slice: CategorySlice, currency: String) {
    val fmt = LocalMoneyFormat.current
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(categoryColor(slice.name, slice.color)))
        Text(slice.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
        Text("${(slice.pct * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        Text(
            fmt.format(slice.amountMinor, currency),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
