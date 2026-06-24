package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.db.InstallmentPlan
import com.finanzen.domain.Money
import com.finanzen.viewmodel.CardsViewModel
import org.koin.compose.viewmodel.koinViewModel
import com.finanzen.db.Card as CardEntity

@Composable
fun CardsScreen(vm: CardsViewModel = koinViewModel()) {
    val cards by vm.cards.collectAsState()
    val plans by vm.plans.collectAsState()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.addSample() },
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text(if (cards.isEmpty()) "Tarjeta" else "Cuota") },
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SectionHeader("Tarjetas (${cards.size})") }
            if (cards.isEmpty()) {
                item {
                    EmptyHint("Sin tarjetas. Usa + para añadir una de muestra (crédito).")
                }
            } else {
                items(cards, key = { "card-${it.id}" }) { card ->
                    val planCount = plans.count { it.cardId == card.id }
                    CardItem(card, planCount, onDelete = { vm.deleteCard(card.id) })
                }
            }

            item { SectionHeader("Cuotas activas (${plans.size})") }
            if (plans.isEmpty()) {
                item { EmptyHint("Sin planes de cuotas. Crea una tarjeta primero.") }
            } else {
                items(plans, key = { "plan-${it.id}" }) { plan ->
                    val cardLast4 = cards.firstOrNull { it.id == plan.cardId }?.last4 ?: "????"
                    InstallmentItem(plan, cardLast4, onDelete = { vm.deletePlan(plan.id) })
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CardItem(card: CardEntity, planCount: Int, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${card.network} •••• ${card.last4}", fontWeight = FontWeight.SemiBold)
                val type = if (card.creditLimitMinor != null) "Crédito" else "Débito"
                val cutoff = card.cutoffDay?.let { "corte $it" } ?: ""
                val due = card.dueDay?.let { "pago $it" } ?: ""
                val limit = card.creditLimitMinor?.let {
                    " · cupo " + Money(it, "USD").format()
                } ?: ""
                Text(
                    listOf(type + limit, cutoff, due).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (planCount > 0) {
                    Text(
                        "$planCount plan(es) de cuotas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Eliminar")
            }
        }
    }
}

@Composable
private fun InstallmentItem(plan: InstallmentPlan, cardLast4: String, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(plan.description.ifBlank { "Plan #${plan.id}" }, fontWeight = FontWeight.SemiBold)
                val perInstallment = plan.totalAmountMinor / plan.installments
                Text(
                    "${Money(plan.totalAmountMinor, "USD").format()} en ${plan.installments} cuotas " +
                        "(${Money(perInstallment, "USD").format()}/mes) · tarjeta ••••$cardLast4",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (plan.interestRate > 0.0) {
                    Text(
                        "interés ${plan.interestRate}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Eliminar")
            }
        }
    }
}
