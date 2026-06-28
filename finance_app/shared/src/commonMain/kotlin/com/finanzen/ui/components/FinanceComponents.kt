package com.finanzen.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalMall
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.finanzen.domain.Money
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalSpacing

/**
 * Superficie base de todas las tarjetas: tonal (sin sombras pesadas), radio `large`, padding
 * consistente. Centraliza la apariencia para no repetir Card+padding en cada pantalla.
 */
@Composable
fun FinanceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val inner: @Composable () -> Unit = { Box(Modifier.padding(contentPadding)) { content() } }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, content = inner)
    } else {
        Surface(modifier = modifier, shape = shape, color = color, content = inner)
    }
}

/**
 * Formatea [Money] con cifras tabulares y color por signo (verde ingreso / rojo gasto).
 * Centraliza el formateo de moneda — antes repetido en cada pantalla con interpolación a mano.
 */
@Composable
fun MoneyText(
    amountMinor: Long,
    currency: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    fontWeight: FontWeight? = FontWeight.SemiBold,
    signed: Boolean = false,
    showCurrency: Boolean = true,
    colorOverride: Color? = null,
) {
    val finance = LocalFinanceColors.current
    val color = colorOverride ?: when {
        !signed -> MaterialTheme.colorScheme.onSurface
        amountMinor >= 0 -> finance.income
        else -> finance.expense
    }
    val sign = if (signed && amountMinor > 0) "+" else "" // Money.format ya antepone "-"
    val text = "$sign${Money(amountMinor, currency).format()}" + if (showCurrency) " $currency" else ""
    Text(text, modifier = modifier, style = style, fontWeight = fontWeight, color = color)
}

/** Píldora compacta para ingreso/gasto/neto del Dashboard. */
@Composable
fun StatPill(
    label: String,
    amountMinor: Long,
    currency: String,
    accent: Color,
    container: Color,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.medium, color = container) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MoneyText(
                amountMinor = amountMinor,
                currency = currency,
                style = MaterialTheme.typography.titleMedium,
                showCurrency = false,
                colorOverride = accent,
            )
        }
    }
}

/** Encabezado de sección consistente. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(top = LocalSpacing.current.xs),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
    )
}

/** Fila de categoría con barra de progreso redondeada (tokens en vez de RoundedCornerShape(4dp)). */
@Composable
fun CategoryProgressRow(
    name: String,
    amountMinor: Long,
    currency: String,
    pct: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val animatedPct by animateFloatAsState(targetValue = pct.coerceIn(0f, 1f), animationSpec = tween(600))
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(name, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
            MoneyText(amountMinor, currency, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedPct)
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
        }
    }
}

/**
 * Paleta amplia para los círculos de categoría y oferta del selector de color. Tonos saturados
 * repartidos por la rueda de color, todos legibles con el glifo blanco encima (≥3:1 en glifo grande).
 */
val categoryColors = listOf(
    Color(0xFFE53935), Color(0xFFD81B60), Color(0xFFAD1457), Color(0xFF8E24AA),
    Color(0xFF6A1B9A), Color(0xFF5E35B1), Color(0xFF3949AB), Color(0xFF1E88E5),
    Color(0xFF1565C0), Color(0xFF0277BD), Color(0xFF00838F), Color(0xFF00897B),
    Color(0xFF2E7D32), Color(0xFF43A047), Color(0xFF558B2F), Color(0xFF827717),
    Color(0xFFEF6C00), Color(0xFFFB8C00), Color(0xFFF4511E), Color(0xFF6D4C41),
    Color(0xFF8D6E63), Color(0xFF546E7A), Color(0xFF455A64), Color(0xFF757575),
)

/** Color por hash del nombre, fallback cuando la categoría no tiene color propio (color = 0). */
fun colorForCategory(name: String): Color = categoryColors[((name.hashCode() % categoryColors.size) + categoryColors.size) % categoryColors.size]

/** Color efectivo de una categoría: el guardado (ARGB en [storedColor]) si lo hay, si no el del hash. */
fun categoryColor(name: String, storedColor: Long): Color = if (storedColor != 0L) Color(storedColor.toInt()) else colorForCategory(name)

/**
 * Iconos vectoriales (Material Outlined, ya disponibles vía material-icons-extended) ofrecidos al
 * crear una categoría. La clave (string estable) es lo que se guarda en Category.icon.
 */
val categoryIcons: List<Pair<String, ImageVector>> = listOf(
    "restaurant" to Icons.Outlined.Restaurant,
    "groceries" to Icons.Outlined.ShoppingCart,
    "shopping" to Icons.Outlined.LocalMall,
    "car" to Icons.Outlined.DirectionsCar,
    "fuel" to Icons.Outlined.LocalGasStation,
    "transit" to Icons.Outlined.Train,
    "home" to Icons.Outlined.Home,
    "bills" to Icons.Outlined.Receipt,
    "utilities" to Icons.Outlined.Bolt,
    "water" to Icons.Outlined.WaterDrop,
    "phone" to Icons.Outlined.Smartphone,
    "internet" to Icons.Outlined.Wifi,
    "tv" to Icons.Outlined.Tv,
    "subs" to Icons.Outlined.Subscriptions,
    "health" to Icons.Outlined.MedicalServices,
    "gym" to Icons.Outlined.FitnessCenter,
    "beauty" to Icons.Outlined.Spa,
    "games" to Icons.Outlined.SportsEsports,
    "movies" to Icons.Outlined.Movie,
    "music" to Icons.Outlined.MusicNote,
    "books" to Icons.Outlined.MenuBook,
    "travel" to Icons.Outlined.Flight,
    "beach" to Icons.Outlined.BeachAccess,
    "clothes" to Icons.Outlined.Checkroom,
    "education" to Icons.Outlined.School,
    "kids" to Icons.Outlined.ChildCare,
    "pets" to Icons.Outlined.Pets,
    "coffee" to Icons.Outlined.LocalCafe,
    "gifts" to Icons.Outlined.Redeem,
    "celebration" to Icons.Outlined.Cake,
    "donation" to Icons.Outlined.VolunteerActivism,
    "tools" to Icons.Outlined.Build,
    "work" to Icons.Outlined.Work,
    "salary" to Icons.Outlined.Payments,
    "card" to Icons.Outlined.CreditCard,
    "savings" to Icons.Outlined.Savings,
    "investment" to Icons.Outlined.TrendingUp,
    "favorite" to Icons.Outlined.FavoriteBorder,
    "star" to Icons.Outlined.StarBorder,
    "other" to Icons.Outlined.Category,
)

private val iconsByKey = categoryIcons.toMap()

/** Vector de una categoría por su clave; cae a un icono genérico si la clave es desconocida o legacy. */
fun iconForKey(key: String): ImageVector = iconsByKey[key] ?: Icons.Outlined.Category

/** Icono vectorial blanco dentro de un círculo de color. Avatar único usado en formularios y listas. */
@Composable
fun CategoryAvatar(icon: String, color: Color, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = iconForKey(icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.58f),
        )
    }
}

/** Estado vacío reutilizable: icono + título + subtítulo. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.padding(20.dp).size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
