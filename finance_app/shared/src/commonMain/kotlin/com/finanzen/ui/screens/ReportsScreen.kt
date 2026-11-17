package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.IconAvatar
import com.finanzen.ui.components.InlineNotice
import com.finanzen.ui.components.MonthSelector
import com.finanzen.ui.components.NoticeTone
import com.finanzen.ui.components.SageButton
import com.finanzen.ui.components.SageButtonKind
import com.finanzen.ui.components.ScreenColumn
import com.finanzen.ui.components.SegmentedControl
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.components.YearSelector
import com.finanzen.ui.format.PeriodMode
import com.finanzen.ui.format.formatMesAnio
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalDateLocale
import com.finanzen.ui.theme.LocalSageType
import com.finanzen.viewmodel.ReportsViewModel
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

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
        SubScreenHeader(title = "Reportes", onBack = onBack)
        ScreenColumn {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SegmentedControl(
                    options = PeriodMode.entries.toList(),
                    selected = mode,
                    onSelect = vm::setMode,
                    label = { if (it == PeriodMode.MONTH) "Mes" else "Año" },
                )
                if (mode == PeriodMode.MONTH) {
                    MonthSelector(
                        label = formatMesAnio(month, LocalDateLocale.current),
                        onPrev = { vm.setMonth(month.plus(DatePeriod(months = -1))) },
                        onNext = { vm.setMonth(month.plus(DatePeriod(months = 1))) },
                        onLabelClick = {
                            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                            vm.setMonth(LocalDate(today.year, today.month, 1))
                        },
                    )
                } else {
                    YearSelector(
                        label = year.toString(),
                        onPrev = { vm.setYear(year - 1) },
                        onNext = { vm.setYear(year + 1) },
                        onLabelClick = { vm.setYear(Clock.System.todayIn(TimeZone.currentSystemDefault()).year) },
                    )
                }
            }
            status?.let { s ->
                InlineNotice(
                    text = if (s.isError) "No se pudo exportar. ${s.message}" else s.message,
                    tone = if (s.isError) NoticeTone.Warning else NoticeTone.Success,
                    trailing = {
                        IconButton(onClick = vm::clearStatus, modifier = Modifier.size(40.dp)) {
                            Icon(LucideIcons.X, contentDescription = "Cerrar aviso", modifier = Modifier.size(20.dp))
                        }
                    },
                )
            }
            ReportCard(
                title = "Movimientos (CSV)",
                subtitle = "Los movimientos de $periodLabel. Se abre en Excel, Google Sheets y otras hojas de cálculo.",
                icon = LucideIcons.FileText,
                buttonText = "Exportar CSV",
                onClick = vm::exportTransactionsCsv,
            )
            ReportCard(
                title = "Resumen del período (PDF)",
                subtitle = "Balance, categorías, presupuestos e inversiones de $periodLabel, listo para imprimir o compartir.",
                icon = LucideIcons.PieChart,
                buttonText = "Exportar resumen PDF",
                onClick = vm::exportPeriodSummaryPdf,
            )
            Text(
                "Los archivos se guardan en la carpeta Descargas del dispositivo. Sage no los envía a ningún lado.",
                style = LocalSageType.current.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconAvatar(icon)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = LocalSageType.current.titleCard)
                    Text(subtitle, style = LocalSageType.current.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            SageButton(buttonText, onClick, Modifier.fillMaxWidth(), kind = SageButtonKind.Secondary)
        }
    }
}
