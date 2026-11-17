package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finanzen.domain.SymbolPosition
import com.finanzen.ui.components.ConfirmDialog
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.Overline
import com.finanzen.ui.components.PickerField
import com.finanzen.ui.components.RadioRow
import com.finanzen.ui.components.RowDivider
import com.finanzen.ui.components.ScreenColumn
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.components.TitledSection
import com.finanzen.ui.theme.LocalSageType
import com.finanzen.viewmodel.SettingsViewModel
import org.koin.compose.koinInject

/** Monedas que se muestran directamente; el resto se elige con el buscador. */
private val FREQUENT_CURRENCIES = listOf("COP", "USD", "EUR", "MXN")

/** Moneda única de la app (sin conversión) y posición del símbolo. */
@Composable
fun CurrencyScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = koinInject(),
) {
    val baseCurrency by vm.baseCurrency.collectAsState()
    val symbolPos by vm.symbolPosition.collectAsState()
    var pendingCurrency by remember { mutableStateOf<String?>(null) }
    val byCode = remember(vm.currencies) { vm.currencies.associateBy { it.code } }

    pendingCurrency?.let { code ->
        ConfirmDialog(
            title = "¿Cambiar la moneda a $code?",
            body = "Tus montos no se convierten: conservan el mismo número y pasan a mostrarse en $code.",
            confirmLabel = "Cambiar",
            onConfirm = {
                vm.setBaseCurrency(code)
                pendingCurrency = null
            },
            onDismiss = { pendingCurrency = null },
            // Cambiar moneda no borra nada: botón con el color normal, no el de eliminar.
            destructive = false,
        )
    }
    val choose: (String) -> Unit = { code -> if (code != baseCurrency) pendingCurrency = code }

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader(title = "Moneda", onBack = onBack)
        ScreenColumn {
            TitledSection("Moneda de la app") {
                FinanceCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Overline("Frecuentes")
                        // La moneda actual siempre aparece aunque no sea frecuente.
                        val shown = (FREQUENT_CURRENCIES + baseCurrency).distinct().mapNotNull { byCode[it] }
                        Column(Modifier.selectableGroup()) {
                            shown.forEachIndexed { index, c ->
                                if (index > 0) RowDivider()
                                RadioRow(
                                    title = "${c.code} · ${c.name}",
                                    subtitle = "Símbolo ${c.symbol} · ${decimalsLabel(c.decimals)}",
                                    selected = c.code == baseCurrency,
                                    onSelect = { choose(c.code) },
                                )
                            }
                        }
                        PickerField(
                            label = "Otra moneda",
                            options = vm.currencies.sortedBy { it.code },
                            selected = null,
                            optionLabel = { "${it.code} · ${it.name} (${it.symbol})" },
                            onSelect = { choose(it.code) },
                            placeholder = "Buscar entre todas las monedas",
                            searchable = true,
                            searchPredicate = { c, q -> c.code.contains(q, ignoreCase = true) || c.name.contains(q, ignoreCase = true) },
                        )
                        Text(
                            "Sage usa una sola moneda. Cambiarla re-etiqueta tus montos sin convertirlos.",
                            style = LocalSageType.current.caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            TitledSection("Símbolo") {
                val sym = byCode[baseCurrency]?.symbol ?: "$"
                FinanceCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Dónde va el símbolo en los montos de toda la app.",
                            style = LocalSageType.current.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(Modifier.selectableGroup()) {
                            SymbolOption("Antes", "$sym 1.000", SymbolPosition.PREFIX, symbolPos, vm::setSymbolPosition)
                            RowDivider()
                            SymbolOption("Después", "1.000 $sym", SymbolPosition.SUFFIX, symbolPos, vm::setSymbolPosition)
                            RowDivider()
                            SymbolOption("Sin símbolo", "1.000", SymbolPosition.NONE, symbolPos, vm::setSymbolPosition)
                        }
                    }
                }
            }
        }
    }
}

private fun decimalsLabel(decimals: Long): String = when (decimals) {
    0L -> "sin decimales"
    1L -> "1 decimal"
    else -> "$decimals decimales"
}

@Composable
private fun SymbolOption(
    label: String,
    example: String,
    value: SymbolPosition,
    selected: SymbolPosition,
    onSelect: (SymbolPosition) -> Unit,
) {
    RadioRow(title = label, selected = selected == value, onSelect = { onSelect(value) }, trailing = example)
}
