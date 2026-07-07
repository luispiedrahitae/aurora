package com.finanzen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.viewmodel.CategorySlice

/**
 * Lista de categorías de gasto ordenadas de mayor a menor: el mayor gasto en rojo, el menor en
 * verde (identidad de instancia, no por nombre, para no confundir dos categorías homónimas), el
 * resto en un color neutral. Una sola [FinanceCard] con todas las filas dentro.
 */
@Composable
fun TopExpensesList(slices: List<CategorySlice>, currency: String, modifier: Modifier = Modifier) {
    val finance = LocalFinanceColors.current
    val spacing = LocalSpacing.current
    val max = slices.maxByOrNull { it.amountMinor }
    val min = slices.minByOrNull { it.amountMinor }

    FinanceCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
            slices.forEach { slice ->
                val color = when {
                    slice === max -> finance.expense
                    slice === min -> finance.income
                    else -> finance.neutral
                }
                CategoryProgressRow(
                    name = slice.name,
                    amountMinor = slice.amountMinor,
                    currency = currency,
                    pct = slice.pct,
                    color = color,
                )
            }
        }
    }
}
