package com.finanzen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.data.RESERVED_CATEGORY_NAMES
import com.finanzen.db.Category
import com.finanzen.ui.components.CategoryAvatar
import com.finanzen.ui.components.CategoryGlyph
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.SectionHeader
import com.finanzen.ui.components.iconGroups
import com.finanzen.viewmodel.CategoriesViewModel
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    vm: CategoriesViewModel = koinViewModel(),
) {
    val categories by vm.categories.collectAsState()
    var name by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("EXPENSE") }
    var icon by remember { mutableStateOf(iconGroups.first().icons.first().first) }

    val parents = categories.filter { it.parentId == null }
    val existingNames = categories.filter { it.kind == kind }.map { it.name.trim().lowercase() }.toSet()
    val nameTaken = name.isNotBlank() && name.trim().lowercase() in existingNames
    val isReserved = name.isNotBlank() && RESERVED_CATEGORY_NAMES.any { it.equals(name.trim(), ignoreCase = true) }
    var pendingDelete by remember { mutableStateOf<Category?>(null) }

    pendingDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("¿Eliminar \"${cat.name}\"?") },
            text = {
                Text(
                    "Se elimina la categoría" +
                        (if (cat.parentId == null) " junto con sus subcategorías" else "") +
                        " y todo lo que la use: movimientos, presupuestos, suscripciones, " +
                        "inversiones y planes de cuotas. Esta acción no se puede deshacer.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(cat.id)
                    pendingDelete = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorías") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
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
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Nueva categoría", fontWeight = FontWeight.SemiBold)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = kind == "EXPENSE",
                                onClick = { kind = "EXPENSE" },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                icon = {},
                            ) { Text("Gasto") }
                            SegmentedButton(
                                selected = kind == "INCOME",
                                onClick = { kind = "INCOME" },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                icon = {},
                            ) { Text("Ingreso") }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CategoryAvatar(icon, size = 44.dp)
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
                        IconPicker(selected = icon, onSelect = { icon = it })
                        Button(
                            onClick = {
                                vm.add(name, kind, parentId = null, icon = icon)
                                name = ""
                                icon = iconGroups.first().icons.first().first
                            },
                            enabled = name.isNotBlank() && !nameTaken && !isReserved,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.Add, null)
                            Text("  Añadir")
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
    if (parents.isEmpty()) return
    item { SectionHeader(title) }
    items(parents, key = { it.id }) { parent ->
        val children = all.filter { it.parentId == parent.id }.sortedBy { it.name.lowercase() }
        CategoryCard(parent, children, onDelete, onAddSubcategory)
    }
}

@Composable
private fun CategoryCard(
    parent: Category,
    children: List<Category>,
    onDelete: (Long) -> Unit,
    onAddSubcategory: (parentId: Long, name: String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    FinanceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CategoryRow(parent, onDelete)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(start = 16.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Contraer" else "Expandir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Subcategorías (${children.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (expanded) {
                children.forEach { child ->
                    Row(modifier = Modifier.padding(start = 16.dp)) { CategoryRow(child, onDelete) }
                }
                var newSubName by remember { mutableStateOf("") }
                val existingNames = children.map { it.name.trim().lowercase() }.toSet()
                val nameTaken = newSubName.isNotBlank() && newSubName.trim().lowercase() in existingNames
                val isReserved = newSubName.isNotBlank() && RESERVED_CATEGORY_NAMES.any { it.equals(newSubName.trim(), ignoreCase = true) }
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp),
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
                        },
                        enabled = newSubName.isNotBlank() && !nameTaken && !isReserved,
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Añadir subcategoría")
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(category: Category, onDelete: (Long) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (category.parentId == null) {
            CategoryAvatar(category.icon, size = 28.dp)
        }
        Text(category.name, modifier = Modifier.weight(1f))
        IconButton(onClick = { onDelete(category.id) }) {
            Icon(Icons.Outlined.Delete, contentDescription = "Eliminar")
        }
    }
}

@Composable
private fun IconPicker(selected: String, onSelect: (String) -> Unit) {
    iconGroups.forEach { group ->
        IconSection(group.title, group.icons, selected, onSelect)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconSection(
    title: String,
    icons: List<Pair<String, CategoryGlyph>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        icons.forEach { (key, glyph) ->
            val isSelected = key == selected
            val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .then(
                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier,
                    )
                    .clickable { onSelect(key) },
                contentAlignment = Alignment.Center,
            ) {
                val glyphModifier = Modifier.size(22.dp)
                when (glyph) {
                    is CategoryGlyph.Vec -> Icon(glyph.image, contentDescription = key, tint = tint, modifier = glyphModifier)
                    is CategoryGlyph.Res -> Icon(painterResource(glyph.drawable), contentDescription = key, tint = tint, modifier = glyphModifier)
                }
            }
        }
    }
}
