package com.finanzen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finanzen.ui.theme.LocalSageType

/**
 * Tabla Sage: encabezado en mayúsculas pequeñas, filas separadas por una línea y un pie opcional
 * en negrita (el total). La primera columna es texto alineado a la izquierda; las demás son cifras
 * alineadas a la derecha con números tabulares. Todas las celdas son texto ya formateado.
 */
@Composable
fun DataTable(header: List<String>, rows: List<List<String>>, modifier: Modifier = Modifier, footer: List<String>? = null) {
    val shape = MaterialTheme.shapes.large
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
    ) {
        TableRow(header, LocalSageType.current.overline, MaterialTheme.colorScheme.surfaceVariant)
        rows.forEach { cells ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            TableRow(cells, LocalSageType.current.amountSm, MaterialTheme.colorScheme.surface)
        }
        if (footer != null) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            TableRow(footer, LocalSageType.current.amountMd, MaterialTheme.colorScheme.surface)
        }
    }
}

@Composable
private fun TableRow(cells: List<String>, style: TextStyle, background: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(background)
            .heightIn(min = 44.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cells.forEachIndexed { i, cell ->
            Text(
                cell,
                modifier = Modifier.weight(if (i == 0) FIRST_COLUMN_WEIGHT else 1f),
                style = style,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = if (i == 0) TextAlign.Start else TextAlign.End,
            )
        }
    }
}

private const val FIRST_COLUMN_WEIGHT = 0.8f
