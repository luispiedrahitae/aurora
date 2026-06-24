package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.finanzen.db.Account
import com.finanzen.domain.Money
import com.finanzen.ui.components.LabeledDropdown
import com.finanzen.viewmodel.AccountsViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onBack: () -> Unit,
    vm: AccountsViewModel = koinViewModel(),
) {
    val accounts by vm.accounts.collectAsState()
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(vm.accountTypes.first()) }
    var currency by remember { mutableStateOf(vm.currencies.firstOrNull()?.code ?: "USD") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cuentas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Nueva cuenta", fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nombre") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LabeledDropdown(
                            label = "Tipo",
                            options = vm.accountTypes,
                            selected = type,
                            optionLabel = { it },
                            onSelect = { type = it },
                        )
                        LabeledDropdown(
                            label = "Moneda",
                            options = vm.currencies,
                            selected = vm.currencies.firstOrNull { it.code == currency },
                            optionLabel = { "${it.code} (${it.symbol})" },
                            onSelect = { currency = it.code },
                        )
                        Button(
                            onClick = {
                                vm.add(name, type, currency)
                                name = ""
                            },
                            enabled = name.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.Add, null)
                            Text("  Crear cuenta")
                        }
                    }
                }
            }

            items(accounts, key = { it.id }) { account -> AccountRow(account, onDelete = { vm.delete(account.id) }) }
        }
    }
}

@Composable
private fun AccountRow(account: Account, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "${account.type} · ${account.currency} · saldo inicial ${Money(account.openingBalanceMinor, account.currency).format()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Eliminar")
            }
        }
    }
}
