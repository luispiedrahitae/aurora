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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.finanzen.platform.rememberBackupPicker
import com.finanzen.ui.components.ConfirmDialog
import com.finanzen.ui.components.FieldError
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.components.sageSwitchColors
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.displayFontFamily
import com.finanzen.viewmodel.BackupViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    vm: BackupViewModel = koinViewModel(),
) {
    val status by vm.status.collectAsState()
    var encrypt by remember { mutableStateOf(true) }
    var exportPass by remember { mutableStateOf("") }
    var importPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    // Primero se elige el archivo y después se confirma: antes se aceptaba sobrescribir todo sin saber
    // siquiera si el usuario iba a elegir un archivo.
    var pickedContent by remember { mutableStateOf<String?>(null) }
    val openPicker = rememberBackupPicker { content ->
        if (content.isNullOrEmpty()) vm.import(importPass, content) else pickedContent = content
    }

    pickedContent?.let { content ->
        ConfirmDialog(
            title = "¿Restaurar esta copia?",
            body = "Todos tus datos actuales se reemplazarán por los del archivo elegido. Si quieres conservarlos, crea una copia antes.",
            confirmLabel = "Restaurar",
            onConfirm = {
                pickedContent = null
                vm.import(importPass, content)
            },
            onDismiss = { pickedContent = null },
        )
    }
    // Perder la contraseña es perder la copia: se pide dos veces y se valida antes de habilitar el botón.
    val passTooShort = encrypt && exportPass.length < MIN_PASS
    val passMismatch = encrypt && confirmPass != exportPass

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader(title = "Copia de seguridad", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            status?.let { s ->
                item {
                    val finance = LocalFinanceColors.current
                    val container = if (s.isError) finance.expenseContainer else MaterialTheme.colorScheme.primaryContainer
                    val fg = if (s.isError) finance.expense else MaterialTheme.colorScheme.onPrimaryContainer
                    // liveRegion: el resultado se anuncia aunque el foco esté en el botón que lo disparó.
                    FinanceCard(
                        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                        color = container,
                        contentPadding = PaddingValues(12.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (s.isError) "No se pudo completar" else "Listo", color = fg, fontWeight = FontWeight.SemiBold)
                            Text(s.message, style = MaterialTheme.typography.bodySmall, color = fg)
                            TextButton(onClick = { vm.clearStatus() }) { Text("Entendido", color = fg) }
                        }
                    }
                }
            }
            item {
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Exportar", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (encrypt) {
                                "Protege la copia de tu base de datos con una contraseña. Sin ella nadie podrá abrirla, ni tú: " +
                                    "guárdala bien. Mínimo 8 caracteres."
                            } else {
                                "Guarda tu base de datos sin contraseña. Cualquiera que tenga el archivo podrá ver tus datos."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.toggleable(value = encrypt, role = Role.Switch, onValueChange = { encrypt = it }),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("Cifrar con contraseña", modifier = Modifier.weight(1f))
                            Switch(checked = encrypt, onCheckedChange = null, colors = sageSwitchColors())
                        }
                        if (encrypt) {
                            val transformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation()
                            OutlinedTextField(
                                value = exportPass,
                                onValueChange = { exportPass = it },
                                label = { Text("Contraseña") },
                                visualTransformation = transformation,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { showPass = !showPass }) {
                                        Icon(
                                            if (showPass) LucideIcons.EyeOff else LucideIcons.Eye,
                                            contentDescription = if (showPass) "Ocultar contraseña" else "Mostrar contraseña",
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = confirmPass,
                                onValueChange = { confirmPass = it },
                                label = { Text("Repetir contraseña") },
                                visualTransformation = transformation,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                isError = confirmPass.isNotEmpty() && passMismatch,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            when {
                                exportPass.isNotEmpty() && passTooShort -> FieldError("Usa al menos $MIN_PASS caracteres.")
                                confirmPass.isNotEmpty() && passMismatch -> FieldError("Las contraseñas no coinciden.")
                            }
                        }
                        Button(
                            onClick = { vm.export(encrypt, exportPass) },
                            enabled = !passTooShort && !passMismatch,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Crear copia", fontFamily = displayFontFamily())
                        }
                    }
                }
            }

            item {
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Importar", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Recupera tu base de datos desde un archivo .finzbkp (las copias se guardan en Descargas). " +
                                "Si tiene contraseña, escríbela. Esto reemplaza todos tus datos actuales.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = importPass,
                            onValueChange = { importPass = it },
                            label = { Text("Contraseña (si la tiene)") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(onClick = openPicker, modifier = Modifier.fillMaxWidth()) {
                            Text("Elegir archivo y restaurar", fontFamily = displayFontFamily())
                        }
                    }
                }
            }
        }
    }
}

private const val MIN_PASS = 8
