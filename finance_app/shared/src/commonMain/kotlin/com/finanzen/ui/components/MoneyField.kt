package com.finanzen.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.finanzen.domain.Money
import com.finanzen.ui.theme.LocalMoneyFormat

/**
 * Campo de monto que mantiene `amountMinor` como fuente de verdad y formatea en vivo mientras se
 * escribe, estilo calculadora: cada dígito nuevo entra por la derecha (`amountMinor = amountMinor * 10
 * + dígito`) y el separador decimal aparece solo donde le corresponde a [currencyCode] (CLDR, vía
 * [LocalMoneyFormat]). El usuario nunca teclea el punto/coma decimal, así que no hay ambigüedad entre
 * separador de miles y decimal para ninguna moneda (0, 2 o 3 decimales).
 */
@Composable
fun MoneyField(
    amountMinor: Long,
    onAmountChange: (Long) -> Unit,
    currencyCode: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val row = LocalMoneyFormat.current.currencies[currencyCode]
    val decimals = row?.decimals?.toInt() ?: 2
    val displayText = remember(amountMinor, decimals, row) {
        if (amountMinor == 0L) {
            ""
        } else {
            Money(amountMinor, currencyCode).format(decimals, row?.decimalSeparator ?: ".", row?.groupSeparator ?: "")
        }
    }
    val fieldValue = remember(displayText) { TextFieldValue(displayText, TextRange(displayText.length)) }

    OutlinedTextField(
        value = fieldValue,
        onValueChange = { new -> onAmountChange(digitsToMinor(new.text)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        prefix = row?.symbol?.takeIf { it.isNotEmpty() }?.let { { Text(it) } },
        modifier = modifier,
    )
}

/** Extrae solo dígitos y los interpreta como `amountMinor` directo (sin separadores). */
internal fun digitsToMinor(text: String): Long = text.filter(Char::isDigit).toLongOrNull() ?: 0L
