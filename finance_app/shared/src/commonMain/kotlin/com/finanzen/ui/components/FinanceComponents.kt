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
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsBoat
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EvStation
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.GasMeter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Icecream
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalMall
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.LocalPizza
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PedalBike
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.RamenDining
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WineBar
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
import finanzen.shared.generated.resources.Res
import finanzen.shared.generated.resources.brand_airbnb
import finanzen.shared.generated.resources.brand_aliexpress
import finanzen.shared.generated.resources.brand_amazon
import finanzen.shared.generated.resources.brand_americanairlines
import finanzen.shared.generated.resources.brand_applemusic
import finanzen.shared.generated.resources.brand_appletv
import finanzen.shared.generated.resources.brand_avianca
import finanzen.shared.generated.resources.brand_battledotnet
import finanzen.shared.generated.resources.brand_bookingdotcom
import finanzen.shared.generated.resources.brand_coursera
import finanzen.shared.generated.resources.brand_crunchyroll
import finanzen.shared.generated.resources.brand_deezer
import finanzen.shared.generated.resources.brand_delta
import finanzen.shared.generated.resources.brand_discord
import finanzen.shared.generated.resources.brand_duolingo
import finanzen.shared.generated.resources.brand_ea
import finanzen.shared.generated.resources.brand_easyjet
import finanzen.shared.generated.resources.brand_edx
import finanzen.shared.generated.resources.brand_emirates
import finanzen.shared.generated.resources.brand_epicgames
import finanzen.shared.generated.resources.brand_facebook
import finanzen.shared.generated.resources.brand_gmail
import finanzen.shared.generated.resources.brand_google
import finanzen.shared.generated.resources.brand_googlecalendar
import finanzen.shared.generated.resources.brand_googlechrome
import finanzen.shared.generated.resources.brand_googleclassroom
import finanzen.shared.generated.resources.brand_googledrive
import finanzen.shared.generated.resources.brand_googlemaps
import finanzen.shared.generated.resources.brand_googlemeet
import finanzen.shared.generated.resources.brand_googlephotos
import finanzen.shared.generated.resources.brand_googleplay
import finanzen.shared.generated.resources.brand_googletranslate
import finanzen.shared.generated.resources.brand_grab
import finanzen.shared.generated.resources.brand_hbomax
import finanzen.shared.generated.resources.brand_hulu
import finanzen.shared.generated.resources.brand_ifood
import finanzen.shared.generated.resources.brand_instagram
import finanzen.shared.generated.resources.brand_kfc
import finanzen.shared.generated.resources.brand_khanacademy
import finanzen.shared.generated.resources.brand_lufthansa
import finanzen.shared.generated.resources.brand_lyft
import finanzen.shared.generated.resources.brand_mcdonalds
import finanzen.shared.generated.resources.brand_mercadopago
import finanzen.shared.generated.resources.brand_microsoftteams
import finanzen.shared.generated.resources.brand_netflix
import finanzen.shared.generated.resources.brand_nintendo
import finanzen.shared.generated.resources.brand_nintendoswitch
import finanzen.shared.generated.resources.brand_notion
import finanzen.shared.generated.resources.brand_nubank
import finanzen.shared.generated.resources.brand_openai
import finanzen.shared.generated.resources.brand_paramountplus
import finanzen.shared.generated.resources.brand_platzi
import finanzen.shared.generated.resources.brand_playstation
import finanzen.shared.generated.resources.brand_primevideo
import finanzen.shared.generated.resources.brand_quizlet
import finanzen.shared.generated.resources.brand_riotgames
import finanzen.shared.generated.resources.brand_ryanair
import finanzen.shared.generated.resources.brand_skillshare
import finanzen.shared.generated.resources.brand_spotify
import finanzen.shared.generated.resources.brand_steam
import finanzen.shared.generated.resources.brand_telegram
import finanzen.shared.generated.resources.brand_tiktok
import finanzen.shared.generated.resources.brand_twitch
import finanzen.shared.generated.resources.brand_uber
import finanzen.shared.generated.resources.brand_ubisoft
import finanzen.shared.generated.resources.brand_udacity
import finanzen.shared.generated.resources.brand_udemy
import finanzen.shared.generated.resources.brand_waze
import finanzen.shared.generated.resources.brand_whatsapp
import finanzen.shared.generated.resources.brand_wikipedia
import finanzen.shared.generated.resources.brand_x
import finanzen.shared.generated.resources.brand_xbox
import finanzen.shared.generated.resources.brand_youtube
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

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
 * Glifo de una categoría: o un vector de Material (iconos generales) o un drawable de marca
 * (logos de servicios, de Simple Icons / CC0). Ambos se pintan monocromos blancos sobre el círculo.
 */
