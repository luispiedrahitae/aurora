package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.db.Category
import com.finanzen.viewmodel.CategoriesViewModel
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Nueva categoría", fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = kind == "EXPENSE", onClick = { kind = "EXPENSE" }, label = { Text("Gasto") })
                            FilterChip(selected = kind == "INCOME", onClick = { kind = "INCOME" }, label = { Text("Ingreso") })
                        }
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nombre") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = {
                                vm.add(name, kind, parentId = null)
                                name = ""
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
    item {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
    items(parents, key = { it.id }) { parent ->
        val children = all.filter { it.parentId == parent.id }
        CategoryCard(parent, children, onDelete)
    }
}

@Composable
private fun CategoryCard(parent: Category, children: List<Category>, onDelete: (Long) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CategoryRow(parent, onDelete)
            children.forEach { child ->
                Row(modifier = Modifier.padding(start = 16.dp)) { CategoryRow(child, onDelete) }
            }
        }
    }
}

@Composable
private fun CategoryRow(category: Category, onDelete: (Long) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            (category.icon.takeIf { it.isNotBlank() }?.let { "$it " } ?: "") + category.name,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onDelete(category.id) }) {
            Icon(Icons.Outlined.Delete, contentDescription = "Eliminar")
        }
    }
}
