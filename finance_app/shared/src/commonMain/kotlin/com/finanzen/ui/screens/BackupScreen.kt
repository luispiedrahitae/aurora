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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.finanzen.platform.rememberBackupPicker
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.SelectorPill
import com.finanzen.ui.components.SubScreenHeader
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
    val openPicker = rememberBackupPicker { content -> vm.import(importPass, content) }

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader(title = "Backup", onBack = onBack)
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
                    FinanceCard(modifier = Modifier.fillMaxWidth(), color = container, contentPadding = PaddingValues(12.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (s.isError) "Error" else "Éxito", color = fg, fontWeight = FontWeight.SemiBold)
                            Text(s.message, style = MaterialTheme.typography.bodySmall, color = fg)
                            Button(onClick = { vm.clearStatus() }) { Text("OK") }
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
                                "Cifra toda la DB con AES-256-GCM (clave derivada de tu passphrase con PBKDF2). Mínimo 8 caracteres."
                            } else {
                                "Exporta toda la DB como JSON sin cifrar. Cualquiera con el archivo podrá leerlo."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Cifrar con contraseña", modifier = Modifier.weight(1f))
                            SelectorPill(text = "Sí", selected = encrypt, onClick = { encrypt = true })
                            SelectorPill(text = "No", selected = !encrypt, onClick = { encrypt = false })
                        }
                        if (encrypt) {
                            OutlinedTextField(
                                value = exportPass,
                                onValueChange = { exportPass = it },
                                label = { Text("Passphrase") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Button(onClick = { vm.export(encrypt, exportPass) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Crear backup", fontFamily = displayFontFamily())
                        }
                    }
                }
            }

            item {
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Importar", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Restaura TODA la DB desde un archivo .finzbkp. Si está cifrado, escribe la passphrase. Esto reemplaza tus datos actuales.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = importPass,
                            onValueChange = { importPass = it },
                            label = { Text("Passphrase (si está cifrado)") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(onClick = { openPicker() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Elegir archivo y restaurar", fontFamily = displayFontFamily())
                        }
                    }
                }
            }
        }
    }
}
