package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.domain.SymbolPosition
import com.finanzen.ui.components.ConfirmDialog
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.PickerField
import com.finanzen.ui.components.SectionHeader
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.viewmodel.SettingsViewModel
import org.koin.compose.koinInject

/** Apartado de Moneda (antes dentro de Apariencia). Moneda única de la app; no hay FX. */
@Composable
fun CurrencyScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = koinInject(),
) {
    val baseCurrency by vm.baseCurrency.collectAsState()
    val symbolPos by vm.symbolPosition.collectAsState()
    var pendingCurrency by remember { mutableStateOf<String?>(null) }

    pendingCurrency?.let { code ->
        ConfirmDialog(
            title = "¿Cambiar la moneda a $code?",
            body = "Tus montos existentes no se convierten: conservan el mismo número y pasan a mostrarse en $code.",
            confirmLabel = "Cambiar",
            onConfirm = {
                vm.setBaseCurrency(code)
                pendingCurrency = null
            },
            onDismiss = { pendingCurrency = null },
        )
    }

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader(title = "Moneda", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader("Moneda de la app")
                    FinanceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                        Column {
                            Text(
                                "Moneda única de la app. Cambiarla re-etiqueta tus montos existentes sin convertirlos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                            )
                            val frecuentes = setOf("USD", "EUR", "COP", "MXN")
                            val orderedCurrencies = vm.currencies.sortedWith(
                                compareBy({ it.code !in frecuentes }, { it.code }),
                            )
                            PickerField(
                                label = "Moneda de la app",
                                options = orderedCurrencies,
                                selected = vm.currencies.firstOrNull { it.code == baseCurrency },
                                optionLabel = { "${it.code} — ${it.name} (${it.symbol})" },
                                onSelect = { if (it.code != baseCurrency) pendingCurrency = it.code },
                                modifier = Modifier.padding(16.dp),
                                searchable = true,
                                searchPredicate = { c, q -> c.code.contains(q, ignoreCase = true) || c.name.contains(q, ignoreCase = true) },
                                sectionOf = { if (it.code in frecuentes) "Frecuentes" else "Todas" },
                            )
                        }
                    }
                }
            }

            item {
                val sym = vm.currencies.firstOrNull { it.code == baseCurrency }?.symbol ?: "$"
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader("Símbolo")
                    FinanceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                        Column {
                            Text(
                                "Dónde va el símbolo de moneda en los montos de toda la app.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                            )
                            SymbolOption("Antes  (${sym}1.000)", SymbolPosition.PREFIX, symbolPos, vm::setSymbolPosition)
                            SymbolOption("Después  (1.000 $sym)", SymbolPosition.SUFFIX, symbolPos, vm::setSymbolPosition)
                            SymbolOption("Sin símbolo  (1.000)", SymbolPosition.NONE, symbolPos, vm::setSymbolPosition)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SymbolOption(
    label: String,
    value: SymbolPosition,
    selected: SymbolPosition,
    onSelect: (SymbolPosition) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // La fila es el único objetivo táctil/semántico; el RadioButton es solo visual
            // (onClick = null) para que TalkBack no enfoque dos veces la misma opción.
            .selectable(selected = selected == value, role = Role.RadioButton, onClick = { onSelect(value) })
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected == value, onClick = null)
        Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
}
