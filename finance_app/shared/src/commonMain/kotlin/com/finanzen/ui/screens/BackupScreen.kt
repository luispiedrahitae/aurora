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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.finanzen.viewmodel.BackupViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    vm: BackupViewModel = koinViewModel(),
) {
    val status by vm.status.collectAsState()
    var exportPass by remember { mutableStateOf("") }
    var importPass by remember { mutableStateOf("") }
    var importPath by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup encriptado") },
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
                        Text("Exportar", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Cifra toda la DB con AES-256-GCM. La clave se deriva de tu passphrase con PBKDF2 (100k iteraciones). Mínimo 8 caracteres.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = exportPass,
                            onValueChange = { exportPass = it },
                            label = { Text("Passphrase") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(onClick = { vm.export(exportPass) }) { Text("Crear backup") }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Importar", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Restaura TODA la DB desde un archivo .finzbkp. Esto reemplaza tus datos actuales.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = importPath,
                            onValueChange = { importPath = it },
                            label = { Text("Ruta absoluta del .finzbkp") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = importPass,
                            onValueChange = { importPass = it },
                            label = { Text("Passphrase") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(onClick = { vm.import(importPass, importPath) }) { Text("Restaurar") }
                    }
                }
            }

            status?.let { s ->
                item {
                    val container = if (s.isError) Color(0xFFFFE0E0) else MaterialTheme.colorScheme.primaryContainer
                    val fg = if (s.isError) Color(0xFFB13E53) else MaterialTheme.colorScheme.onPrimaryContainer
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = container),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
