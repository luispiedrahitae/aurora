package com.finanzen.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finanzen.data.RESERVED_CATEGORY_NAMES
import com.finanzen.db.Subscription
import com.finanzen.db.TransactionRow
import com.finanzen.domain.InstallmentMath
import com.finanzen.ui.components.CategoryAvatar
import com.finanzen.ui.components.ConfirmDialog
import com.finanzen.ui.components.DateField
import com.finanzen.ui.components.DiscardChangesDialog
import com.finanzen.ui.components.FieldError
import com.finanzen.ui.components.FieldLabel
import com.finanzen.ui.components.MoneyField
import com.finanzen.ui.components.PickerField
import com.finanzen.ui.components.SageButton
import com.finanzen.ui.components.SageChip
import com.finanzen.ui.components.SageTextField
import com.finanzen.ui.components.SegmentedControl
import com.finanzen.ui.components.SheetHeader
import com.finanzen.ui.components.SwitchRow
import com.finanzen.ui.components.accountTypeLabel
import com.finanzen.ui.components.kindLabel
import com.finanzen.ui.components.rememberGuardedSheetState
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSageType
import com.finanzen.viewmodel.InvestmentsViewModel
import com.finanzen.viewmodel.SubscriptionsViewModel
import com.finanzen.viewmodel.TransactionsViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

