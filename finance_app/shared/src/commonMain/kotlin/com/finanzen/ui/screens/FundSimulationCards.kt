package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finanzen.domain.FundPlan
import com.finanzen.domain.FundProjector
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.GroupCard
import com.finanzen.ui.components.ListRow
import com.finanzen.ui.components.MoneyText
import com.finanzen.ui.components.RowDivider
import com.finanzen.ui.components.SageButton
import com.finanzen.ui.components.SectionHeader
import com.finanzen.ui.format.formatPercent
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSageType

/** Invitación a simular un fondo, al pie de Fondos de inversión. */
@Composable
internal fun SimulateFundCard(onClick: () -> Unit) {
    FinanceCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("¿Cuánto podrías tener en unos años?", style = LocalSageType.current.titleCard)
            Text(
                "Proyecta un fondo con tus aportes, la rentabilidad esperada y sus comisiones, y compáralo con otro.",
                style = LocalSageType.current.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SageButton("Simular un fondo", onClick = onClick, icon = LucideIcons.TrendingUp, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Simulaciones guardadas: aporte y plazo, y el saldo final del escenario principal. */
@Composable
internal fun SavedSimulations(simulations: List<FundPlan>, onOpen: (Long) -> Unit) {
    val money = LocalMoneyFormat.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Simulaciones guardadas")
        GroupCard {
            simulations.forEachIndexed { i, plan ->
                if (i > 0) RowDivider()
                val finalBalance = remember(plan) { FundProjector.project(plan.mainScenario().second).finalBalanceMinor }
                val years = plan.months / MONTHS_IN_YEAR
                ListRow(
                    title = plan.name,
                    subtitle = "${money.format(plan.monthlyMinor, plan.currency)} al mes · $years ${if (years == 1) "año" else "años"} · " +
                        "${formatPercent(plan.mainRate)} anual",
                    onClick = { plan.id?.let(onOpen) },
                    trailing = { MoneyText(finalBalance, plan.currency, style = LocalSageType.current.amountMd) },
                )
            }
        }
    }
}
