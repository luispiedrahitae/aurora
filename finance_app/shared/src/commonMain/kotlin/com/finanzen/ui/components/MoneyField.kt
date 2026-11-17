package com.finanzen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finanzen.domain.Money
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSageType

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

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FieldLabel(label)
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { new -> onAmountChange(digitsToMinor(new.text)) },
            placeholder = { Text("0") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            prefix = row?.symbol?.takeIf { it.isNotEmpty() }?.let { { Text(it) } },
            textStyle = LocalSageType.current.amountMd.copy(textAlign = TextAlign.End),
            shape = MaterialTheme.shapes.small,
            colors = sageFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Colores de campo Sage: relleno hundido, borde fuerte en reposo y el azul de foco al escribir. */
@Composable
internal fun sageFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedBorderColor = LocalFinanceColors.current.focus,
    errorBorderColor = MaterialTheme.colorScheme.error,
    errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,
)

/** Etiqueta de campo: 14 medio, en tinta, encima del campo (nunca flotante). */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = LocalSageType.current.label, color = MaterialTheme.colorScheme.onSurface)
}

/**
 * Campo de texto Sage: etiqueta arriba, campo de 48 dp con fondo hundido y radio 10, ayuda o error
 * debajo. El error lleva icono además del color.
 */
@Composable
fun SageTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    help: String? = null,
    error: String? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    suffix: String? = null,
    enabled: Boolean = true,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder?.let { { Text(it) } },
            singleLine = singleLine,
            isError = error != null,
            enabled = enabled,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            suffix = suffix?.let { { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            textStyle = LocalSageType.current.body,
            shape = MaterialTheme.shapes.small,
            colors = sageFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        when {
            error != null -> FieldError(error)
            help != null -> Text(help, style = LocalSageType.current.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Extrae solo dígitos y los interpreta como `amountMinor` directo (sin separadores). Se recortan a
 * [MAX_DIGITS] (cubre montos de billones en la subunidad mínima) para que un desborde de Long no
 * resetee el campo a 0 en silencio — más allá de eso ningún monto real tiene sentido.
 */
internal fun digitsToMinor(text: String): Long = text.filter(Char::isDigit).take(MAX_DIGITS).toLongOrNull() ?: 0L

private const val MAX_DIGITS = 15
