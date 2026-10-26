package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.ConfirmDialog
import com.finanzen.ui.components.FieldError
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.components.sageSwitchColors
import com.finanzen.ui.theme.displayFontFamily
import com.finanzen.viewmodel.SecurityViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SecuritySettingsScreen(
    onBack: () -> Unit,
    vm: SecurityViewModel = koinInject(),
) {
    val lockOn = remember { mutableStateOf(vm.lockEnabled()) }
    var showSetup by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }

    var confirmDisable by remember { mutableStateOf(false) }

    if (confirmDisable) {
        ConfirmDialog(
            title = "¿Desactivar el bloqueo con PIN?",
            body = "Cualquiera con acceso a este dispositivo podrá abrir la app y ver tus finanzas.",
            confirmLabel = "Desactivar",
            onConfirm = {
                vm.disableLock()
                lockOn.value = false
                confirmDisable = false
            },
            onDismiss = { confirmDisable = false },
        )
    }

    if (showPreview) {
        PinPreviewScreen(onCancel = { showPreview = false })
        return
    }

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader(title = "Seguridad", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
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
                                onCheckedChange = { on -> if (on) showSetup = true else confirmDisable = true },
                                colors = sageSwitchColors(),
                            )
                        }
                        if (lockOn.value) {
                            TextButton(onClick = { showSetup = true }) { Text("Cambiar PIN") }
                        }
                        TextButton(onClick = { showPreview = true }) { Text("Vista previa del bloqueo") }
                    }
                }
            }

            if (showSetup) {
                item {
                    PinSetupCard(
                        onCancel = { showSetup = false },
                        onSave = { pin, onResult -> vm.setupPin(pin, onResult) },
                        onSaved = {
                            lockOn.value = true
                            showSetup = false
                        },
                        isBusy = vm.isBusy.collectAsState().value,
                    )
                }
            }

            item {
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Biometría", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Si activas el bloqueo con PIN y tu dispositivo tiene huella o rostro configurados, " +
                                "podrás desbloquear con biometría desde la pantalla de bloqueo. (No disponible en el preview de escritorio.)",
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
private fun PinSetupCard(
    onCancel: () -> Unit,
    onSave: (String, (Boolean) -> Unit) -> Unit,
    onSaved: () -> Unit,
    isBusy: Boolean,
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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
            error?.let { FieldError(it) }
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(onClick = onCancel, enabled = !isBusy) { Text("Cancelar") }
                Button(
                    enabled = !isBusy,
                    onClick = {
                        when {
                            pin.length < 4 -> error = "Mínimo 4 dígitos"
                            pin != confirm -> error = "Los PINs no coinciden"
                            else -> onSave(pin) { success ->
                                if (success) {
                                    scope.launch {
                                        delay(150)
                                        onSaved()
                                    }
                                } else {
                                    error = "PIN no válido"
                                }
                            }
                        }
                    },
                ) { Text(if (isBusy) "Guardando…" else "Guardar", fontFamily = displayFontFamily()) }
            }
        }
    }
}
