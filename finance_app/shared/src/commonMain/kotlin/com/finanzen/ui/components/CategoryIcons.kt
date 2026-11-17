package com.finanzen.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Commute
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DeliveryDining
import androidx.compose.material.icons.outlined.DirectionsBoat
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EvStation
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.GasMeter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Icecream
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalMall
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.LocalPizza
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.MedicalServices
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
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WineBar
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.finanzen.ui.theme.LightCategoryColors
import org.jetbrains.compose.resources.DrawableResource
import sage.shared.generated.resources.Res
import sage.shared.generated.resources.brand_airbnb
import sage.shared.generated.resources.brand_aliexpress
import sage.shared.generated.resources.brand_amazon
import sage.shared.generated.resources.brand_americanairlines
import sage.shared.generated.resources.brand_applemusic
import sage.shared.generated.resources.brand_appletv
import sage.shared.generated.resources.brand_avianca
import sage.shared.generated.resources.brand_battledotnet
import sage.shared.generated.resources.brand_bookingdotcom
import sage.shared.generated.resources.brand_coursera
import sage.shared.generated.resources.brand_crunchyroll
import sage.shared.generated.resources.brand_deezer
import sage.shared.generated.resources.brand_delta
import sage.shared.generated.resources.brand_discord
import sage.shared.generated.resources.brand_duolingo
import sage.shared.generated.resources.brand_ea
import sage.shared.generated.resources.brand_easyjet
import sage.shared.generated.resources.brand_edx
import sage.shared.generated.resources.brand_emirates
import sage.shared.generated.resources.brand_epicgames
import sage.shared.generated.resources.brand_facebook
import sage.shared.generated.resources.brand_gmail
import sage.shared.generated.resources.brand_google
import sage.shared.generated.resources.brand_googlecalendar
import sage.shared.generated.resources.brand_googlechrome
import sage.shared.generated.resources.brand_googleclassroom
import sage.shared.generated.resources.brand_googledrive
import sage.shared.generated.resources.brand_googlemaps
import sage.shared.generated.resources.brand_googlemeet
import sage.shared.generated.resources.brand_googlephotos
import sage.shared.generated.resources.brand_googleplay
import sage.shared.generated.resources.brand_googletranslate
import sage.shared.generated.resources.brand_grab
import sage.shared.generated.resources.brand_hbomax
import sage.shared.generated.resources.brand_hulu
import sage.shared.generated.resources.brand_ifood
import sage.shared.generated.resources.brand_instagram
import sage.shared.generated.resources.brand_kfc
import sage.shared.generated.resources.brand_khanacademy
import sage.shared.generated.resources.brand_lufthansa
import sage.shared.generated.resources.brand_lyft
import sage.shared.generated.resources.brand_mcdonalds
import sage.shared.generated.resources.brand_mercadopago
import sage.shared.generated.resources.brand_microsoftteams
import sage.shared.generated.resources.brand_netflix
import sage.shared.generated.resources.brand_nintendo
import sage.shared.generated.resources.brand_nintendoswitch
import sage.shared.generated.resources.brand_notion
import sage.shared.generated.resources.brand_nubank
import sage.shared.generated.resources.brand_openai
import sage.shared.generated.resources.brand_paramountplus
import sage.shared.generated.resources.brand_platzi
import sage.shared.generated.resources.brand_playstation
import sage.shared.generated.resources.brand_primevideo
import sage.shared.generated.resources.brand_quizlet
import sage.shared.generated.resources.brand_riotgames
import sage.shared.generated.resources.brand_ryanair
import sage.shared.generated.resources.brand_skillshare
import sage.shared.generated.resources.brand_spotify
import sage.shared.generated.resources.brand_steam
import sage.shared.generated.resources.brand_telegram
import sage.shared.generated.resources.brand_tiktok
import sage.shared.generated.resources.brand_twitch
import sage.shared.generated.resources.brand_uber
import sage.shared.generated.resources.brand_ubisoft
import sage.shared.generated.resources.brand_udacity
import sage.shared.generated.resources.brand_udemy
import sage.shared.generated.resources.brand_waze
import sage.shared.generated.resources.brand_whatsapp
import sage.shared.generated.resources.brand_wikipedia
import sage.shared.generated.resources.brand_x
import sage.shared.generated.resources.brand_xbox
import sage.shared.generated.resources.brand_youtube

/**
 * Color por hash del nombre dentro de [palette] — fallback cuando la categoría no tiene color
 * propio (color = 0). [palette] es [LightCategoryColors]/[DarkCategoryColors] (ver
 * [LocalCategoryColors]): mismo hash, tono claro u oscuro según el tema activo.
 *
 * Deliberadamente distinto del rol de [CategoryAvatar]/[KindAvatar], que usan el acento de
 * Apariencia: el avatar identifica de forma uniforme (la categoría se distingue por el icono),
 * el chart codifica series de datos y sí necesita un color por categoría.
 */
fun colorForCategory(name: String, palette: List<Color>): Color = palette[((name.hashCode() % palette.size) + palette.size) % palette.size]

/** Color efectivo de una categoría: el guardado (ARGB en [storedColor]) si lo hay, si no el del hash. */
fun categoryColor(name: String, storedColor: Long, palette: List<Color>): Color = if (storedColor != 0L) Color(storedColor.toInt()) else colorForCategory(name, palette)

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
 * Catálogo de iconos por categoría (generales = Material Outlined vía material-icons-extended;
 * marcas = logos Simple Icons CC0). La clave (string) va en Category.icon; las claves existentes
 * se conservan para no romper datos sembrados.
 *
 * ponytail: sin selector de UI que lo use hoy (los avatares muestran la inicial del nombre, no un
 * icono) — se conserva íntegro por si una futura pantalla vuelve a ofrecer selección de icono.
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
            "books" to vec(Icons.AutoMirrored.Outlined.MenuBook),
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
            "investment" to vec(Icons.AutoMirrored.Outlined.TrendingUp),
            "donation" to vec(Icons.Outlined.VolunteerActivism),
            "favorite" to vec(Icons.Outlined.FavoriteBorder),
            "star" to vec(Icons.Outlined.StarBorder),
            "other" to vec(Icons.Outlined.Category),
        ),
    ),
    IconGroup(
        "Streaming",
        listOf(
            "streaming" to vec(Icons.Outlined.LiveTv),
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
            "mobility" to vec(Icons.Outlined.Commute),
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
            "finance" to vec(Icons.Outlined.AccountBalanceWallet),
            "brand_nubank" to CategoryGlyph.Res(Res.drawable.brand_nubank),
            "brand_mercadopago" to CategoryGlyph.Res(Res.drawable.brand_mercadopago),
        ),
    ),
    IconGroup(
        "Compras y viajes",
        listOf(
            "commerce" to vec(Icons.Outlined.Storefront),
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
            "delivery" to vec(Icons.Outlined.DeliveryDining),
            "brand_ifood" to CategoryGlyph.Res(Res.drawable.brand_ifood),
            "brand_mcdonalds" to CategoryGlyph.Res(Res.drawable.brand_mcdonalds),
            "brand_kfc" to CategoryGlyph.Res(Res.drawable.brand_kfc),
        ),
    ),
    IconGroup(
        "Redes sociales",
        listOf(
            "social" to vec(Icons.Outlined.Forum),
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
