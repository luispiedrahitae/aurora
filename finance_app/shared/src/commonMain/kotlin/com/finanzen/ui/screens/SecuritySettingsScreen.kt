package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.finanzen.platform.PlatformBackHandler
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

    // El "atrás" del sistema cierra la vista previa en vez de salir de Seguridad entera.
    PlatformBackHandler(enabled = showPreview) { showPreview = false }
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
                        // Toda la fila es el interruptor (como en Notificaciones), no solo el Switch.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = lockOn.value,
                                    role = Role.Switch,
                                    onValueChange = { on -> if (on) showSetup = true else confirmDisable = true },
                                ),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Bloqueo con PIN", fontWeight = FontWeight.SemiBold)
                                Text(
                                    when {
                                        lockOn.value -> "Activo. Te pediremos el PIN al abrir la app."
                                        showSetup -> "Define tu PIN abajo para activarlo."
                                        else -> "Desactivado. La app abre sin pedir PIN."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(checked = lockOn.value, onCheckedChange = null, colors = sageSwitchColors())
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
                        // Cambiar un PIN existente exige conocer el actual.
                        requireCurrent = lockOn.value,
                        onVerifyCurrent = vm::verifyCurrentPin,
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
                                "podrás desbloquear con biometría desde la pantalla de bloqueo. (No disponible en la versión de escritorio.)",
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
    requireCurrent: Boolean,
    onVerifyCurrent: (String, (String?) -> Unit) -> Unit,
    onCancel: () -> Unit,
    onSave: (String, (Boolean) -> Unit) -> Unit,
    onSaved: () -> Unit,
    isBusy: Boolean,
) {
    var current by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    FinanceCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (requireCurrent) "Cambiar PIN" else "Define tu PIN", fontWeight = FontWeight.SemiBold)
            Text(
                "4 a 8 dígitos. Necesario para volver a abrir la app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (requireCurrent) {
                OutlinedTextField(
                    value = current,
                    onValueChange = {
                        if (it.length <= 8 && it.all(Char::isDigit)) current = it
                        error = null
                    },
                    label = { Text("PIN actual") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
            }
            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 8 && it.all(Char::isDigit)) pin = it
                    error = null
                },
                isError = error == SHORT_PIN,
                label = { Text(if (requireCurrent) "PIN nuevo" else "PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
            OutlinedTextField(
                value = confirm,
                onValueChange = {
                    if (it.length <= 8 && it.all(Char::isDigit)) confirm = it
                    error = null
                },
                isError = error == PIN_MISMATCH,
                label = { Text("Confirmar PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
            error?.let { FieldError(it) }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(onClick = onCancel, enabled = !isBusy) { Text("Cancelar") }
                Button(
                    enabled = !isBusy,
                    onClick = {
                        val save = {
                            onSave(pin) { success ->
                                if (success) {
                                    scope.launch {
                                        delay(150)
                                        onSaved()
                                    }
                                } else {
                                    error = "No se pudo guardar el PIN. Inténtalo de nuevo."
                                }
                            }
                        }
                        when {
                            pin.length < 4 -> error = SHORT_PIN

                            pin != confirm -> error = PIN_MISMATCH

                            requireCurrent -> onVerifyCurrent(current) { verifyError ->
                                if (verifyError == null) save() else error = verifyError
                            }

                            else -> save()
                        }
                    },
                ) { Text(if (isBusy) "Guardando…" else "Guardar", fontFamily = displayFontFamily()) }
            }
        }
    }
}

private const val SHORT_PIN = "El PIN debe tener al menos 4 dígitos"
private const val PIN_MISMATCH = "Los PIN no coinciden"
