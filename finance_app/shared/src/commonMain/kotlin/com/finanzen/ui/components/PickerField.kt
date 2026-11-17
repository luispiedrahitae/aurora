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
    val spacing = LocalSpacing.current
    var open by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

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
        var query by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { open = false }, sheetState = sheetState) {
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
                if (searchable) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.sm),
                        placeholder = { Text("Buscar…") },
                        leadingIcon = { Icon(LucideIcons.Search, contentDescription = null) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        colors = sageFieldColors(),
                    )
                }
                val filtered = if (searchable && query.isNotBlank() && searchPredicate != null) {
                    options.filter { searchPredicate(it, query) }
                } else {
                    options
                }
                if (filtered.isEmpty()) {
                    Text(
                        emptyHint,
                        style = MaterialTheme.typography.bodyMedium,
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
                                val isSelected = option == selected
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        // selectable: TalkBack anuncia cuál opción está elegida, no solo el color.
                                        .selectable(selected = isSelected, role = Role.RadioButton) {
                                            onSelect(option)
                                            open = false
                                        }
                                        .padding(horizontal = spacing.lg, vertical = spacing.md),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                                ) {
                                    leadingContent?.invoke(option)
                                    Text(
                                        optionLabel(option),
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                    trailingLabel?.invoke(option)?.let { label ->
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
