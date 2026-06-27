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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.finanzen.db.Account
import com.finanzen.db.Card
import com.finanzen.db.InstallmentPlan
import com.finanzen.domain.InstallmentMath
import com.finanzen.domain.Money
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.LabeledDropdown
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.viewmodel.AccountsViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.compose.viewmodel.koinViewModel

private fun typeLabel(type: String): String = when (type) {
    "CASH" -> "Efectivo"
    "DEBIT" -> "Débito"
    "SAVINGS" -> "Ahorros"
    "CREDIT" -> "Crédito"
    else -> type
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsTabScreen(vm: AccountsViewModel = koinViewModel()) {
    val accounts by vm.accounts.collectAsState()
    val balances by vm.balances.collectAsState()
    val cards by vm.cards.collectAsState()
    val plansByAccount by vm.plansByAccount.collectAsState()
    var showForm by remember { mutableStateOf(false) }

    if (showForm) {
        AccountFormDialog(
            types = vm.accountTypes,
            parseAmount = vm::parseAmountToMinor,
            onDismiss = { showForm = false },
            onConfirm = { type, name, amount, limit, cutoff, due, interest ->
                vm.addAccount(type, name, amount, limit, cutoff, due, interest)
                showForm = false
            },
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Cuentas") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showForm = true },
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text("Cuenta") },
            )
        },
    ) { inner ->
        if (accounts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    title = "Sin cuentas",
                    subtitle = "Usa el botón + para crear tu primera cuenta.",
                    modifier = Modifier.padding(32.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(accounts, key = { it.id }) { account ->
                    AccountRow(
                        account = account,
                        balanceMinor = balances[account.id] ?: account.openingBalanceMinor,
                        card = cards[account.id],
                        plans = plansByAccount[account.id].orEmpty(),
                        onDelete = { vm.delete(account.id) },
                        onDeletePlan = { vm.deletePlan(it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountRow(
    account: Account,
    balanceMinor: Long,
    card: Card?,
    plans: List<InstallmentPlan>,
    onDelete: () -> Unit,
    onDeletePlan: (Long) -> Unit,
) {
    val isCredit = account.type == "CREDIT"
    FinanceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(account.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        accountSubtitle(account, card, balanceMinor),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${Money(balanceMinor, account.currency).format()} ${account.currency}",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (balanceMinor < 0) LocalFinanceColors.current.expense else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        if (isCredit) "saldo (deuda si −)" else "saldo actual",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Eliminar")
                }
            }
            if (plans.isNotEmpty()) {
                HorizontalDivider()
                val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays().toLong() }
                plans.forEach { plan -> PlanRow(plan, account.currency, today, onDelete = { onDeletePlan(plan.id) }) }
            }
        }
    }
}

/** Una cuota dentro de la tarjeta de la cuenta: cuota/mes, progreso e interés. */
@Composable
private fun PlanRow(plan: InstallmentPlan, currency: String, todayEpochDay: Long, onDelete: () -> Unit) {
    val monthly = InstallmentMath.monthlyPaymentMinor(plan.totalAmountMinor, plan.installments, plan.interestRate)
    val elapsed = InstallmentMath.elapsedInstallments(plan.startDate, todayEpochDay, plan.installments)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(plan.description.ifBlank { "Compra a cuotas" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            val interest = if (plan.interestRate > 0.0) " · ${plan.interestRate}%" else ""
            Text(
                "${Money(monthly, currency).format()}/mes · $elapsed/${plan.installments} cuotas$interest",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, contentDescription = "Eliminar cuota")
        }
    }
}

/** Subtítulo según el tipo: crédito muestra cupo/disponible + corte/pago; el resto, tipo + inicial. */
private fun accountSubtitle(account: Account, card: Card?, balanceMinor: Long): String {
    if (account.type != "CREDIT" || card == null) {
        return "${typeLabel(account.type)} · inicial ${Money(account.openingBalanceMinor, account.currency).format()}"
    }
    val parts = mutableListOf("Crédito")
    card.creditLimitMinor?.let { limit ->
        // Disponible = cupo − deuda; la deuda es el saldo negativo (balance ≤ 0).
        val available = limit + balanceMinor
        parts += "cupo ${Money(limit, account.currency).format()}"
        parts += "disp. ${Money(available, account.currency).format()}"
    }
    card.cutoffDay?.let { parts += "corte $it" }
    card.dueDay?.let { parts += "pago $it" }
    card.interestRate?.let { if (it > 0.0) parts += "int. $it%" }
    return parts.joinToString(" · ")
}

/** Alta de cuenta: el formulario cambia según el tipo elegido. */
@Composable
private fun AccountFormDialog(
    types: List<String>,
    parseAmount: (String) -> Long?,
    onDismiss: () -> Unit,
    onConfirm: (type: String, name: String, amountMinor: Long, limit: Long?, cutoff: Long?, due: Long?, interest: Double?) -> Unit,
) {
    var type by remember { mutableStateOf(types.first()) }
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("") }
    var cutoff by remember { mutableStateOf("") }
    var due by remember { mutableStateOf("") }
    var interest by remember { mutableStateOf("") }

    val isCredit = type == "CREDIT"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva cuenta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledDropdown(
                    label = "Tipo",
                    options = types,
                    selected = type,
                    optionLabel = { typeLabel(it) },
                    onSelect = { type = it },
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isCredit) {
                    DecimalField(limit, "Cupo (ej. 5000.00)") { limit = it }
                    NumberField(cutoff, "Día de corte (opcional, 1–31)", maxLen = 2) { cutoff = it }
                    NumberField(due, "Día de pago (opcional, 1–31)", maxLen = 2) { due = it }
                    DecimalField(interest, "Tasa de interés % (opcional)") { interest = it }
                } else {
                    DecimalField(amount, "Monto inicial (ej. 100.00)") { amount = it }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        type,
                        name,
                        if (isCredit) 0 else (parseAmount(amount) ?: 0),
                        if (isCredit) parseAmount(limit) else null,
                        if (isCredit) cutoff.toLongOrNull() else null,
                        if (isCredit) due.toLongOrNull() else null,
                        if (isCredit) interest.toDoubleOrNull() else null,
                    )
                },
                enabled = name.isNotBlank(),
            ) { Text("Crear cuenta") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun DecimalField(value: String, label: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
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
