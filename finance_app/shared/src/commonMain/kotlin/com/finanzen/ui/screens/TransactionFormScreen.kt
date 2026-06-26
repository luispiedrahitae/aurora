package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finanzen.domain.Money
import com.finanzen.ui.components.PickerField
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.viewmodel.TransactionsViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.compose.viewmodel.koinViewModel

private const val MILLIS_PER_DAY = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    transactionId: Long?,
    onBack: () -> Unit,
    vm: TransactionsViewModel = koinViewModel(),
) {
    val accounts by vm.accounts.collectAsState()
    val categories by vm.categories.collectAsState()

    val existing = remember(transactionId) { transactionId?.let(vm::transactionById) }
    var kind by remember { mutableStateOf(existing?.kind ?: "EXPENSE") }
    var accountId by remember { mutableStateOf(existing?.accountId) }
    var toAccountId by remember { mutableStateOf(existing?.transferAccountId) }
    var categoryId by remember { mutableStateOf(existing?.categoryId) }
    var amount by remember { mutableStateOf(existing?.let { Money(it.amountMinor, it.currency).format() } ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var dateEpochDay by remember {
        mutableStateOf(existing?.date ?: Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays().toLong())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val isTransfer = kind == "TRANSFER"
    val selectedAccount = accounts.firstOrNull { it.id == accountId } ?: accounts.firstOrNull()
    val destOptions = accounts.filter { it.id != selectedAccount?.id }
    val selectedDest = accounts.firstOrNull { it.id == toAccountId }
    val categoryOptions = categories.filter { it.kind == kind }
    val selectedCategory = categoryOptions.firstOrNull { it.id == categoryId }
    val finance = LocalFinanceColors.current

    if (showDatePicker) {
        val dpState = rememberDatePickerStateFor(dateEpochDay)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { dateEpochDay = it / MILLIS_PER_DAY }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } },
        ) { DatePicker(state = dpState) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Nueva transacción" else "Editar transacción") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inner).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = kind == "EXPENSE",
                    onClick = {
                        kind = "EXPENSE"
                        categoryId = null
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                ) { Text("Gasto") }
                SegmentedButton(
                    selected = kind == "INCOME",
                    onClick = {
                        kind = "INCOME"
                        categoryId = null
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                ) { Text("Ingreso") }
                SegmentedButton(
                    selected = isTransfer,
                    onClick = {
                        kind = "TRANSFER"
                        categoryId = null
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                ) { Text("Transfer.") }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Monto") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            PickerField(
                label = if (isTransfer) "Cuenta origen" else "Cuenta",
                options = accounts,
                selected = selectedAccount,
                optionLabel = { "${it.name} (${it.currency})" },
                onSelect = { accountId = it.id },
                placeholder = "Selecciona cuenta",
                emptyHint = "Crea una cuenta primero (pestaña Más › Cuentas).",
            )

            if (isTransfer) {
                PickerField(
                    label = "Cuenta destino",
                    options = destOptions,
                    selected = selectedDest,
                    optionLabel = { "${it.name} (${it.currency})" },
                    onSelect = { toAccountId = it.id },
                    placeholder = "Selecciona destino",
                    emptyHint = "Necesitas al menos dos cuentas para transferir.",
                )
            } else {
                PickerField(
                    label = "Categoría (opcional)",
                    options = categoryOptions,
                    selected = selectedCategory,
                    optionLabel = { it.name },
                    onSelect = { categoryId = it.id },
                    placeholder = "Sin categoría",
                    emptyHint = "No hay categorías de ${if (kind == "EXPENSE") "gasto" else "ingreso"}.",
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Nota") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                Text("  ${LocalDate.fromEpochDays(dateEpochDay.toInt())}")
            }

            error?.let { Text(it, color = finance.expense, style = MaterialTheme.typography.bodySmall) }

            Button(
                onClick = {
                    val account = selectedAccount
                    val minor = Money.parseToMinor(amount)
                    if (isTransfer) {
                        val dest = selectedDest
                        when {
                            account == null || dest == null -> error = "Elige cuenta origen y destino."
                            account.id == dest.id -> error = "Origen y destino deben ser distintos."
                            minor == null || minor <= 0 -> error = "Monto inválido."
                            else -> {
                                vm.saveTransfer(existing?.id, account.id, dest.id, minor, note, dateEpochDay)
                                onBack()
                            }
                        }
                    } else {
                        when {
                            account == null -> error = "Crea una cuenta primero (pestaña Más › … )."
                            minor == null || minor <= 0 -> error = "Monto inválido."
                            else -> {
                                vm.save(
                                    id = existing?.id,
                                    accountId = account.id,
                                    categoryId = categoryId,
                                    amountMinor = minor,
                                    kind = kind,
                                    note = note,
                                    dateEpochDay = dateEpochDay,
                                )
                                onBack()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Guardar") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberDatePickerStateFor(epochDay: Long) = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = epochDay * MILLIS_PER_DAY)
