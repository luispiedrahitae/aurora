package com.finanzen.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.AccountBalanceBars
import com.finanzen.ui.components.AutoSizeText
import com.finanzen.ui.components.BentoTileSize
import com.finanzen.ui.components.CategoryBreakdownSection
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.IncomeExpenseBarChart
import com.finanzen.ui.components.KpiDelta
import com.finanzen.ui.components.MainTabHeader
import com.finanzen.ui.components.MonthSelector
import com.finanzen.ui.components.PeriodModeChip
import com.finanzen.ui.components.SavingsRateGauge
import com.finanzen.ui.components.SectionHeader
import com.finanzen.ui.components.TopFrequentExpensesList
import com.finanzen.ui.components.YearSelector
import com.finanzen.ui.components.kpiDelta
import com.finanzen.ui.format.PeriodMode
import com.finanzen.ui.format.formatMesAnio
import com.finanzen.ui.format.mesLargo
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalAccentColor
import com.finanzen.ui.theme.LocalDateLocale
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.ui.theme.motionTween
import com.finanzen.viewmodel.DashboardViewModel
import com.finanzen.viewmodel.SettingsViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DashboardScreen(
    onOpenSubscriptions: () -> Unit = {},
    vm: DashboardViewModel = koinViewModel(),
    settingsVm: SettingsViewModel = koinInject(),
) {
    val data by vm.data.collectAsState()
    val mode by vm.mode.collectAsState()
    val month by vm.month.collectAsState()
    val year by vm.year.collectAsState()
    val hideAmounts by settingsVm.hideAmounts.collectAsState()
    val savingsGoalPct by settingsVm.savingsGoalPct.collectAsState()
    val spacing = LocalSpacing.current
    val fmt = LocalMoneyFormat.current
    val finance = LocalFinanceColors.current
    val dateLocale = LocalDateLocale.current
    // Columna única de tarjetas planas a todo el ancho (Sage.dc.html: nada de grid bento de 2
    // columnas salvo el par Ingresos/Gastos, que es una fila explícita). Todo salvo el hero y
    // "Saldos por cuenta" está acotado al periodo seleccionado (chip Mes/Año arriba a la derecha
    // del título + selector debajo).
    Column(Modifier.fillMaxSize()) {
        MainTabHeader(
            // "Resumen" (mismo label que la pestaña en Destinations.kt) en vez del wordmark "Sage" —
            // las otras 4 pestañas usan su nombre de sección, no la marca, en este header.
            title = "Resumen",
            action = {
                PeriodModeChip(
                    mode = mode,
                    onToggle = { vm.setMode(if (mode == PeriodMode.MONTH) PeriodMode.YEAR else PeriodMode.MONTH) },
                )
            },
        )
        if (mode == PeriodMode.MONTH) {
            MonthSelector(
                label = formatMesAnio(month, dateLocale),
                onPrev = { vm.setMonth(month.plus(DatePeriod(months = -1))) },
                onNext = { vm.setMonth(month.plus(DatePeriod(months = 1))) },
                modifier = Modifier.padding(horizontal = spacing.lg),
                onLabelClick = {
                    val t = Clock.System.todayIn(TimeZone.currentSystemDefault())
                    vm.setMonth(LocalDate(t.year, t.month, 1))
                },
            )
        } else {
            YearSelector(
                label = year.toString(),
                onPrev = { vm.setYear(year - 1) },
                onNext = { vm.setYear(year + 1) },
                modifier = Modifier.padding(horizontal = spacing.lg),
                onLabelClick = { vm.setYear(Clock.System.todayIn(TimeZone.currentSystemDefault()).year) },
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // bottom extra para que el FAB central no tape la última tarjeta.
            contentPadding = PaddingValues(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            item {
                BalanceHeroCard(
                    totalBalance = data.totalBalanceMinor,
                    currency = data.currency,
                    hidden = hideAmounts,
                    onToggleHidden = { settingsVm.setHideAmounts(!hideAmounts) },
                )
            }

            val prevLabel = when (mode) {
                PeriodMode.MONTH -> mesLargo(month.plus(DatePeriod(months = -1)).monthNumber).replaceFirstChar { it.uppercase() }
                PeriodMode.YEAR -> "${year - 1}"
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    KpiTile(
                        "Ingresos",
                        if (hideAmounts) MASK else fmt.format(data.periodIncomeMinor, data.currency),
                        finance.income,
                        modifier = Modifier.weight(1f),
                        masked = hideAmounts,
                        delta = kpiDelta(data.periodIncomeMinor, data.prevPeriodIncomeMinor, prevLabel, upIsGood = true),
                    )
                    KpiTile(
                        "Gastos",
                        if (hideAmounts) MASK else fmt.format(data.periodExpenseMinor, data.currency),
                        finance.expense,
                        flipTrendIcon = true,
                        modifier = Modifier.weight(1f),
                        masked = hideAmounts,
                        delta = kpiDelta(data.periodExpenseMinor, data.prevPeriodExpenseMinor, prevLabel, upIsGood = false),
                    )
                }
            }
            item {
                SubscriptionsSummaryRow(
                    valueText = if (hideAmounts) MASK else fmt.format(data.subscriptionCostMinor, data.currency),
                    masked = hideAmounts,
                    onClick = onOpenSubscriptions,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                SavingsRateGauge(
                    savingsRate = data.savingsRate,
                    goalPct = savingsGoalPct,
                    onGoalChange = settingsVm::setSavingsGoalPct,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (data.accounts.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        SectionHeader("Saldos por cuenta")
                        AccountBalanceBars(data.accounts, data.currency, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            if (data.incomeExpenseTrend.any { it.incomeMinor > 0 || it.expenseMinor > 0 }) {
                item {
                    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                    val todayIndex = when (mode) {
                        PeriodMode.MONTH -> if (month.year == today.year && month.monthNumber == today.monthNumber) today.dayOfMonth - 1 else null
                        PeriodMode.YEAR -> if (year == today.year) today.monthNumber - 1 else null
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        SectionHeader("Ingresos vs gastos")
                        IncomeExpenseBarChart(
                            data.incomeExpenseTrend,
                            data.currency,
                            modifier = Modifier.fillMaxWidth(),
                            initialSelectedIndex = todayIndex,
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    SectionHeader("Gastos por categoría")
                    CategoryBreakdownSection(data.categoryBreakdown, data.currency, modifier = Modifier.fillMaxWidth())
                }
            }

            if (data.frequent.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        SectionHeader("Gastos más frecuentes")
                        TopFrequentExpensesList(data.frequent, data.currency, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

private const val MASK = "******"

@Composable
private fun BalanceHeroCard(
    totalBalance: Long,
    currency: String,
    hidden: Boolean,
    onToggleHidden: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVar = MaterialTheme.colorScheme.onSurfaceVariant
    // Tarjeta neutra (no primaryContainer): los colores semánticos verde/rojo contrastan como es debido
    // en cualquier acento/dinámico, sin lavar el texto sobre el contenedor de color.
    val expenseRed = LocalFinanceColors.current.expense

    // El balance general es histórico (saldos iniciales + ingresos − gastos de todo el
    // histórico), con el mismo formato global (símbolo + separadores) que el resto de montos de
    // la app. No cambia con el periodo seleccionado (mes/año): es el hero fijo de la pantalla — el
    // número que el usuario más consulta al abrir la app. AutoSizeText se encarga de que la cifra
    // grande siempre quepa en una línea.
    val fmt = LocalMoneyFormat.current
    val haptic = LocalHapticFeedback.current
    val balanceColor = if (totalBalance >= 0) onSurface else expenseRed
    val animatedBalance by animateFloatAsState(targetValue = totalBalance.toFloat(), animationSpec = motionTween(400))
    val balanceText = if (hidden) MASK else fmt.format(animatedBalance.toLong(), currency)

    FinanceCard(modifier = Modifier.fillMaxWidth(), size = BentoTileSize.Hero) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "BALANCE GENERAL",
                    style = MaterialTheme.typography.labelLarge,
                    color = onSurfaceVar,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleHidden()
                }) {
                    Icon(
                        if (hidden) LucideIcons.EyeOff else LucideIcons.Eye,
                        contentDescription = if (hidden) "Mostrar montos" else "Ocultar montos",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            AutoSizeText(
                text = balanceText,
                modifier = Modifier.fillMaxWidth()
                    .then(if (hidden) Modifier.semantics { contentDescription = "Monto oculto" } else Modifier),
                // Sage.dc.html: el balance del hero es 36px/700, no el display de 52sp reservado
                // para "un número por pantalla" — aquí ya conviven balance + KPIs + suscripciones.
                maxFontSize = MaterialTheme.typography.displaySmall.fontSize,
                color = balanceColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.displaySmall,
            )
        }
    }
}

/** Tile de KPI del periodo seleccionado (mes o año): recibe el valor ya formateado (dinero u otra
 * unidad, p. ej. un porcentaje) para servir tanto montos como tasas sin duplicar el componente.
 * [delta] añade la línea "vs periodo anterior"; [caption] añade una segunda línea secundaria
 * opcional (p. ej. la proyección de gasto a fin de mes, solo en modo Mes); [masked] marca
 * semánticamente el valor oculto por privacidad. */
@Composable
private fun KpiTile(
    label: String,
    valueText: String,
    accent: Color,
    modifier: Modifier = Modifier,
    // No hay un ícono "trending-down" en el set Lucide del proyecto; se reutiliza TrendingUp
    // rotado 180° para Gastos en vez de sumar un recurso nuevo.
    flipTrendIcon: Boolean = false,
    masked: Boolean = false,
    delta: KpiDelta? = null,
    caption: String? = null,
) {
    val spacing = LocalSpacing.current
    val finance = LocalFinanceColors.current
    FinanceCard(modifier = modifier, contentPadding = PaddingValues(spacing.md)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    LucideIcons.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp).rotate(if (flipTrendIcon) 180f else 0f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(
                    valueText,
                    // Número secundario de pantalla (KPI), no la cifra hero — Title (20sp) en vez de
                    // Headline (24sp) para que quepa cómodo en la mitad del ancho, con Bold explícito
                    // (Title por defecto es SemiBold) para que siga leyéndose como un dato destacado.
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    maxLines = 1,
                    modifier = if (masked) Modifier.semantics { contentDescription = "Monto oculto" } else Modifier,
                )
                delta?.let {
                    Icon(
                        // Sin glifo de flecha arriba/abajo en el set Lucide (39 iconos) — se reusa
                        // chevron up/down, ya en el set, en vez de agregar un drawable nuevo.
                        if (it.pct >= 0) LucideIcons.ChevronUp else LucideIcons.ChevronDown,
                        contentDescription = null,
                        tint = if (it.isGood) finance.income else finance.expense,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
            delta?.let {
                Text(
                    "${if (it.pct >= 0) "+" else ""}${it.pct}% vs ${it.vsLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (it.isGood) finance.income else finance.expense,
                    maxLines = 1,
                )
            }
            caption?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
        }
    }
}

/** Fila resumen de suscripciones: icono + etiqueta + monto mensual + chevron, tocable → abre
 * Suscripciones (mismo patrón de fila-resumen-navegable que Sage.dc.html). Envuelta en
 * FinanceCard para que quede consistente con el resto de bloques del dashboard (todos con
 * superficie/sombra de tarjeta) en vez del tratamiento "al ras" del handoff original.
 */
@Composable
private fun SubscriptionsSummaryRow(valueText: String, masked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    FinanceCard(modifier = modifier, onClick = onClick, contentPadding = PaddingValues(spacing.md)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            // Mismas medidas que IconAvatar (40dp, glifo a la mitad), que acá no sirve porque recibe
            // ImageVector y este icono es un Painter Lucide.
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(LocalAccentColor.current),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    LucideIcons.Repeat,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                "SUSCRIPCIONES",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                valueText,
                // Mismo tamaño/peso que los montos de fila del dashboard (saldo de cuenta, gasto por
                // categoría, gasto frecuente): bodyLarge+SemiBold, no el titleSmall+Bold de KpiTile.
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                modifier = if (masked) Modifier.semantics { contentDescription = "Monto oculto" } else Modifier,
            )
            Icon(
                LucideIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
