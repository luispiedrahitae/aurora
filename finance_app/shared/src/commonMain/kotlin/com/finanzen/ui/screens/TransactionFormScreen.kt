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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
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
    var categoryId by remember { mutableStateOf(existing?.categoryId) }
    var amount by remember { mutableStateOf(existing?.let { Money(it.amountMinor, it.currency).format() } ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    val dateEpochDay = remember { existing?.date ?: Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays().toLong() }

    val selectedAccount = accounts.firstOrNull { it.id == accountId } ?: accounts.firstOrNull()
    val categoryOptions = categories.filter { it.kind == kind }
    val selectedCategory = categoryOptions.firstOrNull { it.id == categoryId }
    val finance = LocalFinanceColors.current

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
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text("Gasto") }
                SegmentedButton(
                    selected = kind == "INCOME",
                    onClick = {
                        kind = "INCOME"
                        categoryId = null
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text("Ingreso") }
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
                label = "Cuenta",
                options = accounts,
                selected = selectedAccount,
                optionLabel = { "${it.name} (${it.currency})" },
                onSelect = { accountId = it.id },
                placeholder = "Selecciona cuenta",
                emptyHint = "Crea una cuenta primero (pestaña Más › Cuentas).",
            )

            PickerField(
                label = "Categoría (opcional)",
                options = categoryOptions,
                selected = selectedCategory,
                optionLabel = { it.name },
                onSelect = { categoryId = it.id },
                placeholder = "Sin categoría",
                emptyHint = "No hay categorías de ${if (kind == "EXPENSE") "gasto" else "ingreso"}.",
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Nota") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // ponytail: la fecha se fija a hoy en alta y se preserva en edición. Date picker real cuando se necesite editarla.
            Text(
                "Fecha: ${LocalDate.fromEpochDays(dateEpochDay.toInt())}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            error?.let { Text(it, color = finance.expense, style = MaterialTheme.typography.bodySmall) }

            Button(
                onClick = {
                    val account = selectedAccount
                    val minor = Money.parseToMinor(amount)
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
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Guardar") }
        }
    }
}
