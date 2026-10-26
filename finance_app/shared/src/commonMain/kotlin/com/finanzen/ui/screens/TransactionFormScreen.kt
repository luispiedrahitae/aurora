package com.finanzen.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finanzen.data.RESERVED_CATEGORY_NAMES
import com.finanzen.domain.InstallmentMath
import com.finanzen.ui.components.CategoryAvatar
import com.finanzen.ui.components.DateField
import com.finanzen.ui.components.MoneyField
import com.finanzen.ui.components.PickerField
import com.finanzen.ui.components.SelectorPill
import com.finanzen.ui.components.SheetHeader
import com.finanzen.ui.components.accountTypeLabel
import com.finanzen.ui.components.sageFieldColors
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.PillShape
import com.finanzen.ui.theme.displayFontFamily
import com.finanzen.viewmodel.InvestmentsViewModel
import com.finanzen.viewmodel.SubscriptionsViewModel
import com.finanzen.viewmodel.TransactionsViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.compose.viewmodel.koinViewModel

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
    val editingSubscription = remember(existing) { existing?.subscriptionId?.let(subVm::subscriptionById) }
    val editingInvestment = remember(existing) { existing?.investmentId?.let(invVm::investmentById) }
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
        mutableStateOf(existing?.date ?: Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays().toLong())
    }
    var cuotasText by remember { mutableStateOf("") }
    var blockedDelete by remember { mutableStateOf<TransactionsViewModel.DeleteBlock?>(null) }

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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, dragHandle = null) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SheetHeader(
                title = when {
                    editingSubscription != null -> "Editar suscripción"
                    editingInvestment != null -> "Editar inversión"
                    existing == null -> "Nuevo movimiento"
                    else -> "Editar movimiento"
                },
                onDismiss = onDismiss,
            )

            // Editar un movimiento vinculado a una suscripción/inversión reemplaza el selector de tipo
            // por sus propios campos (más abajo) — no tiene sentido "cambiar el tipo" de algo que ya
            // está fijado por el vínculo.
            if (!isLinkedEdit) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectorPill(
                            text = "Gasto",
                            selected = kind == "EXPENSE",
                            modifier = Modifier.weight(1f),
                        ) {
                            kind = "EXPENSE"
                            categoryId = null
                            subcategoryId = null
                            showAddSubcategory = false
                            newSubcategoryName = ""
                        }
                        SelectorPill(
                            text = "Ingreso",
                            selected = kind == "INCOME",
                            modifier = Modifier.weight(1f),
                        ) {
                            kind = "INCOME"
                            categoryId = null
                            subcategoryId = null
                            showAddSubcategory = false
                            newSubcategoryName = ""
                        }
                        SelectorPill(
                            text = "Transferencia",
                            selected = isTransfer,
                            modifier = Modifier.weight(1f),
                        ) {
                            kind = "TRANSFER"
                            categoryId = null
                            subcategoryId = null
                            showAddSubcategory = false
                            newSubcategoryName = ""
                        }
                    }
                    // Suscripción/Inversión solo al crear: editar un movimiento existente sin vínculo
                    // sigue editando solo el TransactionRow.
                    if (existing == null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectorPill(
                                text = "Suscripción",
                                selected = kind == "SUBSCRIPTION",
                                modifier = Modifier.weight(1f),
                            ) {
                                kind = "SUBSCRIPTION"
                                categoryId = null
                                subcategoryId = null
                                showAddSubcategory = false
                                newSubcategoryName = ""
                            }
                            SelectorPill(
                                text = "Inversión",
                                selected = kind == "INVESTMENT",
                                modifier = Modifier.weight(1f),
                            ) {
                                kind = "INVESTMENT"
                                categoryId = null
                                subcategoryId = null
                                showAddSubcategory = false
                                newSubcategoryName = ""
                            }
                        }
                    }
                }
            }

            if (isRecurringType) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Nombre", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = movementName,
                        onValueChange = { movementName = it },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        colors = sageFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            MoneyField(
                amountMinor = amountMinor,
                onAmountChange = { amountMinor = it },
                currencyCode = selectedAccount?.currency ?: "",
                label = "Monto",
                plainLabel = true,
                modifier = Modifier.fillMaxWidth(),
            )

            PickerField(
                label = if (isTransfer) "Cuenta origen" else "Cuenta",
                options = accounts,
                selected = selectedAccount,
                optionLabel = { "${it.name} (${it.currency})" },
                trailingLabel = { accountTypeLabel(it.type) },
                onSelect = { accountId = it.id },
                placeholder = "Selecciona cuenta",
                emptyHint = "Crea una cuenta primero (pestaña Cuentas).",
                plainLabel = true,
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
                    plainLabel = true,
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
                    plainLabel = true,
                )
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
                            plainLabel = true,
                            modifier = Modifier.weight(1f),
                        )
                        if (!showAddSubcategory) {
                            OutlinedIconButton(
                                onClick = { showAddSubcategory = true },
                                enabled = selectedCategory != null,
                                shape = MaterialTheme.shapes.medium,
                                colors = IconButtonDefaults.outlinedIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = newSubcategoryName,
                                onValueChange = { newSubcategoryName = it },
                                placeholder = { Text("Nombre") },
                                singleLine = true,
                                isError = subcategoryNameTaken,
                                shape = MaterialTheme.shapes.medium,
                                colors = sageFieldColors(),
                                modifier = Modifier.weight(1f),
                            )
                            Button(
                                onClick = {
                                    val catId = selectedCategory?.id ?: return@Button
                                    subcategoryId = vm.addSubcategory(newSubcategoryName, kind, catId)
                                    showAddSubcategory = false
                                    newSubcategoryName = ""
                                },
                                enabled = selectedCategory != null && newSubcategoryName.isNotBlank() && !subcategoryNameTaken,
                                shape = PillShape,
                            ) { Text("Añadir") }
                        }
                        if (subcategoryNameTaken) {
                            Text(
                                "Ya existe una subcategoría con ese nombre",
                                style = MaterialTheme.typography.bodySmall,
                                color = finance.expense,
                            )
                        }
                    }
                }
            }

            // Cuotas: solo para gastos con tarjeta de crédito. El interés se toma de la tarjeta,
            // no se pide aquí — Card.interestRate ya lo tiene guardado.
            if (!isTransfer && !isRecurringType && kind == "EXPENSE" && selectedAccount?.type == "CREDIT") {
                val installments = cuotasText.toLongOrNull()?.coerceIn(1, MAX_INSTALLMENTS) ?: 1
                OutlinedTextField(
                    value = cuotasText,
                    // Solo dígitos: el teclado numérico no protege de un teclado físico o un pegado.
                    onValueChange = { cuotasText = it.filter(Char::isDigit).take(3) },
                    label = { Text("Cuotas") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (installments > 1 && amountMinor > 0) {
                    val rate = vm.cardFor(selectedAccount.id)?.interestRate ?: 0.0
                    val monthly = InstallmentMath.monthlyPaymentMinor(amountMinor, installments, rate)
                    Text(
                        "Se registrará la cuota 1 (${LocalMoneyFormat.current.format(monthly, selectedAccount.currency)}) " +
                            "este mes. Las próximas ${installments - 1} cuotas se agregan automáticamente cada mes " +
                            "hasta completar el plan, con el interés de tu tarjeta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // No aplica a isLinkedEdit: no se toca nextChargeDate/nextContributionDate desde aquí, eso
            // lo sigue gestionando solo el catch-up existente.
            if (!isLinkedEdit) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Fecha",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    DateField(
                        epochDay = dateEpochDay,
                        onEpochDayChange = { dateEpochDay = it },
                        plain = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (effectiveKind == "INVESTMENT") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("¿Periódica?", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    SelectorPill(text = "Sí", selected = periodic, onClick = { periodic = true })
                    SelectorPill(text = "No", selected = !periodic, onClick = { periodic = false })
                }
            }
            if (effectiveKind == "SUBSCRIPTION" || (effectiveKind == "INVESTMENT" && periodic)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Recurrencia", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectorPill(text = "Día del mes", selected = recurMonthly, modifier = Modifier.weight(1f), onClick = { recurMonthly = true })
                        SelectorPill(text = "Cada X días", selected = !recurMonthly, modifier = Modifier.weight(1f), onClick = { recurMonthly = false })
                    }
                    if (recurMonthly) {
                        OutlinedTextField(
                            value = dayOfMonth,
                            onValueChange = { dayOfMonth = it.filter(Char::isDigit).take(2) },
                            label = { Text("Día del mes (1–31)") },
                            isError = dayOfMonth.isNotEmpty() && dayOfMonth.toIntOrNull()?.let { it in 1..31 } != true,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        OutlinedTextField(
                            value = everyDays,
                            onValueChange = { everyDays = it.filter(Char::isDigit).take(4) },
                            label = { Text("Cada cuántos días") },
                            isError = everyDays.isNotEmpty() && everyDays.toLongOrNull()?.let { it >= 1 } != true,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            error?.let { Text(it, color = finance.expense, style = MaterialTheme.typography.bodySmall) }

            Button(
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
                            movementName.isBlank() -> error = "Escribe un nombre."
                            minor <= 0 -> error = "Monto inválido."
                            !recurValid -> error = "Recurrencia inválida."
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
                            movementName.isBlank() -> error = "Escribe un nombre."
                            account == null -> error = "Crea una cuenta primero (pestaña Cuentas)."
                            minor <= 0 -> error = "Monto inválido."
                            periodic && !recurValid -> error = "Recurrencia inválida."
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
                            minor <= 0 -> error = "Monto inválido."
                            else -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.saveTransfer(existing?.id, account.id, dest.id, minor, "", dateEpochDay)
                                onDismiss()
                            }
                        }
                    } else if (kind == "SUBSCRIPTION") {
                        when {
                            movementName.isBlank() -> error = "Escribe un nombre."
                            minor <= 0 -> error = "Monto inválido."
                            !recurValid -> error = "Recurrencia inválida."
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
                            movementName.isBlank() -> error = "Escribe un nombre."
                            account == null -> error = "Crea una cuenta primero (pestaña Cuentas)."
                            minor <= 0 -> error = "Monto inválido."
                            periodic && !recurValid -> error = "Recurrencia inválida."
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
                            categoryId == null -> error = "Elige una categoría."
                            subcategoryId == null -> error = "Elige una subcategoría."
                            minor <= 0 -> error = "Monto inválido."
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
            ) {
                Text(
                    when {
                        editingSubscription != null -> "Guardar suscripción"
                        editingInvestment != null -> "Guardar inversión"
                        else -> "Guardar movimiento"
                    },
                    fontFamily = displayFontFamily(),
                )
            }

            if (editingSubscription != null) {
                TextButton(
                    onClick = {
                        subVm.deleteSubscription(editingSubscription.id)
                        onDismiss()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = finance.expense),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Eliminar suscripción") }
            } else if (editingInvestment != null) {
                TextButton(
                    onClick = {
                        invVm.deleteInvestment(editingInvestment.id)
                        onDismiss()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = finance.expense),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Eliminar inversión") }
            } else if (existing != null) {
                TextButton(
                    onClick = {
                        val block = vm.deleteBlockReason(existing.id)
                        if (block != null) {
                            blockedDelete = block
                        } else {
                            vm.delete(existing.id)
                            onDismiss()
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = finance.expense),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Eliminar movimiento") }
            }
        }
    }

    blockedDelete?.let { block ->
        AlertDialog(
            onDismissRequest = { blockedDelete = null },
            title = { Text("No se puede eliminar") },
            text = {
                Text(
                    when (block) {
                        is TransactionsViewModel.DeleteBlock.Subscription ->
                            "\"${block.name}\" es una suscripción periódica. Elimínala desde la pantalla de Suscripciones — una vez eliminada, podrás borrar este movimiento aquí."
                        is TransactionsViewModel.DeleteBlock.Investment ->
                            "\"${block.name}\" es una inversión periódica. Gestiónala desde la pantalla de Inversiones."
                    },
                )
            },
            confirmButton = { TextButton(onClick = { blockedDelete = null }) { Text("Entendido") } },
        )
    }
}

private const val MAX_INSTALLMENTS = 360L
