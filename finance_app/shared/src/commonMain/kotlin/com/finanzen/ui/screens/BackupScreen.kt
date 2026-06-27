package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.finanzen.platform.rememberBackupPicker
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.viewmodel.BackupViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup") },
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
                        Text("Exportar", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
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
                            Switch(checked = encrypt, onCheckedChange = { encrypt = it })
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
                        Button(onClick = { vm.export(encrypt, exportPass) }) { Text("Crear backup") }
                    }
                }
            }

            item {
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Importar", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
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
                        Button(onClick = { openPicker() }) { Text("Elegir archivo y restaurar") }
                    }
                }
            }

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
        }
    }
}
