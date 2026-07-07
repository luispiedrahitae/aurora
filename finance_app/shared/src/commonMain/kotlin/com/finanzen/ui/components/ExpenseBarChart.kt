package com.finanzen.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.viewmodel.MonthExpense

/**
 * Gasto de cada uno de los últimos 12 meses. Tap en una barra la selecciona y muestra su monto
 * exacto arriba (mismo mecanismo en Android/iOS/Desktop, no hay hover real en touch). El mes más
 * reciente empieza seleccionado.
 */
@Composable
fun ExpenseBarChart(months: List<MonthExpense>, currency: String, modifier: Modifier = Modifier) {
    if (months.isEmpty()) return
    var selectedIndex by remember(months) { mutableStateOf(months.lastIndex) }
    val maxExpense = (months.maxOfOrNull { it.expenseMinor } ?: 0L).coerceAtLeast(1L)
    val scrollState = rememberScrollState()
    LaunchedEffect(months, scrollState.maxValue) { scrollState.scrollTo(scrollState.maxValue) }

    FinanceCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().height(140.dp).horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            // Margen extra a los costados: el monto de la barra seleccionada es más ancho que la
            // columna de 32.dp y, sin esto, se recorta contra el borde de la tarjeta en la primera
            // o última barra.
            Spacer(Modifier.width(24.dp))
            months.forEachIndexed { index, month ->
                ExpenseBar(
                    month = month,
                    maxExpense = maxExpense,
                    currency = currency,
                    selected = index == selectedIndex,
                    onClick = { selectedIndex = index },
                )
            }
            Spacer(Modifier.width(24.dp))
        }
    }
}

@Composable
private fun ExpenseBar(month: MonthExpense, maxExpense: Long, currency: String, selected: Boolean, onClick: () -> Unit) {
    val finance = LocalFinanceColors.current
    val fmt = LocalMoneyFormat.current
    val targetFraction = (month.expenseMinor.toFloat() / maxExpense.toFloat()).coerceIn(0f, 1f)
    val fraction by animateFloatAsState(targetValue = targetFraction, animationSpec = tween(400))
    val barColor = if (selected) finance.expense else finance.expense.copy(alpha = 0.5f)

    Column(
        // El monto exacto solo se muestra en la barra seleccionada y puede ser más ancho que
        // 32.dp; con ancho fijo el texto se trunca con "...". Se deja crecer por contenido en
        // ese caso en vez de forzar el mismo ancho que las barras no seleccionadas.
        modifier = Modifier
            .then(if (selected) Modifier.widthIn(min = 32.dp) else Modifier.width(32.dp))
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            if (selected) fmt.format(month.expenseMinor, currency) else "",
            style = MaterialTheme.typography.labelSmall,
            color = finance.expense,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .padding(top = 2.dp, bottom = 6.dp)
                .width(20.dp)
                .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(barColor),
        )
        Text(month.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
