package com.finanzen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finanzen.data.RESERVED_CATEGORY_NAMES
import com.finanzen.db.Category
import com.finanzen.ui.components.ConfirmDialog
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.FieldError
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.GroupCard
import com.finanzen.ui.components.RowDivider
import com.finanzen.ui.components.SageBadge
import com.finanzen.ui.components.SageButton
import com.finanzen.ui.components.SageButtonKind
import com.finanzen.ui.components.SageChip
import com.finanzen.ui.components.SageTextField
import com.finanzen.ui.components.ScreenList
import com.finanzen.ui.components.SegmentedControl
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.components.TitledSection
import com.finanzen.ui.components.categoryColor
import com.finanzen.ui.components.rowClickable
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalCategoryColors
import com.finanzen.ui.theme.LocalSageType
import com.finanzen.viewmodel.CategoriesViewModel
import org.koin.compose.viewmodel.koinViewModel

private const val EXPENSE = "EXPENSE"
private const val INCOME = "INCOME"

/** Qué diálogo de edición está abierto. */
private sealed interface CategoryEdit {
    data class Rename(val category: Category) : CategoryEdit
    data class AddSub(val parent: Category) : CategoryEdit
}

@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    vm: CategoriesViewModel = koinViewModel(),
) {
    val categories by vm.categories.collectAsState()
    var kind by remember { mutableStateOf(EXPENSE) }
    var name by remember { mutableStateOf("") }
    var expandedId by remember { mutableStateOf<Long?>(null) }
    var pendingDelete by remember { mutableStateOf<Category?>(null) }
    var edit by remember { mutableStateOf<CategoryEdit?>(null) }

    val parents = categories.filter { it.parentId == null }
    val userParents = parents.filter { it.kind == kind && it.name !in RESERVED_CATEGORY_NAMES }
    val systemParents = parents.filter { it.kind == kind && it.name in RESERVED_CATEGORY_NAMES }
    val countFor: (String) -> Int = { k -> parents.count { it.kind == k && it.name !in RESERVED_CATEGORY_NAMES } }
    val nameError = nameError(name, categories.filter { it.kind == kind && it.parentId == null })

    CategoryDialogs(
        categories = categories,
        pendingDelete = pendingDelete,
        edit = edit,
        vm = vm,
        onDeleteDone = { pendingDelete = null },
        onEditDone = { edit = null },
    )

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader(title = "Categorías", onBack = onBack)
        ScreenList {
            item {
                SegmentedControl(
                    options = listOf(EXPENSE, INCOME),
                    selected = kind,
                    onSelect = {
                        kind = it
                        expandedId = null
                    },
                    label = { if (it == EXPENSE) "Gastos · ${countFor(EXPENSE)}" else "Ingresos · ${countFor(INCOME)}" },
                )
            }
            item {
                FinanceCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (kind == EXPENSE) "Nueva categoría de gasto" else "Nueva categoría de ingreso", style = LocalSageType.current.titleCard)
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SageTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = "Nombre",
                                placeholder = if (kind == EXPENSE) "Ej.: Regalos" else "Ej.: Arriendo recibido",
                                modifier = Modifier.weight(1f),
                            )
                            SageButton(
                                "Añadir",
                                onClick = {
                                    vm.add(name, kind, parentId = null)
                                    name = ""
                                },
                                enabled = name.isNotBlank() && nameError == null,
                            )
                        }
                        nameError?.let { FieldError(it) }
                    }
                }
            }
            item {
                TitledSection("Tus categorías") {
                    if (userParents.isEmpty()) {
                        EmptyState(
                            icon = LucideIcons.Tag,
                            title = if (kind == EXPENSE) "Sin categorías de gasto" else "Sin categorías de ingreso",
                            subtitle = "Créala arriba para poder clasificar tus movimientos.",
                        )
                    } else {
                        GroupCard {
                            userParents.forEachIndexed { index, parent ->
                                if (index > 0) RowDivider()
                                val children = categories.filter { it.parentId == parent.id }.sortedBy { it.name.lowercase() }
                                CategoryRow(
                                    parent = parent,
                                    children = children,
                                    expanded = expandedId == parent.id,
                                    onToggle = { expandedId = if (expandedId == parent.id) null else parent.id },
                                    onAddSub = { edit = CategoryEdit.AddSub(parent) },
                                    onRename = { edit = CategoryEdit.Rename(parent) },
                                    onDelete = { pendingDelete = it },
                                )
                            }
                        }
                    }
                }
            }
            if (systemParents.isNotEmpty()) {
                item {
                    TitledSection("Del sistema") {
                        GroupCard {
                            systemParents.forEachIndexed { index, cat ->
                                if (index > 0) RowDivider()
                                Row(
                                    Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Text(cat.name, style = LocalSageType.current.titleCard, modifier = Modifier.weight(1f))
                                    SageBadge("Fija")
                                }
                            }
                        }
                        Text(
                            "Sage las usa para transferencias, ajustes, cobros automáticos y aportes a fondos. No se pueden renombrar ni eliminar.",
                            style = LocalSageType.current.caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Fila de categoría plegable: punto de color, nombre, resumen de subcategorías y chevron. Abierta
 * muestra las subcategorías (tocar una la elimina, previa confirmación) y, en una sola fila, las
 * acciones "+ Subcategoría", "Renombrar" y "Eliminar".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryRow(
    parent: Category,
    children: List<Category>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAddSub: () -> Unit,
    onRename: () -> Unit,
    onDelete: (Category) -> Unit,
) {
    val color = categoryColor(parent.name, parent.color, LocalCategoryColors.current)
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .rowClickable(onToggle, label = if (expanded) "Cerrar" else "Abrir")
                .semantics { stateDescription = if (expanded) "Abierta" else "Cerrada" }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(color))
            Text(parent.name, style = LocalSageType.current.titleCard, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subcategorySummary(children), style = LocalSageType.current.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Icon(
                if (expanded) LucideIcons.ChevronUp else LucideIcons.ChevronDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        if (expanded) {
            RowDivider()
            // 16 dp de aire entre la línea y las subcategorías, y abajo antes de la siguiente categoría.
            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (children.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        children.forEach { child ->
                            SageChip(child.name, onClick = { onDelete(child) }, icon = LucideIcons.X)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SageButton("Subcategoría", onAddSub, kind = SageButtonKind.Secondary, icon = LucideIcons.Plus, small = true)
                    SageButton("Renombrar", onRename, kind = SageButtonKind.Secondary, small = true)
                    SageButton("Eliminar", { onDelete(parent) }, kind = SageButtonKind.Danger, small = true)
                }
            }
        }
    }
}

private fun subcategorySummary(children: List<Category>): String = when {
    children.isEmpty() -> "Sin subcategorías"
    children.size <= 2 -> children.joinToString(", ") { it.name }
    else -> "${children.size} subcategorías"
}

/** Error de nombre (vacío no es error: el botón queda apagado). */
private fun nameError(name: String, siblings: List<Category>): String? {
    val trimmed = name.trim()
    return when {
        trimmed.isEmpty() -> null
        RESERVED_CATEGORY_NAMES.any { it.equals(trimmed, ignoreCase = true) } -> "Ese nombre lo usa Sage para una categoría del sistema."
        siblings.any { it.name.trim().equals(trimmed, ignoreCase = true) } -> "Ya existe una categoría con ese nombre."
        else -> null
    }
}

/** Diálogo con un solo campo de nombre, validado contra sus hermanas. */
@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    siblings: List<Category>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    val error = nameError(value, siblings)
    val canSave = value.isNotBlank() && error == null && value.trim() != initial.trim()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = LocalSageType.current.titleSection) },
        text = {
            SageTextField(
                value = value,
                onValueChange = { value = it },
                label = "Nombre",
                error = error,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        },
        confirmButton = { SageButton(confirmLabel, { onConfirm(value) }, enabled = canSave) },
        dismissButton = { SageButton("Cancelar", onDismiss, kind = SageButtonKind.Ghost) },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
    )
}

/** Confirmación de borrado y diálogos de nombre (renombrar, nueva subcategoría). */
@Composable
private fun CategoryDialogs(
    categories: List<Category>,
    pendingDelete: Category?,
    edit: CategoryEdit?,
    vm: CategoriesViewModel,
    onDeleteDone: () -> Unit,
    onEditDone: () -> Unit,
) {
    pendingDelete?.let { cat ->
        ConfirmDialog(
            title = "¿Eliminar \"${cat.name}\"?",
            body = "Se elimina la categoría" +
                (if (cat.parentId == null) " junto con sus subcategorías" else "") +
                " y todo lo que la use: movimientos, presupuestos, suscripciones, " +
                "inversiones y planes de cuotas. Esta acción no se puede deshacer.",
            onConfirm = {
                vm.delete(cat.id)
                onDeleteDone()
            },
            onDismiss = { onDeleteDone() },
        )
    }
    when (val e = edit) {
        is CategoryEdit.Rename -> NameDialog(
            title = "Renombrar categoría",
            initial = e.category.name,
            confirmLabel = "Guardar",
            siblings = categories.filter { it.kind == e.category.kind && it.parentId == e.category.parentId && it.id != e.category.id },
            onConfirm = {
                vm.rename(e.category, it)
                onEditDone()
            },
            onDismiss = { onEditDone() },
        )

        is CategoryEdit.AddSub -> NameDialog(
            title = "Nueva subcategoría en ${e.parent.name}",
            initial = "",
            confirmLabel = "Añadir",
            siblings = categories.filter { it.parentId == e.parent.id },
            onConfirm = {
                vm.add(it, e.parent.kind, parentId = e.parent.id)
                onEditDone()
            },
            onDismiss = { onEditDone() },
        )

        null -> Unit
    }
}
