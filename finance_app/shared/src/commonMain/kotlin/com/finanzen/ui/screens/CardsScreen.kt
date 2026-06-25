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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finanzen.db.Card
import com.finanzen.db.InstallmentPlan
import com.finanzen.domain.Money
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.LabeledDropdown
import com.finanzen.ui.components.SectionHeader
import com.finanzen.viewmodel.CardsViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import com.finanzen.db.Card as CardEntity

@Composable
fun CardsScreen(vm: CardsViewModel = koinViewModel()) {
    val cards by vm.cards.collectAsState()
    val plans by vm.plans.collectAsState()
    val currency = vm.baseCurrency
    var showCardForm by remember { mutableStateOf(false) }
    var showPlanForm by remember { mutableStateOf(false) }

    if (showCardForm) {
        CardFormDialog(
            networks = vm.networks,
            parseAmount = vm::parseAmountToMinor,
            onDismiss = { showCardForm = false },
            onConfirm = { network, last4, isCredit, limitMinor, cutoff, due ->
                vm.addCard(network, last4, isCredit, limitMinor, cutoff, due)
                showCardForm = false
            },
        )
    }
    if (showPlanForm) {
        PlanFormDialog(
            cards = cards,
            parseAmount = vm::parseAmountToMinor,
            onDismiss = { showPlanForm = false },
            onConfirm = { cardId, desc, totalMinor, installments, interest ->
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toEpochDays().toLong()
                vm.addPlan(cardId, desc, totalMinor, installments, interest, today)
                showPlanForm = false
            },
        )
    }

    Scaffold(
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExtendedFloatingActionButton(
                    onClick = { showCardForm = true },
                    icon = { Icon(Icons.Outlined.Add, null) },
                    text = { Text("Tarjeta") },
                )
                if (cards.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = { showPlanForm = true },
                        icon = { Icon(Icons.Outlined.Add, null) },
                        text = { Text("Cuota") },
                    )
                }
            }
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SectionHeader("Tarjetas (${cards.size})") }
            if (cards.isEmpty()) {
                item { EmptyHint("Sin tarjetas. Usa + para añadir una.") }
            } else {
                items(cards, key = { "card-${it.id}" }) { card ->
                    val planCount = plans.count { it.cardId == card.id }
                    CardItem(card, planCount, currency, onDelete = { vm.deleteCard(card.id) })
                }
            }

            item { SectionHeader("Cuotas activas (${plans.size})") }
            if (plans.isEmpty()) {
                item { EmptyHint("Sin planes de cuotas. Crea una tarjeta primero.") }
            } else {
                items(plans, key = { "plan-${it.id}" }) { plan ->
                    val cardLast4 = cards.firstOrNull { it.id == plan.cardId }?.last4 ?: "????"
                    InstallmentItem(plan, cardLast4, currency, onDelete = { vm.deletePlan(plan.id) })
                }
            }
        }
    }
}

/** Alta de tarjeta: red, últimos 4, crédito/débito y (si crédito) cupo, día de corte y de pago. */
@Composable
private fun CardFormDialog(
    networks: List<String>,
    parseAmount: (String) -> Long?,
    onDismiss: () -> Unit,
    onConfirm: (network: String, last4: String, isCredit: Boolean, limitMinor: Long?, cutoff: Long?, due: Long?) -> Unit,
) {
    var network by remember { mutableStateOf(networks.first()) }
    var last4 by remember { mutableStateOf("") }
    var isCredit by remember { mutableStateOf(true) }
    var limit by remember { mutableStateOf("") }
    var cutoff by remember { mutableStateOf("") }
    var due by remember { mutableStateOf("") }

    val last4Valid = last4.length == 4
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva tarjeta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledDropdown(
                    label = "Red",
                    options = networks,
                    selected = network,
                    optionLabel = { it },
                    onSelect = { network = it },
                )
                NumberField(last4, "Últimos 4 dígitos", maxLen = 4) { last4 = it }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(checked = isCredit, onCheckedChange = { isCredit = it })
                    Text(if (isCredit) "Crédito" else "Débito")
                }
                if (isCredit) {
                    OutlinedTextField(
                        value = limit,
                        onValueChange = { limit = it },
                        label = { Text("Cupo (ej. 5000.00)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    NumberField(cutoff, "Día de corte (1–31)", maxLen = 2) { cutoff = it }
                    NumberField(due, "Día de pago (1–31)", maxLen = 2) { due = it }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        network,
                        last4,
                        isCredit,
                        if (isCredit) parseAmount(limit) else null,
                        cutoff.toLongOrNull(),
                        due.toLongOrNull(),
                    )
                },
                enabled = last4Valid,
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Alta de plan de cuotas sobre una tarjeta existente. */
@Composable
private fun PlanFormDialog(
    cards: List<Card>,
    parseAmount: (String) -> Long?,
    onDismiss: () -> Unit,
    onConfirm: (cardId: Long, description: String, totalMinor: Long, installments: Long, interest: Double) -> Unit,
) {
    var selectedCard by remember { mutableStateOf(cards.first()) }
    var description by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var installments by remember { mutableStateOf("12") }
    var interest by remember { mutableStateOf("0") }

    val totalMinor = parseAmount(total)
    val installmentsValid = installments.toLongOrNull()?.let { it > 0 } == true
    val valid = description.isNotBlank() && totalMinor != null && totalMinor > 0 && installmentsValid
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo plan de cuotas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledDropdown(
                    label = "Tarjeta",
                    options = cards,
                    selected = selectedCard,
                    optionLabel = { "${it.network} ••••${it.last4}" },
                    onSelect = { selectedCard = it },
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = total,
                    onValueChange = { total = it },
                    label = { Text("Monto total (ej. 1200.00)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                NumberField(installments, "Número de cuotas", maxLen = 3) { installments = it }
                OutlinedTextField(
                    value = interest,
                    onValueChange = { interest = it },
                    label = { Text("Interés % (0 si no aplica)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedCard.id, description.trim(), totalMinor!!, installments.toLong(), interest.toDoubleOrNull() ?: 0.0)
                },
                enabled = valid,
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun NumberField(value: String, label: String, maxLen: Int, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(maxLen)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
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
private fun CardItem(card: CardEntity, planCount: Int, currency: String, onDelete: () -> Unit) {
    FinanceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${card.network} •••• ${card.last4}", fontWeight = FontWeight.SemiBold)
                val type = if (card.creditLimitMinor != null) "Crédito" else "Débito"
                val cutoff = card.cutoffDay?.let { "corte $it" } ?: ""
                val due = card.dueDay?.let { "pago $it" } ?: ""
                val limit = card.creditLimitMinor?.let {
                    " · cupo " + Money(it, currency).format()
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
private fun InstallmentItem(plan: InstallmentPlan, cardLast4: String, currency: String, onDelete: () -> Unit) {
    FinanceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(plan.description.ifBlank { "Plan #${plan.id}" }, fontWeight = FontWeight.SemiBold)
                val perInstallment = plan.totalAmountMinor / plan.installments
                Text(
                    "${Money(plan.totalAmountMinor, currency).format()} en ${plan.installments} cuotas " +
                        "(${Money(perInstallment, currency).format()}/mes) · tarjeta ••••$cardLast4",
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
