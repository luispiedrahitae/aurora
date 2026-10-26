package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.data.RESERVED_CATEGORY_NAMES
import com.finanzen.db.Category
import com.finanzen.ui.components.CategoryAvatar
import com.finanzen.ui.components.ConfirmDialog
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.SectionHeader
import com.finanzen.ui.components.SelectorPill
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.components.SwipeToDeleteRow
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.ui.theme.PillShape
import com.finanzen.ui.theme.displayFontFamily
import com.finanzen.viewmodel.CategoriesViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    vm: CategoriesViewModel = koinViewModel(),
) {
    val categories by vm.categories.collectAsState()
    var name by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("EXPENSE") }

    val parents = categories.filter { it.parentId == null }
    val existingNames = categories.filter { it.kind == kind }.map { it.name.trim().lowercase() }.toSet()
    val nameTaken = name.isNotBlank() && name.trim().lowercase() in existingNames
    val isReserved = name.isNotBlank() && RESERVED_CATEGORY_NAMES.any { it.equals(name.trim(), ignoreCase = true) }
    var pendingDelete by remember { mutableStateOf<Category?>(null) }

    pendingDelete?.let { cat ->
        ConfirmDialog(
            title = "¿Eliminar \"${cat.name}\"?",
            body = "Se elimina la categoría" +
                (if (cat.parentId == null) " junto con sus subcategorías" else "") +
                " y todo lo que la use: movimientos, presupuestos, suscripciones, " +
                "inversiones y planes de cuotas. Esta acción no se puede deshacer.",
            onConfirm = {
                vm.delete(cat.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader(title = "Categorías", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Nueva categoría", fontWeight = FontWeight.SemiBold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectorPill(
                                text = "Gasto",
                                selected = kind == "EXPENSE",
                                modifier = Modifier.weight(1f),
                                onClick = { kind = "EXPENSE" },
                            )
                            SelectorPill(
                                text = "Ingreso",
                                selected = kind == "INCOME",
                                modifier = Modifier.weight(1f),
                                onClick = { kind = "INCOME" },
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CategoryAvatar(name, size = 44.dp)
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Nombre") },
                                singleLine = true,
                                isError = nameTaken || isReserved,
                                supportingText = when {
                                    isReserved -> {
                                        { Text("Nombre de categoría reservado") }
                                    }
                                    nameTaken -> {
                                        { Text("Ya existe una categoría con ese nombre") }
                                    }
                                    else -> null
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Button(
                            onClick = {
                                vm.add(name, kind, parentId = null, icon = "")
                                name = ""
                            },
                            enabled = name.isNotBlank() && !nameTaken && !isReserved,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs),
                            ) {
                                Icon(LucideIcons.Plus, null)
                                // Caprasimo: mismo tratamiento que el resto de los CTA primarios
                                // (AddFab, "Guardar cambios"...) — ver Type.kt.
                                Text("Añadir", fontFamily = displayFontFamily())
                            }
                        }
                    }
                }
            }

            val addSubcategory: (Long, String) -> Unit = { parentId, subName ->
                categories.firstOrNull { it.id == parentId }?.let { parent ->
                    vm.add(subName, parent.kind, parentId = parent.id, icon = "")
                }
            }
            val requestDelete: (Long) -> Unit = { id -> pendingDelete = categories.firstOrNull { it.id == id } }
            categoryGroup(parents.filter { it.kind == "EXPENSE" && it.name !in RESERVED_CATEGORY_NAMES }, "Gastos", categories, requestDelete, addSubcategory)
            categoryGroup(parents.filter { it.kind == "INCOME" && it.name !in RESERVED_CATEGORY_NAMES }, "Ingresos", categories, requestDelete, addSubcategory)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.categoryGroup(
    parents: List<Category>,
    title: String,
    all: List<Category>,
    onDelete: (Long) -> Unit,
    onAddSubcategory: (parentId: Long, name: String) -> Unit,
) {
    if (parents.isEmpty()) {
        // Antes el grupo desaparecía entero (encabezado incluido) y no quedaba claro dónde crear uno.
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(title)
                EmptyState(
                    icon = LucideIcons.Tag,
                    title = "Sin categorías de ${title.lowercase()}",
                    subtitle = "Créala arriba, en \"Nueva categoría\", para poder clasificar tus movimientos.",
                    modifier = Modifier.fillMaxWidth().padding(vertical = LocalSpacing.current.lg),
                )
            }
        }
        return
    }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(title)
            val first = parents.first()
            val firstChildren = all.filter { it.parentId == first.id }.sortedBy { it.name.lowercase() }
            // El swipe no borra directo (a diferencia de Suscripciones/Inversiones): eliminar una
            // categoría cascada a movimientos/presupuestos/etc. y es irreversible, así que solo abre
            // el ConfirmDialog existente (onDelete) y la fila vuelve a su sitio (`false`).
            SwipeToDeleteRow(onSwipeToDelete = {
                onDelete(first.id)
                false
            }) {
                CategoryCard(first, firstChildren, onDelete, onAddSubcategory)
            }
        }
    }
    items(parents.drop(1), key = { it.id }) { parent ->
        val children = all.filter { it.parentId == parent.id }.sortedBy { it.name.lowercase() }
        SwipeToDeleteRow(onSwipeToDelete = {
            onDelete(parent.id)
            false
        }) {
            CategoryCard(parent, children, onDelete, onAddSubcategory)
        }
    }
}

