package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.finanzen.platform.PlatformBackHandler
import com.finanzen.ui.components.ConfirmDialog
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.SageButton
import com.finanzen.ui.components.SageButtonKind
import com.finanzen.ui.components.SageTextField
import com.finanzen.ui.components.ScreenColumn
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.components.SwitchRow
import com.finanzen.ui.components.TitledSection
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalSageType
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
        ScreenColumn {
            TitledSection("Bloqueo con PIN") {
                FinanceCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SwitchRow(
                            title = "Pedir PIN al abrir Sage",
                            subtitle = when {
                                lockOn.value -> "Activo. Te pediremos el PIN al abrir la app."
                                showSetup -> "Define tu PIN abajo para activarlo."
                                else -> "Desactivado. La app abre sin pedir PIN."
                            },
                            checked = lockOn.value,
                            onCheckedChange = { on -> if (on) showSetup = true else confirmDisable = true },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (lockOn.value) {
                                SageButton("Cambiar PIN", { showSetup = true }, Modifier.weight(1f), kind = SageButtonKind.Secondary)
                            }
                            SageButton("Ver el bloqueo", { showPreview = true }, Modifier.weight(1f), kind = SageButtonKind.Secondary)
                        }
                    }
                }
            }

            if (showSetup) {
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

            TitledSection("Biometría") {
                InfoCard(
                    icon = LucideIcons.Lock,
                    title = "Huella o rostro",
                    body = "Con el bloqueo con PIN activo, si el teléfono tiene huella o rostro configurados podrás " +
                        "desbloquear con ellos. El PIN sigue sirviendo de respaldo. (No disponible en la versión de escritorio.)",
                )
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
    val digitsOnly: (String) -> String? = { it.takeIf { v -> v.length <= MAX_PIN && v.all(Char::isDigit) } }
    val pinKeyboard = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)

    FinanceCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(if (requireCurrent) "Cambiar PIN" else "Define tu PIN", style = LocalSageType.current.titleCard)
            if (requireCurrent) {
                SageTextField(
                    value = current,
                    onValueChange = { v ->
                        digitsOnly(v)?.let { current = it }
                        error = null
                    },
                    label = "PIN actual",
                    error = error.takeIf { it != SHORT_PIN && it != PIN_MISMATCH },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = pinKeyboard,
                )
            }
            SageTextField(
                value = pin,
                onValueChange = { v ->
                    digitsOnly(v)?.let { pin = it }
                    error = null
                },
                label = if (requireCurrent) "PIN nuevo" else "PIN",
                help = "De 4 a 8 dígitos. Lo necesitarás para abrir la app.",
                error = error.takeIf { it == SHORT_PIN },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = pinKeyboard,
            )
            SageTextField(
                value = confirm,
                onValueChange = { v ->
                    digitsOnly(v)?.let { confirm = it }
                    error = null
                },
                label = "Confirmar PIN",
                error = error.takeIf { it == PIN_MISMATCH },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = pinKeyboard,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SageButton("Cancelar", onCancel, Modifier.weight(1f), kind = SageButtonKind.Secondary, enabled = !isBusy)
                SageButton(
                    text = if (isBusy) "Guardando…" else "Guardar",
                    modifier = Modifier.weight(1f),
                    enabled = !isBusy,
                    onClick = {
                        val save = {
                            onSave(pin) { success ->
                                if (success) {
                                    scope.launch {
                                        delay(SAVED_FEEDBACK_MS)
                                        onSaved()
                                    }
                                } else {
                                    error = "No se pudo guardar el PIN. Inténtalo de nuevo."
                                }
                            }
                        }
                        when {
                            pin.length < MIN_PIN -> error = SHORT_PIN

                            pin != confirm -> error = PIN_MISMATCH

                            requireCurrent -> onVerifyCurrent(current) { verifyError ->
                                if (verifyError == null) save() else error = verifyError
                            }

                            else -> save()
                        }
                    },
                )
            }
        }
    }
}

private const val MIN_PIN = 4
private const val MAX_PIN = 8
private const val SAVED_FEEDBACK_MS = 150L
private const val SHORT_PIN = "El PIN debe tener al menos 4 dígitos."
private const val PIN_MISMATCH = "Los PIN no coinciden."
