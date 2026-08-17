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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.MonthSelector
import com.finanzen.ui.components.PeriodModeChip
import com.finanzen.ui.components.YearSelector
import com.finanzen.ui.format.PeriodMode
import com.finanzen.ui.format.formatMesAnio
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.viewmodel.ReportsViewModel
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    vm: ReportsViewModel = koinViewModel(),
) {
    val status by vm.status.collectAsState()
    val mode by vm.mode.collectAsState()
    val month by vm.month.collectAsState()
    val year by vm.year.collectAsState()
    val periodLabel = vm.periodLabel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    PeriodModeChip(
                        mode = mode,
                        onToggle = { vm.setMode(if (mode == PeriodMode.MONTH) PeriodMode.YEAR else PeriodMode.MONTH) },
                    )
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
                if (mode == PeriodMode.MONTH) {
                    MonthSelector(
                        label = formatMesAnio(month),
                        onPrev = { vm.setMonth(month.plus(DatePeriod(months = -1))) },
                        onNext = { vm.setMonth(month.plus(DatePeriod(months = 1))) },
                    )
                } else {
                    YearSelector(
                        label = year.toString(),
                        onPrev = { vm.setYear(year - 1) },
                        onNext = { vm.setYear(year + 1) },
                    )
                }
            }
            item {
                ReportCard(
                    title = "Movimientos (CSV)",
                    subtitle = "Exporta los movimientos de $periodLabel. Compatible con Excel, Google Sheets, scripts.",
                    icon = Icons.Outlined.Description,
                    buttonText = "Exportar CSV",
                    onClick = { vm.exportTransactionsCsv() },
                )
            }
            item {
                ReportCard(
                    title = "Resumen del periodo (PDF)",
                    subtitle = "Balance, patrimonio neto, categorías, presupuestos e inversiones de $periodLabel.",
                    icon = Icons.Outlined.PictureAsPdf,
                    buttonText = "Exportar PDF",
                    onClick = { vm.exportPeriodSummaryPdf() },
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
    FinanceCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    val finance = LocalFinanceColors.current
    val container = if (isError) finance.expenseContainer else MaterialTheme.colorScheme.primaryContainer
    val fg = if (isError) finance.expense else MaterialTheme.colorScheme.onPrimaryContainer
    FinanceCard(modifier = Modifier.fillMaxWidth(), color = container, contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (isError) "Pendiente / error" else "Exportado", color = fg, fontWeight = FontWeight.SemiBold)
            Text(message, style = MaterialTheme.typography.bodySmall, color = fg)
            Button(onClick = onDismiss) { Text("OK") }
        }
    }
}
