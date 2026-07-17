package com.finanzen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finanzen.db.Account
import com.finanzen.db.Investment
import com.finanzen.db.TransactionRow
import com.finanzen.ui.components.DateField
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.KindAvatar
import com.finanzen.ui.components.MoneyField
import com.finanzen.ui.components.MoneyText
import com.finanzen.ui.components.PickerField
import com.finanzen.ui.components.SectionHeader
import com.finanzen.ui.components.kindLabel
import com.finanzen.ui.format.formatDiaMes
import com.finanzen.ui.theme.LocalDateLocale
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.viewmodel.InvestmentsViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.compose.viewmodel.koinViewModel

private val MESES_INV = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

private fun fechaCortaInv(epochDay: Long): String {
    val d = LocalDate.fromEpochDays(epochDay.toInt())
    return "${d.dayOfMonth} ${MESES_INV[d.monthNumber - 1]}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsScreen(
    onBack: () -> Unit,
    openAddInitially: Boolean = false,
    vm: InvestmentsViewModel = koinViewModel(),
) {
    val open by vm.openInvestments.collectAsState()
    val closed by vm.closedInvestments.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val contributionsByInvestment by vm.contributionsByInvestment.collectAsState()
    var showForm by remember { mutableStateOf(openAddInitially) }
    var withdrawingInvestment by remember { mutableStateOf<Investment?>(null) }
    var closedExpanded by remember { mutableStateOf(false) }
    val expandedIds = remember { mutableStateMapOf<Long, Boolean>() }
    val accountNameById = remember(accounts) { accounts.associate { it.id to it.name } }

    if (showForm) {
        InvestmentFormDialog(
            accounts = accounts,
            currencyCode = vm.baseCurrency,
            onDismiss = { showForm = false },
            onConfirm = { name, amountMinor, accountId, periodic, frequency, interval, startEpochDay ->
                vm.addInvestment(name, amountMinor, accountId, periodic, frequency, interval, startEpochDay)
                showForm = false
            },
        )
    }

    withdrawingInvestment?.let { inv ->
        val contributed = contributionsByInvestment[inv.id].orEmpty().sumOf { it.amountMinor }
        WithdrawDialog(
            investmentName = inv.name,
            contributedMinor = contributed,
            currencyCode = inv.currency,
            onDismiss = { withdrawingInvestment = null },
            onConfirm = { withdrawnAmountMinor ->
                vm.closeInvestment(inv.id, withdrawnAmountMinor)
                withdrawingInvestment = null
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inversiones") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showForm = true },
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text("Inversión") },
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SectionHeader("Abiertas (${open.size})") }
            if (open.isEmpty()) {
                item { EmptyTextInv("Sin inversiones abiertas. Usa + para añadir una.") }
            } else {
                items(open, key = { it.id }) { inv ->
                    InvestmentItem(
                        investment = inv,
                        accountName = accountNameById[inv.accountId] ?: "",
                        contributions = contributionsByInvestment[inv.id].orEmpty(),
                        expanded = expandedIds[inv.id] == true,
                        onToggleExpand = { expandedIds[inv.id] = !(expandedIds[inv.id] == true) },
                        onWithdraw = { withdrawingInvestment = inv },
                        onDelete = { vm.deleteInvestment(inv.id) },
                    )
                }
            }
            if (closed.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { closedExpanded = !closedExpanded }
                            .padding(top = LocalSpacing.current.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionHeader("Inversiones cerradas (${closed.size})", modifier = Modifier.weight(1f))
                        Icon(
                            if (closedExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = if (closedExpanded) "Contraer" else "Expandir",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (closedExpanded) {
                    items(closed, key = { it.id }) { inv ->
                        ClosedInvestmentItem(investment = inv, accountName = accountNameById[inv.accountId] ?: "")
                    }
                }
            }
        }
    }
}

private fun todayEpochDayInv(): Long = Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays().toLong()

/**
 * Alta de inversión. Igual que Suscripciones: nombre, monto, y — si es periódica — un segmented
 * control "Cada X días" (DAILY) / "Día del mes" (MONTHLY). La fecha de inicio (por defecto hoy)
 * puede ser distinta de hoy — el primer aporte se registra en esa fecha, no forzosamente hoy.
 */
@Composable
private fun InvestmentFormDialog(
    accounts: List<Account>,
    currencyCode: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amountMinor: Long, accountId: Long, periodic: Boolean, frequency: String?, interval: Long?, startEpochDay: Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var amountMinor by remember { mutableStateOf(0L) }
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()) }
    var startEpochDay by remember { mutableStateOf(todayEpochDayInv()) }
    var periodic by remember { mutableStateOf(false) }
    var monthly by remember { mutableStateOf(true) } // true = Día del mes, false = Cada X días
    var dayOfMonth by remember { mutableStateOf("1") }
    var everyDays by remember { mutableStateOf("30") }

    val dayValid = dayOfMonth.toIntOrNull()?.let { it in 1..31 } == true
    val everyValid = everyDays.toLongOrNull()?.let { it >= 1 } == true
    val recurValid = if (monthly) dayValid else everyValid
    val valid = name.isNotBlank() && amountMinor > 0 && selectedAccount != null && (!periodic || recurValid)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva inversión") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                MoneyField(
                    amountMinor = amountMinor,
                    onAmountChange = { amountMinor = it },
                    currencyCode = currencyCode,
                    label = "Monto",
                    modifier = Modifier.fillMaxWidth(),
                )
                PickerField(
                    label = "Cuenta",
                    options = accounts,
                    selected = selectedAccount,
                    optionLabel = { "${it.name} (${it.currency})" },
                    onSelect = { selectedAccount = it },
                    emptyHint = "Crea una cuenta primero (pestaña Cuentas).",
                    modifier = Modifier.fillMaxWidth(),
                )
                DateField(
                    epochDay = startEpochDay,
                    onEpochDayChange = { startEpochDay = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("¿Periódica?", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Switch(checked = periodic, onCheckedChange = { periodic = it })
                }
                if (periodic) {
                    Text("Recurrencia", style = MaterialTheme.typography.labelLarge)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = monthly,
                            onClick = { monthly = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = {},
                        ) { Text("DÍA DEL MES") }
                        SegmentedButton(
                            selected = !monthly,
                            onClick = { monthly = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = {},
                        ) { Text("CADA X DÍAS") }
                    }
                    if (monthly) {
                        OutlinedTextField(
                            value = dayOfMonth,
                            onValueChange = { dayOfMonth = it.filter(Char::isDigit).take(2) },
                            label = { Text("Día del mes (1–31)") },
                            isError = dayOfMonth.isNotEmpty() && !dayValid,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        OutlinedTextField(
                            value = everyDays,
                            onValueChange = { everyDays = it.filter(Char::isDigit).take(4) },
                            label = { Text("Cada cuántos días") },
                            isError = everyDays.isNotEmpty() && !everyValid,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val frequency = if (!periodic) {
                        null
                    } else if (monthly) {
                        "MONTHLY"
                    } else {
                        "DAILY"
                    }
                    val interval = if (!periodic) {
                        null
                    } else if (monthly) {
                        dayOfMonth.toLong()
                    } else {
                        everyDays.toLong()
                    }
                    onConfirm(name.trim(), amountMinor, selectedAccount!!.id, periodic, frequency, interval, startEpochDay)
                },
                enabled = valid,
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Retiro/cierre: el usuario ingresa el monto retirado y ve en vivo el rendimiento resultante. */
@Composable
private fun WithdrawDialog(
    investmentName: String,
    contributedMinor: Long,
    currencyCode: String,
    onDismiss: () -> Unit,
    onConfirm: (withdrawnAmountMinor: Long) -> Unit,
) {
    var withdrawnAmountMinor by remember { mutableStateOf(0L) }
    val finance = LocalFinanceColors.current
    val yieldMinor = withdrawnAmountMinor - contributedMinor

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Retirar \"$investmentName\"") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MoneyField(
                    amountMinor = withdrawnAmountMinor,
                    onAmountChange = { withdrawnAmountMinor = it },
                    currencyCode = currencyCode,
                    label = "Monto retirado",
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Rendimiento", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MoneyText(
                        amountMinor = yieldMinor,
                        currency = currencyCode,
                        signed = true,
                        colorOverride = if (yieldMinor >= 0) finance.income else finance.expense,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(withdrawnAmountMinor) },
                enabled = withdrawnAmountMinor > 0,
            ) { Text("Confirmar retiro") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun EmptyTextInv(text: String) {
    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Tarjeta de inversión abierta: icono · nombre + subtítulo (cuenta, periodicidad) · monto por aporte
 * · expandir/contraer historial · retirar/eliminar. */
@Composable
private fun InvestmentItem(
    investment: Investment,
    accountName: String,
    contributions: List<TransactionRow>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onWithdraw: () -> Unit,
    onDelete: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val dateLocale = LocalDateLocale.current
    val finance = LocalFinanceColors.current
    val subtitle = if (investment.periodic == 1L) {
        val next = investment.nextContributionDate?.let { " · próximo ${fechaCortaInv(it)}" } ?: ""
        "$accountName$next"
    } else {
        "$accountName · puntual"
    }

    FinanceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(investment.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                MoneyText(amountMinor = investment.amountMinor, currency = investment.currency, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (expanded) "Contraer" else "Expandir",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                TextButton(onClick = onWithdraw) { Text("Retirar / Cerrar") }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (expanded) {
                HorizontalDivider()
                if (contributions.isEmpty()) {
                    Text(
                        "Sin aportes registrados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        contributions.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                            ) {
                                KindAvatar(kind = row.kind, size = 32.dp)
                                Column(Modifier.weight(1f)) {
                                    Text(row.note.ifBlank { kindLabel(row.kind) }, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                    Text(formatDiaMes(row.date, dateLocale), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                MoneyText(
                                    amountMinor = -row.amountMinor,
                                    currency = row.currency,
                                    style = MaterialTheme.typography.bodyMedium,
                                    signed = true,
                                    colorOverride = finance.expense,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Tarjeta de inversión cerrada: aportado / retirado / rendimiento (verde si ganancia, rosa si pérdida). */
@Composable
private fun ClosedInvestmentItem(investment: Investment, accountName: String) {
    val spacing = LocalSpacing.current
    val finance = LocalFinanceColors.current
    val withdrawn = investment.withdrawnAmountMinor ?: 0L
    val yieldMinor = investment.yieldMinor ?: 0L
    val contributed = withdrawn - yieldMinor
    val yieldColor = if (yieldMinor >= 0) finance.income else finance.expense
    val yieldPct = if (contributed != 0L) yieldMinor * 100.0 / contributed else null

    FinanceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Column {
                Text(investment.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(accountName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ClosedInvestmentRow("Aportado", contributed, investment.currency)
            ClosedInvestmentRow("Retirado", withdrawn, investment.currency)
            ClosedInvestmentRow(
                "Rendimiento",
                yieldMinor,
                investment.currency,
                signed = true,
                colorOverride = yieldColor,
                pct = yieldPct,
            )
        }
    }
}

@Composable
private fun ClosedInvestmentRow(
    label: String,
    amountMinor: Long,
    currency: String,
    signed: Boolean = false,
    colorOverride: Color? = null,
    pct: Double? = null,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (pct != null) {
                val sign = if (pct >= 0) "+" else ""
                Text(
                    "($sign${pct.toInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorOverride ?: MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MoneyText(amountMinor = amountMinor, currency = currency, style = MaterialTheme.typography.bodyMedium, signed = signed, colorOverride = colorOverride)
        }
    }
}
