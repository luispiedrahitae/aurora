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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.SectionHeader
import com.finanzen.viewmodel.SettingsViewModel
import org.koin.compose.koinInject

/** Apartado de Moneda (antes dentro de Apariencia). Moneda única de la app; no hay FX. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = koinInject(),
) {
    val baseCurrency by vm.baseCurrency.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Moneda") },
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
            item { SectionHeader("Moneda de la app") }
            item {
                FinanceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                    Column {
                        Text(
                            "Moneda única de la app. Cambiarla re-etiqueta tus montos existentes sin convertirlos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                        )
                        vm.currencies.forEach { currency ->
                            CurrencyOption(currency.code, currency.symbol, baseCurrency, vm::setBaseCurrency)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyOption(
    code: String,
    symbol: String,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected == code, onClick = { onSelect(code) })
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected == code, onClick = { onSelect(code) })
        Text("$code ($symbol)", fontWeight = FontWeight.SemiBold)
    }
}
