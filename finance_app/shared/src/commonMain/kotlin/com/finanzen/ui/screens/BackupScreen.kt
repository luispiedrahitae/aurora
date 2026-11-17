package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.finanzen.platform.rememberBackupPicker
import com.finanzen.ui.components.ConfirmDialog
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.InlineNotice
import com.finanzen.ui.components.NoticeTone
import com.finanzen.ui.components.SageButton
import com.finanzen.ui.components.SageButtonKind
import com.finanzen.ui.components.SageTextField
import com.finanzen.ui.components.ScreenColumn
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.components.SwitchRow
import com.finanzen.ui.components.TitledSection
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalSageType
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
        ScreenColumn {
            status?.let { s ->
                InlineNotice(
                    text = if (s.isError) "No se pudo completar. ${s.message}" else s.message,
                    tone = if (s.isError) NoticeTone.Warning else NoticeTone.Success,
                    trailing = {
                        IconButton(onClick = vm::clearStatus, modifier = Modifier.size(40.dp)) {
                            Icon(LucideIcons.X, contentDescription = "Cerrar aviso", modifier = Modifier.size(20.dp))
                        }
                    },
                )
            }
            TitledSection("Exportar") {
                FinanceCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SwitchRow(
                            title = "Cifrar con contraseña",
                            subtitle = if (encrypt) {
                                "Sin la contraseña nadie podrá abrir la copia, ni tú: guárdala en un lugar seguro."
                            } else {
                                "Sin contraseña, cualquiera que tenga el archivo podrá ver tus datos."
                            },
                            checked = encrypt,
                            onCheckedChange = { encrypt = it },
                        )
                        if (encrypt) {
                            val transformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation()
                            SageTextField(
                                value = exportPass,
                                onValueChange = { exportPass = it },
                                label = "Contraseña",
                                help = "Mínimo $MIN_PASS caracteres.",
                                error = if (exportPass.isNotEmpty() && passTooShort) "Usa al menos $MIN_PASS caracteres." else null,
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
                            )
                            SageTextField(
                                value = confirmPass,
                                onValueChange = { confirmPass = it },
                                label = "Repetir contraseña",
                                error = if (confirmPass.isNotEmpty() && passMismatch) "Las contraseñas no coinciden." else null,
                                visualTransformation = transformation,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            )
                        }
                        SageButton(
                            text = "Crear copia",
                            onClick = { vm.export(encrypt, exportPass) },
                            modifier = Modifier.fillMaxWidth(),
                            icon = LucideIcons.Download,
                            enabled = !passTooShort && !passMismatch,
                        )
                        Text(
                            "Se guarda en Descargas con extensión .finzbkp.",
                            style = LocalSageType.current.caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            TitledSection("Importar") {
                FinanceCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        InlineNotice(
                            "Restaurar reemplaza todos tus datos actuales por los del archivo. Si quieres conservarlos, crea una copia antes.",
                            tone = NoticeTone.Warning,
                        )
                        SageTextField(
                            value = importPass,
                            onValueChange = { importPass = it },
                            label = "Contraseña (si la tiene)",
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        )
                        SageButton(
                            text = "Elegir archivo y restaurar",
                            onClick = openPicker,
                            modifier = Modifier.fillMaxWidth(),
                            kind = SageButtonKind.Secondary,
                        )
                    }
                }
            }
        }
    }
}

private const val MIN_PASS = 8
