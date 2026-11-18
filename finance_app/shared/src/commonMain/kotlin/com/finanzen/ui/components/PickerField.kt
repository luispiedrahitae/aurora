package com.finanzen.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalSageType
import com.finanzen.ui.theme.LocalSpacing

/**
 * Campo de selección que abre un [ModalBottomSheet] con la lista de opciones (mejor que un dropdown
 * para listas medianas/largas en móvil). El campo es de solo lectura; un overlay transparente captura
 * el toque para abrir el sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> PickerField(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Selecciona…",
    emptyHint: String = "No hay opciones disponibles.",
    leadingContent: (@Composable (T) -> Unit)? = null,
    trailingLabel: ((T) -> String?)? = null,
    searchable: Boolean = false,
    searchPredicate: ((T, String) -> Boolean)? = null,
    sectionOf: ((T) -> String?)? = null,
    // false: campo de solo-lectura visual (sin abrir el sheet) — p. ej. el tipo de cuenta bloqueado
    // al editar en AccountFormDialog, que se muestra como PickerField pero no es modificable.
    enabled: Boolean = true,
) {
    var open by remember { mutableStateOf(false) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FieldLabel(label)
        Box(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selected?.let(optionLabel) ?: "",
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                placeholder = { Text(placeholder) },
                trailingIcon = { Icon(LucideIcons.ChevronDown, contentDescription = null) },
                textStyle = LocalSageType.current.body,
                shape = MaterialTheme.shapes.small,
                colors = sageFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            // Overlay transparente: el OutlinedTextField readOnly no expone onClick, así que capturamos
            // el toque encima de él. clearAndSetSemantics: sin él, TalkBack anuncia el TextField como
            // "campo de solo lectura" y el overlay no existe como control — este picker era invisible
            // para lectores de pantalla en todo el flujo de captura. Si enabled=false (p. ej. tipo de
            // cuenta bloqueado), no se agrega ni el clickable ni la semántica de botón.
            if (enabled) {
                Box(
                    Modifier
                        .matchParentSize()
                        .clip(MaterialTheme.shapes.small)
                        .clickable(onClickLabel = "Abrir opciones") { open = true }
                        .clearAndSetSemantics {
                            role = Role.Button
                            contentDescription = "$label: ${selected?.let(optionLabel) ?: placeholder}"
                        },
                )
            }
        }
    }

    if (open) {
        PickerSheet(
            label = label,
            options = options,
            selected = selected,
            optionLabel = optionLabel,
            onSelect = onSelect,
            onDismiss = { open = false },
            emptyHint = emptyHint,
            leadingContent = leadingContent,
            trailingLabel = trailingLabel,
            searchable = searchable,
            searchPredicate = searchPredicate,
            sectionOf = sectionOf,
        )
    }
}

/** Hoja de opciones de [PickerField]: título, buscador opcional y la lista agrupada por sección. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> PickerSheet(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    emptyHint: String,
    leadingContent: (@Composable (T) -> Unit)?,
    trailingLabel: ((T) -> String?)?,
    searchable: Boolean,
    searchPredicate: ((T, String) -> Boolean)?,
    sectionOf: ((T) -> String?)?,
) {
    val spacing = LocalSpacing.current
    val sheetState = rememberModalBottomSheetState()
    var query by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = spacing.xl),
        ) {
            Text(
                label,
                style = LocalSageType.current.titleSection,
                modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
            )
            if (searchable) PickerSearchField(query) { query = it }
            val filtered = if (searchable && query.isNotBlank() && searchPredicate != null) {
                options.filter { searchPredicate(it, query) }
            } else {
                options
            }
            if (filtered.isEmpty()) {
                Text(
                    emptyHint,
                    style = LocalSageType.current.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.md),
                )
            } else {
                // Lista dentro de un recuadro redondeado con separadores, como una tarjeta de opciones.
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        itemsIndexed(filtered) { index, option ->
                            sectionOf?.invoke(option)?.let { section ->
                                val previousSection = filtered.getOrNull(index - 1)?.let(sectionOf)
                                if (section != previousSection) {
                                    Text(
                                        section,
                                        style = LocalSageType.current.overline,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
                                    )
                                }
                            }
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = spacing.lg),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                            PickerOptionRow(
                                text = optionLabel(option),
                                selected = option == selected,
                                onClick = {
                                    onSelect(option)
                                    onDismiss()
                                },
                                leading = leadingContent?.let { lc -> { lc(option) } },
                                trailing = trailingLabel?.invoke(option),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Fila de una opción: selección anunciada a TalkBack, no solo por color. */
@Composable
private fun PickerOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)?,
    trailing: String?,
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = spacing.lg, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        leading?.invoke()
        Text(
            text,
            modifier = Modifier.weight(1f),
            style = LocalSageType.current.body,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        )
        if (trailing != null) {
            Text(trailing, style = LocalSageType.current.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Buscador de la hoja de opciones. */
@Composable
private fun PickerSearchField(query: String, onChange: (String) -> Unit) {
    val spacing = LocalSpacing.current
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.sm),
        placeholder = { Text("Buscar…") },
        leadingIcon = { Icon(LucideIcons.Search, contentDescription = null) },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        colors = sageFieldColors(),
    )
}