sealed interface CategoryGlyph {
    data class Vec(val image: ImageVector) : CategoryGlyph
    data class Res(val drawable: DrawableResource) : CategoryGlyph
}

private fun vec(image: ImageVector): CategoryGlyph = CategoryGlyph.Vec(image)

/** Grupo de iconos con subtítulo; el selector renderiza una sección por grupo. */
data class IconGroup(val title: String, val icons: List<Pair<String, CategoryGlyph>>)

/**
 * Iconos ofrecidos al crear una categoría, organizados en subcategorías. Generales = Material Outlined
 * (material-icons-extended); marcas = logos Simple Icons (CC0). La clave (string) va en Category.icon;
 * las claves existentes se conservan para no romper datos sembrados.
 */
val iconGroups: List<IconGroup> = listOf(
    IconGroup(
        "Alimentación",
        listOf(
            "restaurant" to vec(Icons.Outlined.Restaurant),
            "groceries" to vec(Icons.Outlined.ShoppingCart),
            "coffee" to vec(Icons.Outlined.LocalCafe),
            "fastfood" to vec(Icons.Outlined.Fastfood),
            "localpizza" to vec(Icons.Outlined.LocalPizza),
            "ramen" to vec(Icons.Outlined.RamenDining),
            "icecream" to vec(Icons.Outlined.Icecream),
            "bakery" to vec(Icons.Outlined.BakeryDining),
            "localbar" to vec(Icons.Outlined.LocalBar),
            "wine" to vec(Icons.Outlined.WineBar),
        ),
    ),
    IconGroup(
        "Transporte",
        listOf(
            "car" to vec(Icons.Outlined.DirectionsCar),
            "fuel" to vec(Icons.Outlined.LocalGasStation),
            "transit" to vec(Icons.Outlined.Train),
            "bus" to vec(Icons.Outlined.DirectionsBus),
            "taxi" to vec(Icons.Outlined.LocalTaxi),
            "flight" to vec(Icons.Outlined.Flight),
            "motorcycle" to vec(Icons.Outlined.TwoWheeler),
            "bike" to vec(Icons.Outlined.PedalBike),
            "parking" to vec(Icons.Outlined.LocalParking),
            "ev" to vec(Icons.Outlined.EvStation),
            "boat" to vec(Icons.Outlined.DirectionsBoat),
        ),
    ),
    IconGroup(
        "Hogar y servicios",
        listOf(
            "home" to vec(Icons.Outlined.Home),
            "bills" to vec(Icons.Outlined.Receipt),
            "utilities" to vec(Icons.Outlined.Bolt),
            "water" to vec(Icons.Outlined.WaterDrop),
            "gas" to vec(Icons.Outlined.GasMeter),
            "internet" to vec(Icons.Outlined.Wifi),
            "phone" to vec(Icons.Outlined.Smartphone),
            "tv" to vec(Icons.Outlined.Tv),
            "subs" to vec(Icons.Outlined.Subscriptions),
            "cleaning" to vec(Icons.Outlined.CleaningServices),
            "furniture" to vec(Icons.Outlined.Weekend),
            "lightbulb" to vec(Icons.Outlined.Lightbulb),
            "tools" to vec(Icons.Outlined.Build),
        ),
    ),
    IconGroup(
        "Ocio y entretenimiento",
        listOf(
            "games" to vec(Icons.Outlined.SportsEsports),
            "movies" to vec(Icons.Outlined.Movie),
            "music" to vec(Icons.Outlined.MusicNote),
            "books" to vec(Icons.Outlined.MenuBook),
            "sports" to vec(Icons.Outlined.SportsSoccer),
            "gym" to vec(Icons.Outlined.FitnessCenter),
            "travel" to vec(Icons.Outlined.Flight),
            "beach" to vec(Icons.Outlined.BeachAccess),
            "art" to vec(Icons.Outlined.Palette),
            "theater" to vec(Icons.Outlined.TheaterComedy),
            "pets" to vec(Icons.Outlined.Pets),
            "celebration" to vec(Icons.Outlined.Cake),
        ),
    ),
    IconGroup(
        "Compras y otros",
        listOf(
            "shopping" to vec(Icons.Outlined.LocalMall),
            "clothes" to vec(Icons.Outlined.Checkroom),
            "beauty" to vec(Icons.Outlined.Spa),
            "gifts" to vec(Icons.Outlined.Redeem),
            "health" to vec(Icons.Outlined.MedicalServices),
            "education" to vec(Icons.Outlined.School),
            "kids" to vec(Icons.Outlined.ChildCare),
            "card" to vec(Icons.Outlined.CreditCard),
            "work" to vec(Icons.Outlined.Work),
            "salary" to vec(Icons.Outlined.Payments),
            "savings" to vec(Icons.Outlined.Savings),
            "investment" to vec(Icons.Outlined.TrendingUp),
            "donation" to vec(Icons.Outlined.VolunteerActivism),
            "favorite" to vec(Icons.Outlined.FavoriteBorder),
            "star" to vec(Icons.Outlined.StarBorder),
            "other" to vec(Icons.Outlined.Category),
        ),
    ),
    IconGroup(
        "Streaming",
        listOf(
            "brand_netflix" to CategoryGlyph.Res(Res.drawable.brand_netflix),
            "brand_primevideo" to CategoryGlyph.Res(Res.drawable.brand_primevideo),
            "brand_hbomax" to CategoryGlyph.Res(Res.drawable.brand_hbomax),
            "brand_hulu" to CategoryGlyph.Res(Res.drawable.brand_hulu),
            "brand_paramountplus" to CategoryGlyph.Res(Res.drawable.brand_paramountplus),
            "brand_crunchyroll" to CategoryGlyph.Res(Res.drawable.brand_crunchyroll),
            "brand_youtube" to CategoryGlyph.Res(Res.drawable.brand_youtube),
            "brand_appletv" to CategoryGlyph.Res(Res.drawable.brand_appletv),
            "brand_twitch" to CategoryGlyph.Res(Res.drawable.brand_twitch),
            "brand_spotify" to CategoryGlyph.Res(Res.drawable.brand_spotify),
            "brand_applemusic" to CategoryGlyph.Res(Res.drawable.brand_applemusic),
            "brand_deezer" to CategoryGlyph.Res(Res.drawable.brand_deezer),
        ),
    ),
    IconGroup(
        "Movilidad",
        listOf(
            "brand_uber" to CategoryGlyph.Res(Res.drawable.brand_uber),
            "brand_lyft" to CategoryGlyph.Res(Res.drawable.brand_lyft),
            "brand_grab" to CategoryGlyph.Res(Res.drawable.brand_grab),
            "brand_waze" to CategoryGlyph.Res(Res.drawable.brand_waze),
            "brand_ryanair" to CategoryGlyph.Res(Res.drawable.brand_ryanair),
            "brand_easyjet" to CategoryGlyph.Res(Res.drawable.brand_easyjet),
            "brand_lufthansa" to CategoryGlyph.Res(Res.drawable.brand_lufthansa),
            "brand_americanairlines" to CategoryGlyph.Res(Res.drawable.brand_americanairlines),
            "brand_delta" to CategoryGlyph.Res(Res.drawable.brand_delta),
            "brand_emirates" to CategoryGlyph.Res(Res.drawable.brand_emirates),
        ),
    ),
    IconGroup(
        "Google",
        listOf(
            "brand_google" to CategoryGlyph.Res(Res.drawable.brand_google),
            "brand_gmail" to CategoryGlyph.Res(Res.drawable.brand_gmail),
            "brand_googledrive" to CategoryGlyph.Res(Res.drawable.brand_googledrive),
            "brand_googlemaps" to CategoryGlyph.Res(Res.drawable.brand_googlemaps),
            "brand_googleplay" to CategoryGlyph.Res(Res.drawable.brand_googleplay),
            "brand_googlechrome" to CategoryGlyph.Res(Res.drawable.brand_googlechrome),
            "brand_googlephotos" to CategoryGlyph.Res(Res.drawable.brand_googlephotos),
            "brand_googlecalendar" to CategoryGlyph.Res(Res.drawable.brand_googlecalendar),
            "brand_googlemeet" to CategoryGlyph.Res(Res.drawable.brand_googlemeet),
            "brand_googletranslate" to CategoryGlyph.Res(Res.drawable.brand_googletranslate),
        ),
    ),
    IconGroup(
        "Estudio",
        listOf(
            "brand_duolingo" to CategoryGlyph.Res(Res.drawable.brand_duolingo),
            "brand_coursera" to CategoryGlyph.Res(Res.drawable.brand_coursera),
            "brand_platzi" to CategoryGlyph.Res(Res.drawable.brand_platzi),
            "brand_udemy" to CategoryGlyph.Res(Res.drawable.brand_udemy),
            "brand_edx" to CategoryGlyph.Res(Res.drawable.brand_edx),
            "brand_udacity" to CategoryGlyph.Res(Res.drawable.brand_udacity),
            "brand_skillshare" to CategoryGlyph.Res(Res.drawable.brand_skillshare),
            "brand_googleclassroom" to CategoryGlyph.Res(Res.drawable.brand_googleclassroom),
            "brand_microsoftteams" to CategoryGlyph.Res(Res.drawable.brand_microsoftteams),
            "brand_khanacademy" to CategoryGlyph.Res(Res.drawable.brand_khanacademy),
            "brand_quizlet" to CategoryGlyph.Res(Res.drawable.brand_quizlet),
            "brand_openai" to CategoryGlyph.Res(Res.drawable.brand_openai),
            "brand_wikipedia" to CategoryGlyph.Res(Res.drawable.brand_wikipedia),
            "brand_notion" to CategoryGlyph.Res(Res.drawable.brand_notion),
        ),
    ),
    IconGroup(
        "Videojuegos",
        listOf(
            "brand_xbox" to CategoryGlyph.Res(Res.drawable.brand_xbox),
            "brand_playstation" to CategoryGlyph.Res(Res.drawable.brand_playstation),
            "brand_nintendo" to CategoryGlyph.Res(Res.drawable.brand_nintendo),
            "brand_nintendoswitch" to CategoryGlyph.Res(Res.drawable.brand_nintendoswitch),
            "brand_steam" to CategoryGlyph.Res(Res.drawable.brand_steam),
            "brand_epicgames" to CategoryGlyph.Res(Res.drawable.brand_epicgames),
            "brand_riotgames" to CategoryGlyph.Res(Res.drawable.brand_riotgames),
            "brand_ea" to CategoryGlyph.Res(Res.drawable.brand_ea),
            "brand_ubisoft" to CategoryGlyph.Res(Res.drawable.brand_ubisoft),
            "brand_battledotnet" to CategoryGlyph.Res(Res.drawable.brand_battledotnet),
            "brand_discord" to CategoryGlyph.Res(Res.drawable.brand_discord),
        ),
    ),
    IconGroup(
        "Finanzas y pagos",
        listOf(
            "brand_nubank" to CategoryGlyph.Res(Res.drawable.brand_nubank),
            "brand_mercadopago" to CategoryGlyph.Res(Res.drawable.brand_mercadopago),
        ),
    ),
    IconGroup(
        "Compras y viajes",
        listOf(
            "brand_amazon" to CategoryGlyph.Res(Res.drawable.brand_amazon),
            "brand_aliexpress" to CategoryGlyph.Res(Res.drawable.brand_aliexpress),
            "brand_airbnb" to CategoryGlyph.Res(Res.drawable.brand_airbnb),
            "brand_bookingdotcom" to CategoryGlyph.Res(Res.drawable.brand_bookingdotcom),
            "brand_avianca" to CategoryGlyph.Res(Res.drawable.brand_avianca),
        ),
    ),
    IconGroup(
        "Domicilios y comida",
        listOf(
            "brand_ifood" to CategoryGlyph.Res(Res.drawable.brand_ifood),
            "brand_mcdonalds" to CategoryGlyph.Res(Res.drawable.brand_mcdonalds),
            "brand_kfc" to CategoryGlyph.Res(Res.drawable.brand_kfc),
        ),
    ),
    IconGroup(
        "Redes sociales",
        listOf(
            "brand_whatsapp" to CategoryGlyph.Res(Res.drawable.brand_whatsapp),
            "brand_instagram" to CategoryGlyph.Res(Res.drawable.brand_instagram),
            "brand_tiktok" to CategoryGlyph.Res(Res.drawable.brand_tiktok),
            "brand_facebook" to CategoryGlyph.Res(Res.drawable.brand_facebook),
            "brand_x" to CategoryGlyph.Res(Res.drawable.brand_x),
            "brand_telegram" to CategoryGlyph.Res(Res.drawable.brand_telegram),
        ),
    ),
)

/** Todos los iconos en plano (para lookups). La clave va en Category.icon. */
val categoryIcons: List<Pair<String, CategoryGlyph>> = iconGroups.flatMap { it.icons }

private val glyphByKey = categoryIcons.toMap()

/** Glifo de una categoría por su clave; cae a un icono genérico si la clave es desconocida o legacy. */
fun glyphForKey(key: String): CategoryGlyph = glyphByKey[key] ?: CategoryGlyph.Vec(Icons.Outlined.Category)

/** Glifo blanco (vector Material o logo de marca) dentro de un círculo de color. Avatar único. */
@Composable
fun CategoryAvatar(icon: String, color: Color, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center,
    ) {
        val glyphModifier = Modifier.size(size * 0.58f)
        when (val glyph = glyphForKey(icon)) {
            is CategoryGlyph.Vec -> Icon(glyph.image, contentDescription = null, tint = Color.White, modifier = glyphModifier)
            is CategoryGlyph.Res -> Icon(painterResource(glyph.drawable), contentDescription = null, tint = Color.White, modifier = glyphModifier)
        }
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
