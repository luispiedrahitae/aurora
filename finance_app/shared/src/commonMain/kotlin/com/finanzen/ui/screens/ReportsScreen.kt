package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.viewmodel.ReportsViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    vm: ReportsViewModel = koinViewModel(),
) {
    val status by vm.status.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes") },
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
                ReportCard(
                    title = "Transacciones (CSV)",
                    subtitle = "Exporta todas las transacciones. Compatible con Excel, Google Sheets, scripts.",
                    icon = Icons.Outlined.Description,
                    buttonText = "Exportar CSV",
                    onClick = { vm.exportTransactionsCsv() },
                )
            }
            item {
                ReportCard(
                    title = "Resumen mensual (PDF)",
                    subtitle = "Totales del periodo + top 10 categorías de gasto. En Desktop sale como .pdf.txt (stub); PDF real en Android/iOS.",
                    icon = Icons.Outlined.PictureAsPdf,
                    buttonText = "Exportar PDF",
                    onClick = { vm.exportMonthlySummaryPdf() },
                )
            }

            status?.let { s ->
                item { StatusBanner(s.message, s.isError, onDismiss = { vm.clearStatus() }) }
            }
        }
    }
}

@Composable
private fun ReportCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    buttonText: String,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onClick) { Text(buttonText) }
        }
    }
}

@Composable
private fun StatusBanner(message: String, isError: Boolean, onDismiss: () -> Unit) {
    val container = if (isError) Color(0xFFFFE0E0) else MaterialTheme.colorScheme.primaryContainer
    val fg = if (isError) Color(0xFFB13E53) else MaterialTheme.colorScheme.onPrimaryContainer
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = container),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (isError) "Pendiente / error" else "Exportado", color = fg, fontWeight = FontWeight.SemiBold)
            Text(message, style = MaterialTheme.typography.bodySmall, color = fg)
            Button(onClick = onDismiss) { Text("OK") }
        }
    }
}
