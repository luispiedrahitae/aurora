package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finanzen.domain.InstallmentMath
import com.finanzen.ui.components.CategoryAvatar
import com.finanzen.ui.components.MoneyField
import com.finanzen.ui.components.PickerField
import com.finanzen.ui.components.accountTypeLabel
import com.finanzen.ui.format.formatFechaCorta
import com.finanzen.ui.theme.LocalDateLocale
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.viewmodel.TransactionsViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.compose.viewmodel.koinViewModel

private const val MILLIS_PER_DAY = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    transactionId: Long?,
    onBack: () -> Unit,
    initialKind: String? = null,
    vm: TransactionsViewModel = koinViewModel(),
) {
    val accounts by vm.accounts.collectAsState()
    val allAccounts by vm.allAccounts.collectAsState()
    val categories by vm.categories.collectAsState()
    val dateLocale = LocalDateLocale.current

    val existing = remember(transactionId) { transactionId?.let(vm::transactionById) }
    var kind by remember { mutableStateOf(existing?.kind ?: initialKind ?: "EXPENSE") }
    var accountId by remember { mutableStateOf(existing?.accountId) }
    var toAccountId by remember { mutableStateOf(existing?.transferAccountId) }
    var categoryId by remember { mutableStateOf(existing?.categoryId) }
    var subcategoryId by remember { mutableStateOf<Long?>(null) }
    var amountMinor by remember { mutableStateOf(existing?.amountMinor ?: 0L) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAddSubcategory by remember { mutableStateOf(false) }
    var newSubcategoryName by remember { mutableStateOf("") }
    var dateEpochDay by remember {
        mutableStateOf(existing?.date ?: Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays().toLong())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var cuotasText by remember { mutableStateOf("") }

    val isTransfer = kind == "TRANSFER"
    val selectedAccount = accounts.firstOrNull { it.id == accountId }
        ?: existing?.let { allAccounts.firstOrNull { a -> a.id == accountId } }
        ?: accounts.firstOrNull()
    val destOptions = accounts.filter { it.id != selectedAccount?.id }
    val selectedDest = accounts.firstOrNull { it.id == toAccountId }
        ?: existing?.let { allAccounts.firstOrNull { a -> a.id == toAccountId } }
    val categoryOptions = categories.filter { it.kind == kind && it.parentId == null }
    val selectedCategory = categoryOptions.firstOrNull { it.id == categoryId }
    val subcategoryOptions = categories.filter { it.parentId == selectedCategory?.id }
    val selectedSubcategory = subcategoryOptions.firstOrNull { it.id == subcategoryId }
    val finance = LocalFinanceColors.current
    val haptic = LocalHapticFeedback.current

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
                title = { Text(if (existing == null) "Nuevo movimiento" else "Editar movimiento") },
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
                        subcategoryId = null
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    icon = {},
                ) { Text("Gasto") }
                SegmentedButton(
                    selected = kind == "INCOME",
                    onClick = {
                        kind = "INCOME"
                        categoryId = null
                        subcategoryId = null
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    icon = {},
                ) { Text("Ingreso") }
                SegmentedButton(
                    selected = isTransfer,
                    onClick = {
                        kind = "TRANSFER"
                        categoryId = null
                        subcategoryId = null
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    icon = {},
                ) { Text("Transferencia") }
            }

            MoneyField(
                amountMinor = amountMinor,
                onAmountChange = { amountMinor = it },
                currencyCode = selectedAccount?.currency ?: "",
                label = "Monto",
                modifier = Modifier.fillMaxWidth(),
            )

            PickerField(
                label = if (isTransfer) "Cuenta origen" else "Cuenta",
                options = accounts,
                selected = selectedAccount,
                optionLabel = { "${it.name} (${it.currency})" },
                trailingLabel = { accountTypeLabel(it.type) },
                onSelect = { accountId = it.id },
                placeholder = "Selecciona cuenta",
                emptyHint = "Crea una cuenta primero (pestaña Cuentas).",
            )

            if (isTransfer) {
                PickerField(
                    label = "Cuenta destino",
                    options = destOptions,
                    selected = selectedDest,
                    optionLabel = { "${it.name} (${it.currency})" },
                    trailingLabel = { accountTypeLabel(it.type) },
                    onSelect = { toAccountId = it.id },
                    placeholder = "Selecciona destino",
                    emptyHint = "Necesitas al menos dos cuentas para transferir.",
                )
            } else {
                PickerField(
                    label = "Categoría",
                    options = categoryOptions,
                    selected = selectedCategory,
                    optionLabel = { it.name },
                    onSelect = {
                        categoryId = it.id
                        subcategoryId = null
                    },
                    placeholder = "Selecciona categoría",
                    emptyHint = "No hay categorías de ${if (kind == "EXPENSE") "gasto" else "ingreso"}.",
                    leadingContent = { cat -> CategoryAvatar(cat.icon) },
                )
            }

            if (!isTransfer && selectedCategory != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PickerField(
                        label = "Subcategoría (opcional)",
                        options = subcategoryOptions,
                        selected = selectedSubcategory,
                        optionLabel = { it.name },
                        onSelect = { subcategoryId = it.id },
                        placeholder = "Sin subcategoría",
                        emptyHint = "Aún no hay subcategorías en ${selectedCategory.name}.",
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { showAddSubcategory = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = "Nueva subcategoría")
                    }
                }
            }

            // Cuotas: solo para gastos con tarjeta de crédito. El interés se toma de la tarjeta,
            // no se pide aquí — Card.interestRate ya lo tiene guardado.
            if (!isTransfer && kind == "EXPENSE" && selectedAccount?.type == "CREDIT") {
                val installments = cuotasText.toLongOrNull()?.coerceIn(1, MAX_INSTALLMENTS) ?: 1
                OutlinedTextField(
                    value = cuotasText,
                    onValueChange = { cuotasText = it },
                    label = { Text("Cuotas") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (installments > 1 && amountMinor > 0) {
                    val rate = vm.cardFor(selectedAccount.id)?.interestRate ?: 0.0
                    val monthly = InstallmentMath.monthlyPaymentMinor(amountMinor, installments, rate)
                    Text(
                        "Se registrará la cuota 1 (${LocalMoneyFormat.current.format(monthly, selectedAccount.currency)}) " +
                            "este mes. Las próximas ${installments - 1} cuotas se agregan automáticamente cada mes " +
                            "hasta completar el plan, con el interés de tu tarjeta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(formatFechaCorta(dateEpochDay, dateLocale))
            }

            error?.let { Text(it, color = finance.expense, style = MaterialTheme.typography.bodySmall) }

            Button(
                onClick = {
                    val account = selectedAccount
                    val minor = amountMinor
                    if (isTransfer) {
                        val dest = selectedDest
                        when {
                            account == null || dest == null -> error = "Elige cuenta origen y destino."
                            account.id == dest.id -> error = "Origen y destino deben ser distintos."
                            minor <= 0 -> error = "Monto inválido."
                            else -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.saveTransfer(existing?.id, account.id, dest.id, minor, "", dateEpochDay)
                                onBack()
                            }
                        }
                    } else {
                        when {
                            account == null -> error = "Crea una cuenta primero (pestaña Cuentas)."
                            categoryId == null -> error = "Elige una categoría."
                            minor <= 0 -> error = "Monto inválido."
                            else -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val movementName = selectedSubcategory?.name ?: selectedCategory?.name ?: ""
                                vm.save(
                                    id = existing?.id,
                                    accountId = account.id,
                                    categoryId = categoryId,
                                    amountMinor = minor,
                                    kind = kind,
                                    note = movementName,
                                    dateEpochDay = dateEpochDay,
                                    installments = cuotasText.toLongOrNull()?.coerceAtLeast(1) ?: 1,
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

    if (showAddSubcategory && selectedCategory != null) {
        val existingNames = subcategoryOptions.map { it.name.trim().lowercase() }.toSet()
        val nameTaken = newSubcategoryName.isNotBlank() && newSubcategoryName.trim().lowercase() in existingNames
        AlertDialog(
            onDismissRequest = {
                showAddSubcategory = false
                newSubcategoryName = ""
            },
            title = { Text("Nueva subcategoría en ${selectedCategory.name}") },
            text = {
                OutlinedTextField(
                    value = newSubcategoryName,
                    onValueChange = { newSubcategoryName = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    isError = nameTaken,
                    supportingText = if (nameTaken) {
                        { Text("Ya existe una subcategoría con ese nombre") }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newSubcategoryName.isNotBlank() && !nameTaken,
                    onClick = {
                        subcategoryId = vm.addSubcategory(newSubcategoryName, kind, selectedCategory.id)
                        showAddSubcategory = false
                        newSubcategoryName = ""
                    },
                ) { Text("Añadir") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddSubcategory = false
                    newSubcategoryName = ""
                }) { Text("Cancelar") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberDatePickerStateFor(epochDay: Long) = androidx.compose.material3.rememberDatePickerState(
    initialSelectedDateMillis = epochDay * MILLIS_PER_DAY,
    yearRange = REASONABLE_YEAR_RANGE,
)

private val REASONABLE_YEAR_RANGE = 1990..2100
private const val MAX_INSTALLMENTS = 360L
