package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finanzen.domain.FundPlan
import com.finanzen.domain.RateMode
import com.finanzen.platform.PlatformBackHandler
import com.finanzen.ui.components.BottomSnackbarHost
import com.finanzen.ui.components.ConfirmDialog
import com.finanzen.ui.components.InlineNotice
import com.finanzen.ui.components.MoneyField
import com.finanzen.ui.components.NoticeTone
import com.finanzen.ui.components.SageButton
import com.finanzen.ui.components.SageButtonKind
import com.finanzen.ui.components.SageTextField
import com.finanzen.ui.components.ScreenColumn
import com.finanzen.ui.components.SectionHeader
import com.finanzen.ui.components.SegmentedControl
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.format.parsePercent
import com.finanzen.ui.format.percentFieldText
import com.finanzen.ui.theme.LocalSageType
import com.finanzen.viewmodel.FundSimulationsViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/** Pasos de la simulación: los datos, la proyección y la comparación con otra simulación guardada. */
private enum class FundStep { Form, Projection, Compare }

/**
 * Simulación de un fondo: formulario → proyección → comparación, con su propia pila de pasos para
 * que el borrador sin guardar no tenga que viajar entre rutas. [simulationId] nulo = simulación nueva.
 */
@Composable
fun FundSimulationScreen(simulationId: Long?, onBack: () -> Unit, vm: FundSimulationsViewModel = koinViewModel()) {
    val saved by vm.simulations.collectAsState()
    var plan by remember { mutableStateOf(simulationId?.let(vm::byId) ?: vm.newPlan()) }
    var savedPlan by remember { mutableStateOf(plan.takeIf { it.id != null }) }
    var stack by remember { mutableStateOf(listOf(if (plan.id != null) FundStep.Projection else FundStep.Form)) }
    var confirmDelete by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val goBack: () -> Unit = { if (stack.size > 1) stack = stack.dropLast(1) else onBack() }
    PlatformBackHandler(enabled = stack.size > 1) { stack = stack.dropLast(1) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            when (stack.last()) {
                FundStep.Form -> {
                    SubScreenHeader(if (plan.id == null) "Simular fondo" else "Editar simulación", onBack = goBack)
                    FundForm(
                        initial = plan,
                        onSimulate = { edited ->
                            plan = edited
                            stack = if (FundStep.Projection in stack) stack.dropLast(1) else stack + FundStep.Projection
                        },
                        onDelete = if (plan.id != null) ({ confirmDelete = true }) else null,
                    )
                }

                FundStep.Projection -> {
                    SubScreenHeader(plan.name, onBack = goBack, action = {
                        SageButton("Editar", onClick = { stack = stack + FundStep.Form }, kind = SageButtonKind.Ghost, small = true)
                    })
                    FundProjectionContent(
                        plan = plan,
                        isSaved = savedPlan == plan,
                        onCompare = { stack = stack + FundStep.Compare },
                        onSave = {
                            vm.save(plan)?.let { id ->
                                plan = plan.copy(id = id)
                                savedPlan = plan
                                scope.launch { snackbar.showSnackbar("Simulación guardada") }
                            }
                        },
                    )
                }

                FundStep.Compare -> {
                    SubScreenHeader("Comparar fondos", onBack = goBack)
                    FundCompareContent(plan = plan, others = saved.filter { it.id != plan.id })
                }
            }
        }
        BottomSnackbarHost(snackbar)
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "¿Eliminar \"${plan.name}\"?",
            body = "La simulación se borra de tus simulaciones guardadas. No afecta tus fondos reales.",
            onConfirm = {
                confirmDelete = false
                plan.id?.let(vm::delete)
                onBack()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/**
 * Lo que el usuario escribe en el formulario, tal cual (texto), y su conversión a [FundPlan].
 * Puro para poder probar la conversión sin UI.
 */
internal data class FundFormState(
    val name: String,
    val initialMinor: Long,
    val monthlyMinor: Long,
    val years: String,
    val rateMode: RateMode,
    val conservative: String,
    val moderate: String,
    val aggressive: String,
    val fixed: String,
    val adminFee: String,
    val contributionFee: String,
) {
    /** El plan resultante sobre [base] (conserva id y moneda), o los errores que impiden calcularlo. */
    fun toPlan(base: FundPlan): Pair<FundPlan?, List<String>> {
        val years = years.trim().toIntOrNull() ?: return null to listOf("Escribe el plazo en años, por ejemplo 10.")
        val texts = listOf(conservative, moderate, aggressive, fixed, adminFee, contributionFee)
        val rates = texts.map { if (it.isBlank()) 0.0 else parsePercent(it) ?: return null to listOf("Revisa los porcentajes: usa solo números, por ejemplo 1,5.") }
        val (con, mod, agr) = rates
        val (fix, admin, contribution) = rates.drop(SCENARIO_COUNT)
        val plan = base.copy(
            name = name.trim(),
            initialMinor = initialMinor,
            monthlyMinor = monthlyMinor,
            months = years * MONTHS_IN_YEAR,
            rateMode = rateMode,
            conservativeRate = con,
            moderateRate = mod,
            aggressiveRate = agr,
            fixedRate = fix,
            adminFee = admin,
            contributionFee = contribution,
        )
        val errors = plan.validate()
        return (if (errors.isEmpty()) plan else null) to errors
    }

    companion object {
        fun from(plan: FundPlan) = FundFormState(
            name = plan.name,
            initialMinor = plan.initialMinor,
            monthlyMinor = plan.monthlyMinor,
            years = (plan.months / MONTHS_IN_YEAR).toString(),
            rateMode = plan.rateMode,
            conservative = percentFieldText(plan.conservativeRate),
            moderate = percentFieldText(plan.moderateRate),
            aggressive = percentFieldText(plan.aggressiveRate),
            fixed = percentFieldText(plan.fixedRate),
            adminFee = percentFieldText(plan.adminFee),
            contributionFee = percentFieldText(plan.contributionFee),
        )
    }
}

@Composable
private fun FundForm(initial: FundPlan, onSimulate: (FundPlan) -> Unit, onDelete: (() -> Unit)?) {
    var form by remember { mutableStateOf(FundFormState.from(initial)) }
    var errors by remember { mutableStateOf(emptyList<String>()) }
    ScreenColumn {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionHeader("Aportes")
            SageTextField(form.name, { form = form.copy(name = it) }, label = "Nombre de la simulación", placeholder = "Fondo A")
            MoneyField(form.initialMinor, { form = form.copy(initialMinor = it) }, initial.currency, label = "Aporte inicial")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MoneyField(form.monthlyMinor, { form = form.copy(monthlyMinor = it) }, initial.currency, "Aporte mensual", Modifier.weight(1f))
                SageTextField(
                    form.years,
                    { form = form.copy(years = it.filter(Char::isDigit).take(MAX_YEAR_DIGITS)) },
                    label = "Plazo",
                    modifier = Modifier.weight(1f),
                    suffix = "años",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        }
        RateSection(form) { form = it }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionHeader("Comisiones")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PercentField("Administración", form.adminFee, { form = form.copy(adminFee = it) }, Modifier.weight(1f), "% anual", "Sobre el saldo.")
                PercentField("Por aporte", form.contributionFee, { form = form.copy(contributionFee = it) }, Modifier.weight(1f), help = "Sobre cada aporte.")
            }
        }
        if (errors.isNotEmpty()) InlineNotice(errors.joinToString("\n"), tone = NoticeTone.Warning)
        SageButton("Simular", onClick = {
            val (plan, problems) = form.toPlan(initial)
            errors = problems
            plan?.let(onSimulate)
        }, modifier = Modifier.fillMaxWidth())
        if (onDelete != null) SageButton("Eliminar simulación", onClick = onDelete, kind = SageButtonKind.Danger, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun RateSection(form: FundFormState, onChange: (FundFormState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader("Rentabilidad esperada")
        SegmentedControl(
            options = RateMode.entries,
            selected = form.rateMode,
            onSelect = { onChange(form.copy(rateMode = it)) },
            label = { if (it == RateMode.SCENARIOS) "Tres escenarios" else "Tasa fija" },
            fill = false,
        )
        if (form.rateMode == RateMode.SCENARIOS) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PercentField("Conservador", form.conservative, { onChange(form.copy(conservative = it)) }, Modifier.weight(1f))
                PercentField("Moderado", form.moderate, { onChange(form.copy(moderate = it)) }, Modifier.weight(1f))
                PercentField("Agresivo", form.aggressive, { onChange(form.copy(aggressive = it)) }, Modifier.weight(1f))
            }
        } else {
            PercentField("Tasa anual", form.fixed, { onChange(form.copy(fixed = it)) }, Modifier.fillMaxWidth(), "% anual")
        }
        Text(
            "Tasa efectiva anual, antes de comisiones. Es una estimación: ningún fondo garantiza su rentabilidad.",
            style = LocalSageType.current.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PercentField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier, suffix: String = "%", help: String? = null) {
    SageTextField(
        value = value,
        onValueChange = { text -> onChange(text.filter { it.isDigit() || it == ',' || it == '.' || it == '-' }.take(MAX_PERCENT_CHARS)) },
        label = label,
        modifier = modifier,
        suffix = suffix,
        help = help,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

internal const val MONTHS_IN_YEAR = 12
private const val MAX_PERCENT_CHARS = 6
private const val MAX_YEAR_DIGITS = 2
private const val SCENARIO_COUNT = 3
