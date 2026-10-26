package com.finanzen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.ui.theme.PillShape
import com.finanzen.viewmodel.AccountBar
import kotlin.math.abs

/**
 * "Saldos por cuenta": nombre + saldo por fila, con una barra delgada debajo dimensionada al %
 * del saldo (en valor absoluto) frente a la cuenta con mayor saldo — igual que `Sage.dc.html`
 * (lista vertical con barra de progreso, sin avatares ni scroll horizontal).
 */
@Composable
fun AccountBalanceBars(accounts: List<AccountBar>, currency: String, modifier: Modifier = Modifier) {
    if (accounts.isEmpty()) return
    val fmt = LocalMoneyFormat.current
    val spacing = LocalSpacing.current
    val maxAbs = accounts.maxOf { abs(it.balanceMinor) }.coerceAtLeast(1L)

    FinanceCard(modifier = modifier.semantics { contentDescription = "Saldos por cuenta" }) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            accounts.forEach { acc ->
                val fraction = (abs(acc.balanceMinor).toFloat() / maxAbs.toFloat()).coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(acc.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            fmt.format(acc.balanceMinor, currency, signed = true),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(PillShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(fraction).height(6.dp).clip(PillShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        }
    }
}