/** Alta/edición de movimiento, en un [ModalBottomSheet] (no una pantalla completa) — mismo patrón
 * que `AccountFormDialog` en `AccountsTabScreen.kt`. El estado de qué mostrar (nuevo/edición) vive
 * elevado en `AppNav` porque el sheet se abre desde tres sitios (FAB, Movimientos, Calendario).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    transactionId: Long?,
    onDismiss: () -> Unit,
    initialKind: String? = null,
    // Abrir directo el maestro desde Suscripciones/Inversiones, sin pasar por un movimiento vinculado.
    subscriptionId: Long? = null,
    investmentId: Long? = null,
    vm: TransactionsViewModel = koinViewModel(),
    subVm: SubscriptionsViewModel = koinViewModel(),
    invVm: InvestmentsViewModel = koinViewModel(),
) {
    val accounts by vm.accounts.collectAsState()
    val allAccounts by vm.allAccounts.collectAsState()
    val categories by vm.categories.collectAsState()

    val existing = remember(transactionId) { transactionId?.let(vm::transactionById) }
    // Si este movimiento vino de una suscripción/inversión, se edita el maestro (nombre/monto/cuenta/
    // recurrencia) en vez del TransactionRow puntual.
    val editingSubscription = remember(existing, subscriptionId) {
        (subscriptionId ?: existing?.subscriptionId)?.let(subVm::subscriptionById)
    }
    val editingInvestment = remember(existing, investmentId) {
        (investmentId ?: existing?.investmentId)?.let(invVm::investmentById)
    }
    val isLinkedEdit = editingSubscription != null || editingInvestment != null
    var kind by remember { mutableStateOf(existing?.kind ?: initialKind ?: "EXPENSE") }
    var accountId by remember {
        mutableStateOf(editingSubscription?.accountId ?: editingInvestment?.accountId ?: existing?.accountId)
    }
    var toAccountId by remember { mutableStateOf(existing?.transferAccountId) }
    // categoryId es solo el filtro de categoría padre en la UI; lo que se guarda de verdad es
    // subcategoryId (categoryId en TransactionRow siempre apunta a una hoja — ver Transaction.sq).
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var subcategoryId by remember { mutableStateOf<Long?>(null) }
    var amountMinor by remember {
        mutableStateOf(editingSubscription?.amountMinor ?: editingInvestment?.amountMinor ?: existing?.amountMinor ?: 0L)
    }
    var error by remember { mutableStateOf<String?>(null) }
    var showAddSubcategory by remember { mutableStateOf(false) }
    var newSubcategoryName by remember { mutableStateOf("") }
    var dateEpochDay by remember {
        mutableStateOf(existing?.date ?: Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays())
    }
    var cuotasText by remember { mutableStateOf("") }
    var blockedDelete by remember { mutableStateOf<TransactionsViewModel.DeleteBlock?>(null) }
    var confirmDelete by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    // Solo para kind == SUBSCRIPTION/INVESTMENT (alta) o isLinkedEdit (edición del maestro): estos dos
    // tipos no son un TransactionRow.kind real (ver Transaction.sq), así que su alta/edición se resuelve
    // con SubscriptionsViewModel/InvestmentsViewModel en vez de TransactionsViewModel.save — mismos
    // campos que SubscriptionFormDialog/InvestmentFormDialog.
    val linkedFrequency = editingSubscription?.frequency ?: editingInvestment?.frequency
    val linkedInterval = editingSubscription?.intervalCount ?: editingInvestment?.intervalCount
    var movementName by remember { mutableStateOf(editingSubscription?.name ?: editingInvestment?.name ?: "") }
    var recurMonthly by remember { mutableStateOf(linkedFrequency?.let { it == "MONTHLY" } ?: true) } // true = Día del mes, false = Cada X días
    var dayOfMonth by remember { mutableStateOf(linkedInterval?.takeIf { linkedFrequency == "MONTHLY" }?.toString() ?: "1") }
    var everyDays by remember { mutableStateOf(linkedInterval?.takeIf { linkedFrequency == "DAILY" }?.toString() ?: "30") }
    var periodic by remember { mutableStateOf(editingInvestment?.let { it.periodic == 1L } ?: false) } // solo INVESTMENT

    // En edición, existing.categoryId es la hoja guardada; espera a que categories cargue para
    // resolver su padre y precargar ambos pickers. El guard evita pisar una selección ya en curso.
    LaunchedEffect(existing, categories) {
        if (existing != null && subcategoryId == null) {
            categories.firstOrNull { it.id == existing.categoryId }?.let { leaf ->
                categoryId = leaf.parentId
                subcategoryId = leaf.id
            }
        }
    }

    val effectiveKind = when {
        editingSubscription != null -> "SUBSCRIPTION"
        editingInvestment != null -> "INVESTMENT"
        else -> kind
    }
    val isTransfer = effectiveKind == "TRANSFER"
    val isRecurringType = effectiveKind == "SUBSCRIPTION" || effectiveKind == "INVESTMENT"
    val selectedAccount = accounts.firstOrNull { it.id == accountId }
        ?: existing?.let { allAccounts.firstOrNull { a -> a.id == accountId } }
        ?: accounts.firstOrNull()
    val destOptions = accounts.filter { it.id != selectedAccount?.id }
    val selectedDest = accounts.firstOrNull { it.id == toAccountId }
        ?: existing?.let { allAccounts.firstOrNull { a -> a.id == toAccountId } }
    val categoryOptions = categories.filter { it.kind == kind && it.parentId == null && it.name !in RESERVED_CATEGORY_NAMES }
    val selectedCategory = categoryOptions.firstOrNull { it.id == categoryId }
    val subcategoryOptions = selectedCategory?.let { cat -> categories.filter { it.parentId == cat.id } } ?: emptyList()
    val selectedSubcategory = subcategoryOptions.firstOrNull { it.id == subcategoryId }
    val finance = LocalFinanceColors.current
    val haptic = LocalHapticFeedback.current

    // Un error viejo no debe quedarse después de que el usuario corrige el campo.
    LaunchedEffect(amountMinor, categoryId, subcategoryId, accountId, toAccountId, movementName, dayOfMonth, everyDays, recurMonthly) {
        error = null
    }

    // Cambios sin guardar: un roce que cierre la hoja no debe tirar lo escrito.
    val initialAmount = remember { amountMinor }
    val initialAccountId = remember { accountId }
    val initialName = remember { movementName }
    val initialDate = remember { dateEpochDay }
    val dirty = amountMinor != initialAmount ||
        accountId != initialAccountId ||
        movementName != initialName ||
        dateEpochDay != initialDate ||
        toAccountId != existing?.transferAccountId ||
        (subcategoryId != null && subcategoryId != existing?.categoryId) ||
        cuotasText.isNotEmpty()
    var confirmDiscard by remember { mutableStateOf(false) }
    val requestDismiss = { if (dirty) confirmDiscard = true else onDismiss() }
    if (confirmDiscard) DiscardChangesDialog(onDiscard = onDismiss, onKeepEditing = { confirmDiscard = false })
    val sheetState = rememberGuardedSheetState(dirty) { confirmDiscard = true }
    ModalBottomSheet(
        onDismissRequest = requestDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SheetHeader(
                title = when {
                    editingSubscription != null -> "Editar suscripción"
                    editingInvestment != null -> "Editar aporte a fondo"
                    existing == null && kind == "SUBSCRIPTION" -> "Nueva suscripción"
                    existing == null && kind == "INVESTMENT" -> "Nuevo aporte a fondo"
                    existing == null -> "Nuevo movimiento"
                    else -> "Editar movimiento"
                },
                onDismiss = requestDismiss,
            )

            // Editar un movimiento vinculado a una suscripción/inversión reemplaza el selector de tipo
            // por sus propios campos (más abajo) — no tiene sentido "cambiar el tipo" de algo que ya
            // está fijado por el vínculo.
            if (!isLinkedEdit) {
                val selectKind: (String) -> Unit = { k ->
                    kind = k
                    categoryId = null
                    subcategoryId = null
                    showAddSubcategory = false
                    newSubcategoryName = ""
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SegmentedControl(
                        options = listOf("EXPENSE", "INCOME", "TRANSFER"),
                        // Suscripción e Inversión no son pestañas: con una de ellas elegida no se marca ninguna.
                        selected = kind,
                        onSelect = selectKind,
                        label = { kindLabel(it) },
                    )
                    // Suscripción/Inversión solo al crear: editar un movimiento sin vínculo edita solo esa fila.
                    if (existing == null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("También:", style = LocalSageType.current.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            SageChip(
                                "Suscripción",
                                onClick = { selectKind(if (kind == "SUBSCRIPTION") "EXPENSE" else "SUBSCRIPTION") },
                                selected = kind == "SUBSCRIPTION",
                                icon = LucideIcons.Repeat,
                            )
                            SageChip(
                                "Aporte a fondo",
                                onClick = { selectKind(if (kind == "INVESTMENT") "EXPENSE" else "INVESTMENT") },
                                selected = kind == "INVESTMENT",
                                icon = LucideIcons.TrendingUp,
                            )
                        }
                    }
                }
            }

            if (isRecurringType) {
                SageTextField(
                    value = movementName,
                    onValueChange = { movementName = it },
                    label = if (effectiveKind == "SUBSCRIPTION") "Nombre de la suscripción" else "Nombre del fondo",
                    placeholder = if (effectiveKind == "SUBSCRIPTION") "Ej.: Netflix" else "Ej.: Fondo de emergencia",
                    error = NAME_ERROR.takeIf { error == NAME_ERROR },
                )
            }

            MoneyField(
                amountMinor = amountMinor,
                onAmountChange = { amountMinor = it },
                currencyCode = selectedAccount?.currency ?: "",
                label = "Monto",
                modifier = Modifier.fillMaxWidth(),
            )
            if (error == AMOUNT_ERROR) FieldError(AMOUNT_ERROR)

            PickerField(
                label = if (isTransfer) "Cuenta origen" else "Cuenta",
                options = accounts,
                selected = selectedAccount,
                optionLabel = { "${it.name} (${it.currency})" },
                trailingLabel = { accountTypeLabel(it.type) },
                onSelect = { accountId = it.id },
                placeholder = "Selecciona cuenta",
                emptyHint = "Crea una cuenta primero (pestaña Cuentas).",
            )

            if (isTransfer) {
                PickerField(
                    label = "Cuenta destino",
                    options = destOptions,
                    selected = selectedDest,
                    optionLabel = { "${it.name} (${it.currency})" },
                    trailingLabel = { accountTypeLabel(it.type) },
                    onSelect = { toAccountId = it.id },
                    placeholder = "Selecciona destino",
                    emptyHint = "Necesitas al menos dos cuentas para transferir.",
                )
            } else if (!isRecurringType) {
                PickerField(
                    label = "Categoría",
                    options = categoryOptions,
                    selected = selectedCategory,
                    optionLabel = { it.name },
                    onSelect = {
                        categoryId = it.id
                        subcategoryId = null
                    },
                    placeholder = "Selecciona categoría",
                    emptyHint = "No hay categorías de ${if (kind == "EXPENSE") "gasto" else "ingreso"}.",
                    leadingContent = { cat -> CategoryAvatar(cat.name) },
                )
                if (error == CATEGORY_ERROR) FieldError(CATEGORY_ERROR)
            }

            if (!isTransfer && !isRecurringType) {
                val subcategoryNameTaken = newSubcategoryName.isNotBlank() &&
                    newSubcategoryName.trim().lowercase() in subcategoryOptions.map { it.name.trim().lowercase() }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        // Bottom, no CenterVertically: PickerField con plainLabel dibuja su propia
                        // etiqueta arriba del campo, así que el bloque completo es más alto que el
                        // "+" — centrar contra esa altura entera dejaba el botón desalineado con la
                        // caja del campo. Alineando al fondo, el "+" queda a la altura de la caja.
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PickerField(
                            label = "Subcategoría",
                            options = subcategoryOptions,
                            selected = selectedSubcategory,
                            optionLabel = { it.name },
                            onSelect = { subcategoryId = it.id },
                            placeholder = "Selecciona subcategoría",
                            emptyHint = selectedCategory?.let { "Aún no hay subcategorías en ${it.name}." }
                                ?: "Primero selecciona una categoría.",
                            modifier = Modifier.weight(1f),
                        )
                        if (!showAddSubcategory) {
                            OutlinedIconButton(
                                onClick = { showAddSubcategory = true },
                                enabled = selectedCategory != null,
                                shape = MaterialTheme.shapes.small,
                                colors = IconButtonDefaults.outlinedIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.size(56.dp),
                            ) {
                                Icon(LucideIcons.Plus, contentDescription = "Nueva subcategoría")
                            }
                        }
                    }
                    // Revelación inline (no un diálogo aparte) — mismo patrón que Sage.dc.html:
                    // el "+" se reemplaza por un campo "Nombre" + botón "Añadir" dentro del formulario.
                    if (showAddSubcategory) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SageTextField(
                                value = newSubcategoryName,
                                onValueChange = { newSubcategoryName = it },
                                label = "Nueva subcategoría",
                                placeholder = "Nombre",
                                modifier = Modifier.weight(1f),
                            )
                            SageButton(
                                "Añadir",
                                onClick = {
                                    selectedCategory?.id?.let { catId ->
                                        subcategoryId = vm.addSubcategory(newSubcategoryName, kind, catId)
                                        showAddSubcategory = false
                                        newSubcategoryName = ""
                                    }
                                },
                                enabled = selectedCategory != null && newSubcategoryName.isNotBlank() && !subcategoryNameTaken,
                            )
                        }
                        if (subcategoryNameTaken) FieldError("Ya existe una subcategoría con ese nombre")
                    }
                    if (error == SUBCATEGORY_ERROR) FieldError(SUBCATEGORY_ERROR)
                }
            }

            // Cuotas: solo para gastos con tarjeta de crédito. El interés se toma de la tarjeta,
            // no se pide aquí — Card.interestRate ya lo tiene guardado.
            if (!isTransfer && !isRecurringType && kind == "EXPENSE" && selectedAccount?.type == "CREDIT") {
                val installments = cuotasText.toLongOrNull()?.coerceIn(1, MAX_INSTALLMENTS) ?: 1
                LabeledNumberField(
                    label = "Cuotas",
                    value = cuotasText,
                    // Solo dígitos: el teclado numérico no protege de un teclado físico o un pegado.
                    onValueChange = { cuotasText = it.filter(Char::isDigit).take(3) },
                    placeholder = "1",
                    error = "Máximo $MAX_INSTALLMENTS cuotas; se usará $MAX_INSTALLMENTS.".takeIf { (cuotasText.toLongOrNull() ?: 0) > MAX_INSTALLMENTS },
                )
                if (installments > 1 && amountMinor > 0) {
                    val rate = vm.cardFor(selectedAccount.id)?.interestRate ?: 0.0
                    val monthly = InstallmentMath.monthlyPaymentMinor(amountMinor, installments, rate)
                    Text(
                        "Se registrará la cuota 1 (${LocalMoneyFormat.current.format(monthly, selectedAccount.currency)}) " +
                            "este mes. Las próximas ${installments - 1} cuotas se agregan automáticamente cada mes " +
                            "hasta completar el plan, con el interés de tu tarjeta.",
                        style = LocalSageType.current.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // No aplica a isLinkedEdit: no se toca nextChargeDate/nextContributionDate desde aquí, eso
            // lo sigue gestionando solo el catch-up existente.
            if (!isLinkedEdit) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FieldLabel("Fecha")
                    DateField(
                        epochDay = dateEpochDay,
                        onEpochDayChange = { dateEpochDay = it },
                        plain = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (effectiveKind == "INVESTMENT") {
                SwitchRow(
                    title = "Aporte periódico",
                    subtitle = "Sage registra el aporte solo cada vez que toque.",
                    checked = periodic,
                    onCheckedChange = { periodic = it },
                )
            }
            if (effectiveKind == "SUBSCRIPTION" || (effectiveKind == "INVESTMENT" && periodic)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("Se repite")
                    SegmentedControl(
                        options = listOf(true, false),
                        selected = recurMonthly,
                        onSelect = { recurMonthly = it },
                        label = { if (it) "Un día del mes" else "Cada X días" },
                    )
                    if (recurMonthly) {
                        LabeledNumberField(
                            label = "Día del mes (1–31)",
                            value = dayOfMonth,
                            onValueChange = { dayOfMonth = it.filter(Char::isDigit).take(2) },
                            error = "Debe ser un día entre 1 y 31".takeIf {
                                error == RECURRENCE_ERROR || (dayOfMonth.isNotEmpty() && dayOfMonth.toIntOrNull()?.let { it in 1..31 } != true)
                            },
                        )
                    } else {
                        LabeledNumberField(
                            label = "Cada cuántos días",
                            value = everyDays,
                            onValueChange = { everyDays = it.filter(Char::isDigit).take(4) },
                            error = "Debe ser al menos 1 día".takeIf {
                                error == RECURRENCE_ERROR || (everyDays.isNotEmpty() && everyDays.toLongOrNull()?.let { it >= 1 } != true)
                            },
                        )
                    }
                }
            }

            // Los errores de un campo concreto ya se muestran bajo ese campo; aquí quedan los generales.
            error?.takeIf { it !in FIELD_ERRORS }?.let { FieldError(it) }

            SageButton(
                text = when {
                    editingSubscription != null -> "Guardar suscripción"
                    editingInvestment != null -> "Guardar aporte"
                    effectiveKind == "SUBSCRIPTION" -> "Guardar suscripción"
                    effectiveKind == "INVESTMENT" -> "Guardar aporte"
                    effectiveKind == "INCOME" -> "Guardar ingreso"
                    effectiveKind == "TRANSFER" -> "Guardar transferencia"
                    else -> "Guardar gasto"
                },
                onClick = {
                    val account = selectedAccount
                    val minor = amountMinor
                    val recurValid = if (recurMonthly) {
                        dayOfMonth.toIntOrNull()?.let { it in 1..31 } == true
                    } else {
                        everyDays.toLongOrNull()?.let { it >= 1 } == true
                    }
                    if (editingSubscription != null) {
                        when {
                            movementName.isBlank() -> error = NAME_ERROR

                            minor <= 0 -> error = AMOUNT_ERROR

                            !recurValid -> error = RECURRENCE_ERROR

                            else -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val frequency = if (recurMonthly) "MONTHLY" else "DAILY"
                                val interval = if (recurMonthly) dayOfMonth.toLong() else everyDays.toLong()
                                subVm.updateSubscription(editingSubscription.id, movementName.trim(), minor, frequency, interval, account?.id)
                                onDismiss()
                            }
                        }
                    } else if (editingInvestment != null) {
                        when {
                            movementName.isBlank() -> error = NAME_ERROR

                            account == null -> error = "Crea una cuenta primero (pestaña Cuentas)."

                            minor <= 0 -> error = AMOUNT_ERROR

                            periodic && !recurValid -> error = RECURRENCE_ERROR

                            else -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val frequency = if (!periodic) {
                                    null
                                } else if (recurMonthly) {
                                    "MONTHLY"
                                } else {
                                    "DAILY"
                                }
                                val interval = if (!periodic) {
                                    null
                                } else if (recurMonthly) {
                                    dayOfMonth.toLong()
                                } else {
                                    everyDays.toLong()
                                }
                                invVm.updateInvestment(editingInvestment.id, movementName.trim(), minor, account.id, periodic, frequency, interval)
                                onDismiss()
                            }
                        }
                    } else if (isTransfer) {
                        val dest = selectedDest
                        when {
                            account == null || dest == null -> error = "Elige cuenta origen y destino."

                            account.id == dest.id -> error = "Origen y destino deben ser distintos."

                            minor <= 0 -> error = AMOUNT_ERROR

                            else -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.saveTransfer(existing?.id, account.id, dest.id, minor, "", dateEpochDay)
                                onDismiss()
                            }
                        }
                    } else if (kind == "SUBSCRIPTION") {
                        when {
                            movementName.isBlank() -> error = NAME_ERROR

                            minor <= 0 -> error = AMOUNT_ERROR

                            !recurValid -> error = RECURRENCE_ERROR

                            else -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val frequency = if (recurMonthly) "MONTHLY" else "DAILY"
                                val interval = if (recurMonthly) dayOfMonth.toLong() else everyDays.toLong()
                                subVm.addSubscription(movementName.trim(), minor, frequency, interval, account?.id, dateEpochDay)
                                onDismiss()
                            }
                        }
                    } else if (kind == "INVESTMENT") {
                        when {
                            movementName.isBlank() -> error = NAME_ERROR

                            account == null -> error = "Crea una cuenta primero (pestaña Cuentas)."

                            minor <= 0 -> error = AMOUNT_ERROR

                            periodic && !recurValid -> error = RECURRENCE_ERROR

                            else -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val frequency = if (!periodic) {
                                    null
                                } else if (recurMonthly) {
                                    "MONTHLY"
                                } else {
                                    "DAILY"
                                }
                                val interval = if (!periodic) {
                                    null
                                } else if (recurMonthly) {
                                    dayOfMonth.toLong()
                                } else {
                                    everyDays.toLong()
                                }
                                invVm.addInvestment(movementName.trim(), minor, account.id, periodic, frequency, interval, dateEpochDay)
                                onDismiss()
                            }
                        }
                    } else {
                        when {
                            account == null -> error = "Crea una cuenta primero (pestaña Cuentas)."

                            categoryId == null -> error = CATEGORY_ERROR

                            subcategoryId == null -> error = SUBCATEGORY_ERROR

                            minor <= 0 -> error = AMOUNT_ERROR

                            else -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                // Sin campo de nota manual, la nota es siempre el nombre de la
                                // subcategoría/categoría (y así la búsqueda la encuentra).
                                val autoNote = selectedSubcategory?.name ?: selectedCategory?.name ?: ""
                                vm.save(
                                    id = existing?.id,
                                    accountId = account.id,
                                    categoryId = subcategoryId!!,
                                    amountMinor = minor,
                                    kind = kind,
                                    note = autoNote,
                                    dateEpochDay = dateEpochDay,
                                    installments = cuotasText.toLongOrNull()?.coerceIn(1, MAX_INSTALLMENTS) ?: 1,
                                )
                                onDismiss()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            if (editingSubscription != null) {
                TextButton(
                    onClick = {
                        confirmDelete = "esta suscripción" to {
                            subVm.deleteSubscription(editingSubscription.id)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text("Eliminar suscripción", style = LocalSageType.current.label) }
            } else if (editingInvestment != null) {
                TextButton(
                    onClick = {
                        confirmDelete = "esta inversión" to {
                            invVm.deleteInvestment(editingInvestment.id)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text("Eliminar aporte", style = LocalSageType.current.label) }
            } else if (existing != null) {
                TextButton(
                    onClick = {
                        val block = vm.deleteBlockReason(existing.id)
                        if (block != null) {
                            blockedDelete = block
                        } else {
                            confirmDelete = "este movimiento" to {
                                vm.delete(existing.id)
                                onDismiss()
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text("Eliminar movimiento", style = LocalSageType.current.label) }
            }
        }
    }

    // Desde el formulario no hay snackbar de "Deshacer" (el sheet se cierra), así que se confirma antes.
    confirmDelete?.let { (what, delete) ->
        ConfirmDialog(
            title = "¿Eliminar $what?",
            body = "Esta acción no se puede deshacer.",
            onConfirm = {
                confirmDelete = null
                delete()
            },
            onDismiss = { confirmDelete = null },
        )
    }

    blockedDelete?.let { block ->
        ConfirmDialog(
            title = "No se puede eliminar",
            body = when (block) {
                is TransactionsViewModel.DeleteBlock.Subscription ->
                    "\"${block.name}\" es una suscripción periódica. Elimínala desde Suscripciones; después podrás borrar este movimiento aquí."

                is TransactionsViewModel.DeleteBlock.Investment ->
                    "\"${block.name}\" es un aporte periódico a un fondo. Gestiónalo desde Fondos de inversión."
            },
            confirmLabel = "Entendido",
            onConfirm = { blockedDelete = null },
            onDismiss = { blockedDelete = null },
            destructive = false,
        )
    }
}

private const val MAX_INSTALLMENTS = 360L

/** Campo numérico con la etiqueta encima y el error debajo, igual que los demás campos de la hoja. */
@Composable
private fun LabeledNumberField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String? = null, error: String? = null) {
    SageTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        error = error,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

private const val AMOUNT_ERROR = "Escribe un monto mayor que 0."
private const val NAME_ERROR = "Escribe un nombre."
private const val CATEGORY_ERROR = "Elige una categoría."
private const val SUBCATEGORY_ERROR = "Elige una subcategoría."
private const val RECURRENCE_ERROR = "Recurrencia inválida."
private val FIELD_ERRORS = setOf(AMOUNT_ERROR, NAME_ERROR, CATEGORY_ERROR, SUBCATEGORY_ERROR, RECURRENCE_ERROR)
