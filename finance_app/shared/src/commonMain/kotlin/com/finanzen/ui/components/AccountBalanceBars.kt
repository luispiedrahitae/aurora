package com.finanzen.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.viewmodel.AccountBar
import kotlin.math.abs

/**
 * Barra vertical por cuenta sobre una línea cero compartida: saldos positivos suben en gris
 * neutro, deudas (saldo negativo) bajan en rojo gasto. Cada barra lleva su nombre y su saldo con
 * signo debajo, así el signo/número desambigua el color. Fila desplazable como en ExpenseBarChart.
 */
@Composable
fun AccountBalanceBars(accounts: List<AccountBar>, currency: String, modifier: Modifier = Modifier) {
    if (accounts.isEmpty()) return
    val finance = LocalFinanceColors.current
    val fmt = LocalMoneyFormat.current
    val maxAbs = accounts.maxOf { abs(it.balanceMinor) }.coerceAtLeast(1L)
    val scrollState = rememberScrollState()

    FinanceCard(modifier = modifier.semantics { contentDescription = "Saldos por cuenta" }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                accounts.forEach { acc ->
                    val positive = acc.balanceMinor >= 0
                    val fraction = (abs(acc.balanceMinor).toFloat() / maxAbs.toFloat()).coerceIn(0f, 1f)
                    val animated by animateFloatAsState(targetValue = fraction, animationSpec = tween(400))
                    val barColor = if (positive) finance.neutral else finance.expense
                    Column(
                        modifier = Modifier.width(76.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Zona de barra con la línea cero en el medio; positivos arriba, deudas abajo.
                        Column(modifier = Modifier.height(120.dp).fillMaxWidth()) {
                            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                                if (positive) {
                                    Box(
                                        Modifier
                                            .width(22.dp)
                                            .fillMaxHeight(animated.coerceAtLeast(0.02f))
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(barColor),
                                    )
                                }
                            }
                            Box(Modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.outline))
                            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                                if (!positive) {
                                    Box(
                                        Modifier
                                            .width(22.dp)
                                            .fillMaxHeight(animated.coerceAtLeast(0.02f))
                                            .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                                            .background(barColor),
                                    )
                                }
                            }
                        }
                        AccountTypeAvatar(acc.type, size = 32.dp)
                        Text(
                            acc.name,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            fmt.format(acc.balanceMinor, currency, signed = true),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (positive) finance.neutral else finance.expense,
                            maxLines = 1,
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendSwatch(finance.neutral, "Saldo")
                LegendSwatch(finance.expense, "Deuda")
            }
        }
    }
}

/** Punto de color + etiqueta para las leyendas de los gráficos de cuentas. */
@Composable
private fun LegendSwatch(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
