package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.viewmodel.SecurityViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    onBack: () -> Unit,
    vm: SecurityViewModel = koinInject(),
) {
    val lockOn = remember { mutableStateOf(vm.lockEnabled()) }
    var showSetup by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seguridad") },
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
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Bloqueo con PIN", fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (lockOn.value) {
                                        "Activo. Te pediremos el PIN al abrir la app."
                                    } else {
                                        "Desactivado. La app abre directamente al dashboard."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = lockOn.value,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        showSetup = true
                                    } else {
                                        vm.disableLock()
                                        lockOn.value = false
                                    }
                                },
                            )
                        }
                        if (lockOn.value) {
                            TextButton(onClick = { showSetup = true }) { Text("Cambiar PIN") }
                        }
                    }
                }
            }

            if (showSetup) {
                item {
                    PinSetupCard(
                        onCancel = { showSetup = false },
                        onSave = { pin ->
                            if (vm.setupPin(pin)) {
                                lockOn.value = true
                                showSetup = false
                                true
                            } else {
                                false
                            }
                        },
                    )
                }
            }

            item {
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Biometría", fontWeight = FontWeight.SemiBold)
                        Text(
                            "No disponible en el preview de escritorio. En Android se conectará a BiometricPrompt; en iOS a LocalAuthentication.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinSetupCard(onCancel: () -> Unit, onSave: (String) -> Boolean) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val finance = LocalFinanceColors.current

    FinanceCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Define tu PIN", fontWeight = FontWeight.SemiBold)
            Text(
                "4 a 8 dígitos. Necesario para volver a abrir la app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
                label = { Text("PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
            OutlinedTextField(
                value = confirm,
                onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) confirm = it },
                label = { Text("Confirmar PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
            error?.let { Text(it, color = finance.expense, style = MaterialTheme.typography.bodySmall) }
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(onClick = onCancel) { Text("Cancelar") }
                Button(
                    onClick = {
                        when {
                            pin.length < 4 -> error = "Mínimo 4 dígitos"
                            pin != confirm -> error = "Los PINs no coinciden"
                            else -> if (!onSave(pin)) error = "PIN no válido"
                        }
                    },
                ) { Text("Guardar") }
            }
        }
    }
}
