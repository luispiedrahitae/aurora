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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.db.Category
import com.finanzen.ui.components.CategoryAvatar
import com.finanzen.ui.components.CategoryGlyph
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.SectionHeader
import com.finanzen.ui.components.categoryColor
import com.finanzen.ui.components.categoryColors
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
    var color by remember { mutableStateOf(categoryColors.first()) }

    val parents = categories.filter { it.parentId == null }

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
                            CategoryAvatar(icon, color, size = 44.dp)
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Nombre") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        IconPicker(selected = icon, onSelect = { icon = it })
                        ColorPicker(selected = color, onSelect = { color = it })
                        Button(
                            onClick = {
                                vm.add(name, kind, parentId = null, icon = icon, color = color.toArgb().toLong())
                                name = ""
                                icon = iconGroups.first().icons.first().first
                                color = categoryColors.first()
                            },
                            enabled = name.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.Add, null)
                            Text("  Añadir")
                        }
                    }
                }
            }

            categoryGroup(parents.filter { it.kind == "EXPENSE" }, "Gastos", categories, vm::delete)
            categoryGroup(parents.filter { it.kind == "INCOME" }, "Ingresos", categories, vm::delete)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.categoryGroup(
    parents: List<Category>,
    title: String,
    all: List<Category>,
    onDelete: (Long) -> Unit,
) {
    if (parents.isEmpty()) return
    item { SectionHeader(title) }
    items(parents, key = { it.id }) { parent ->
        val children = all.filter { it.parentId == parent.id }
        CategoryCard(parent, children, onDelete)
    }
}

@Composable
private fun CategoryCard(parent: Category, children: List<Category>, onDelete: (Long) -> Unit) {
    FinanceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CategoryRow(parent, onDelete)
            children.forEach { child ->
                Row(modifier = Modifier.padding(start = 16.dp)) { CategoryRow(child, onDelete) }
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
        CategoryAvatar(category.icon, categoryColor(category.name, category.color), size = 28.dp)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPicker(selected: Color, onSelect: (Color) -> Unit) {
    Text("Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categoryColors.forEach { swatch ->
            val isSelected = swatch == selected
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(swatch)
                    .then(
                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier,
                    )
                    .clickable { onSelect(swatch) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(Icons.Outlined.Check, contentDescription = "Seleccionado", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
