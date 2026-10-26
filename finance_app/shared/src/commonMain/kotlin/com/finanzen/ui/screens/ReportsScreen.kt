package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.MonthSelector
import com.finanzen.ui.components.PeriodModeChip
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.components.YearSelector
import com.finanzen.ui.format.PeriodMode
import com.finanzen.ui.format.formatMesAnio
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.ui.theme.displayFontFamily
import com.finanzen.viewmodel.ReportsViewModel
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import org.koin.compose.viewmodel.koinViewModel

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

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader(
            title = "Reportes",
            onBack = onBack,
            action = {
                PeriodModeChip(
                    mode = mode,
                    onToggle = { vm.setMode(if (mode == PeriodMode.MONTH) PeriodMode.YEAR else PeriodMode.MONTH) },
                )
            },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
            status?.let { s ->
                item { StatusBanner(s.message, s.isError, onDismiss = { vm.clearStatus() }) }
            }
            item {
                ReportCard(
                    title = "Movimientos (CSV)",
                    subtitle = "Exporta los movimientos de $periodLabel. Compatible con Excel, Google Sheets, scripts.",
                    icon = LucideIcons.FileText,
                    buttonText = "Exportar CSV",
                    onClick = { vm.exportTransactionsCsv() },
                )
            }
            item {
                ReportCard(
                    title = "Resumen del periodo (PDF)",
                    subtitle = "Balance, categorías, presupuestos e inversiones de $periodLabel.",
                    icon = LucideIcons.FileText,
                    buttonText = "Exportar PDF",
                    onClick = { vm.exportPeriodSummaryPdf() },
                )
            }
        }
    }
}

@Composable
private fun ReportCard(
    title: String,
    subtitle: String,
    icon: Painter,
    buttonText: String,
    onClick: () -> Unit,
) {
    FinanceCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.sm),
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(buttonText, fontFamily = displayFontFamily()) }
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
