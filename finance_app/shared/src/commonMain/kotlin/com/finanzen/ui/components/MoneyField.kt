package com.finanzen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    // Look "Sage" del handoff (etiqueta aparte arriba, sin label flotante, campo más redondeado):
    // ver KDoc de [PickerField] para el mismo flag. Por ahora solo lo usa TransactionFormScreen —
    // el resto de pantallas siguen con el default (label flotante de Material) sin cambios.
    plainLabel: Boolean = false,
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

    if (!plainLabel) {
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { new -> onAmountChange(digitsToMinor(new.text)) },
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            prefix = row?.symbol?.takeIf { it.isNotEmpty() }?.let { { Text(it) } },
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
            modifier = modifier,
        )
        return
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { new -> onAmountChange(digitsToMinor(new.text)) },
            placeholder = { Text("0") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            prefix = row?.symbol?.takeIf { it.isNotEmpty() }?.let { { Text(it) } },
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
            shape = MaterialTheme.shapes.medium,
            colors = sageFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Colores planos "Sage" para campos en modo [MoneyField.plainLabel] y variantes equivalentes en
 * [PickerField]/[DateField]: relleno `surfaceContainer`, borde `outlineVariant` en reposo y
 * `primary` en foco — igual a "Forms & Inputs" en DESIGN.md. */
@Composable
internal fun sageFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
)

/**
 * Extrae solo dígitos y los interpreta como `amountMinor` directo (sin separadores). Se recortan a
 * [MAX_DIGITS] (cubre montos de billones en la subunidad mínima) para que un desborde de Long no
 * resetee el campo a 0 en silencio — más allá de eso ningún monto real tiene sentido.
 */
internal fun digitsToMinor(text: String): Long = text.filter(Char::isDigit).take(MAX_DIGITS).toLongOrNull() ?: 0L

private const val MAX_DIGITS = 15
