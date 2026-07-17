package com.finanzen.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finanzen.data.CurrencyLocaleInfo
import com.finanzen.ui.format.formatFechaCorta
import com.finanzen.ui.theme.LocalDateLocale

private const val MILLIS_PER_DAY = 86_400_000L
private val REASONABLE_YEAR_RANGE = 1990..2100

/**
 * Botón con la fecha elegida que abre un [DatePickerDialog] de Material3 al tocarlo. Mismo patrón
 * que usa `TransactionFormScreen`, extraído para reutilizar en Suscripciones e Inversiones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    epochDay: Long,
    onEpochDayChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    dateLocale: CurrencyLocaleInfo = LocalDateLocale.current,
) {
    var showPicker by remember { mutableStateOf(false) }

    if (showPicker) {
        val dpState = rememberDatePickerState(
            initialSelectedDateMillis = epochDay * MILLIS_PER_DAY,
            yearRange = REASONABLE_YEAR_RANGE,
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { onEpochDayChange(it / MILLIS_PER_DAY) }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancelar") } },
        ) { DatePicker(state = dpState) }
    }

    OutlinedButton(onClick = { showPicker = true }, modifier = modifier) {
        Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(formatFechaCorta(epochDay, dateLocale))
    }
}