// Coincide con Sage.dc.html L627-649: nombre + toggle "+" (revela el campo inline, sin ocultar
// las subcategorías existentes) + subcategorías siempre visibles como chips en una fila que envuelve.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryCard(
    parent: Category,
    children: List<Category>,
    onDelete: (Long) -> Unit,
    onAddSubcategory: (parentId: Long, name: String) -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    FinanceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.sm),
            ) {
                CategoryAvatar(parent.name, size = 28.dp)
                Text(parent.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { adding = !adding }) {
                    Icon(LucideIcons.Plus, contentDescription = "Añadir subcategoría")
                }
            }
            if (children.isNotEmpty()) {
                // Sin espacio vertical: cada chip ya ocupa 48dp de alto (área táctil) alrededor de una
                // píldora más baja, lo que deja el hueco visible entre filas.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.sm)) {
                    children.forEach { child -> SubcategoryTag(child, onDelete) }
                }
            }
            if (adding) {
                var newSubName by remember { mutableStateOf("") }
                val existingNames = children.map { it.name.trim().lowercase() }.toSet()
                val nameTaken = newSubName.isNotBlank() && newSubName.trim().lowercase() in existingNames
                val isReserved = newSubName.isNotBlank() && RESERVED_CATEGORY_NAMES.any { it.equals(newSubName.trim(), ignoreCase = true) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = newSubName,
                        onValueChange = { newSubName = it },
                        label = { Text("Subcategoría") },
                        singleLine = true,
                        isError = nameTaken || isReserved,
                        supportingText = when {
                            isReserved -> {
                                { Text("Nombre de categoría reservado") }
                            }
                            nameTaken -> {
                                { Text("Ya existe") }
                            }
                            else -> null
                        },
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            onAddSubcategory(parent.id, newSubName)
                            newSubName = ""
                            adding = false
                        },
                        enabled = newSubName.isNotBlank() && !nameTaken && !isReserved,
                    ) {
                        Icon(LucideIcons.Plus, contentDescription = "Añadir subcategoría")
                    }
                }
            }
        }
    }
}

@Composable
private fun SubcategoryTag(category: Category, onDelete: (Long) -> Unit) {
    // Todo el chip es el objetivo táctil (Surface con onClick aplica el mínimo de 48dp por fuera de la
    // píldora visible); antes era una "X" de 20dp. Borrar abre el ConfirmDialog, así que un toque
    // accidental no destruye nada.
    Surface(
        onClick = { onDelete(category.id) },
        shape = PillShape,
        // Highest, no High: en la paleta Sage surfaceContainerHigh == color de la tarjeta, y la
        // píldora quedaba invisible (solo se veía el texto + X flotando).
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier.padding(
                start = LocalSpacing.current.md,
                end = LocalSpacing.current.sm,
                top = LocalSpacing.current.xs,
                bottom = LocalSpacing.current.xs,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs),
        ) {
            Text(category.name, style = MaterialTheme.typography.labelMedium)
            Icon(LucideIcons.X, contentDescription = "Eliminar", modifier = Modifier.size(16.dp))
        }
    }
}
