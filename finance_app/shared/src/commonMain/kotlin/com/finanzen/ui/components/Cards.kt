package com.finanzen.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.finanzen.ui.theme.LocalSageType
import com.finanzen.ui.theme.LocalSpacing

/**
 * Variante de tarjeta. [Hero] es la tarjeta de resumen (cifra principal de la pantalla, 20 de padding);
 * [Medium] la tarjeta normal (16); [Small] una tarjeta compacta (12). Las tres son planas: superficie
 * más borde fino, sin sombra.
 */
enum class BentoTileSize { Hero, Medium, Small }

/** Tarjeta Sage: superficie, borde de 1 dp y radio 16. Con [onClick] toda la tarjeta es un botón. */
@Composable
fun FinanceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    size: BentoTileSize = BentoTileSize.Medium,
    color: Color = MaterialTheme.colorScheme.surface,
    contentPadding: PaddingValues = PaddingValues(cardPadding(size)),
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    content: @Composable () -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val inner: @Composable () -> Unit = { Box(Modifier.padding(contentPadding)) { content() } }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, border = border, content = inner)
    } else {
        Surface(modifier = modifier, shape = shape, color = color, border = border, content = inner)
    }
}

private fun cardPadding(size: BentoTileSize) = when (size) {
    BentoTileSize.Hero -> 20.dp
    BentoTileSize.Medium -> 16.dp
    BentoTileSize.Small -> 12.dp
}

/**
 * `contentPadding` para [FinanceCard] cuando la fila termina en un `IconButton`: el botón ya reserva
 * 12 dp de aire alrededor de su icono, así que el borde final se recorta para que el glifo quede al
 * mismo margen visual que en las filas sin botón.
 */
fun trailingIconButtonPadding(size: BentoTileSize = BentoTileSize.Medium): PaddingValues {
    val edge = cardPadding(size)
    return PaddingValues(start = edge, top = edge, bottom = edge, end = 4.dp)
}

/** Título de sección dentro de una pantalla (Caprasimo 20). Va 8 dp encima de su contenido. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(top = LocalSpacing.current.xs).semantics { heading() },
        style = LocalSageType.current.titleSection,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

/** Etiqueta pequeña en mayúsculas sobre una cifra o un grupo ("SALDO TOTAL", "HOY · 16 NOV"). */
@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text.uppercase(), modifier = modifier, style = LocalSageType.current.overline, color = color)
}
