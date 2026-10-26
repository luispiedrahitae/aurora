package com.finanzen.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
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
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PedalBike
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PriceChange
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
import androidx.compose.material.icons.outlined.SwapHoriz
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.finanzen.domain.Money
import com.finanzen.ui.format.PeriodMode
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.FinanceColors
import com.finanzen.ui.theme.LightCategoryColors
import com.finanzen.ui.theme.LocalAccentColor
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.ui.theme.PillShape
import com.finanzen.ui.theme.displayFontFamily
import com.finanzen.ui.theme.motionTween
import kotlinx.coroutines.launch
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
import kotlin.math.roundToInt

/** Los tres tamaños de tile bento (DESIGN.md, sección "Bento Tiles"). Fija radio/padding/tono por defecto. */
enum class BentoTileSize { Hero, Medium, Small }

/**
 * Superficie base de todos los tiles bento: plana con sombra suave, radio y padding derivados de
 * [size]. Centraliza la apariencia para no repetir Card+padding en cada pantalla.
 */
@Composable
fun FinanceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    size: BentoTileSize = BentoTileSize.Medium,
    color: Color = when (size) {
        BentoTileSize.Hero -> MaterialTheme.colorScheme.surfaceContainerHigh
        BentoTileSize.Medium -> MaterialTheme.colorScheme.surfaceContainer
        BentoTileSize.Small -> MaterialTheme.colorScheme.surfaceContainerLow
    },
    contentPadding: PaddingValues = when (size) {
        BentoTileSize.Hero -> PaddingValues(20.dp)
        BentoTileSize.Medium -> PaddingValues(16.dp)
        BentoTileSize.Small -> PaddingValues(12.dp)
    },
    content: @Composable () -> Unit,
) {
    // Sage converge todas las bento tiles al radio base de 16dp (ver DESIGN.md, Bento Tiles) en vez
    // de escalar el radio por tamaño — el tamaño y la sombra hacen la jerarquía, no la forma.
    val shape = MaterialTheme.shapes.medium
    // Vocabulario de sombra suave de Sage (DESIGN.md, shadow-sm/md/lg) en vez de vidrio/hairline.
    val shadowElevation = when (size) {
        BentoTileSize.Hero -> 6.dp
        BentoTileSize.Medium -> 3.dp
        BentoTileSize.Small -> 1.dp
    }
    val inner: @Composable () -> Unit = { Box(Modifier.padding(contentPadding)) { content() } }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = color,
            shadowElevation = shadowElevation,
            content = inner,
        )
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = color,
            shadowElevation = shadowElevation,
            content = inner,
        )
    }
}

/**
 * Variante de `contentPadding` para [FinanceCard] cuando el último elemento de la fila es un
 * `IconButton` (p. ej. un chevron de expandir/contraer). Un `IconButton` sin modificadores
 * reserva un target táctil de 48dp con el ícono de 24dp centrado adentro — sin este ajuste, el
 * glifo queda ~12dp más lejos del borde que en filas sin `IconButton` (p. ej. `DayHeader` en
 * Movimientos, a `spacing.lg` = 16dp plano). Este helper recorta el padding final para que ambos
 * casos terminen al mismo margen visual, sin tocar el tamaño estándar del ícono/botón.
 *
 * Esto solo arregla el lado del borde de la tarjeta. Del otro lado (entre el `IconButton` y el
 * contenido que lo precede, p. ej. el saldo), el propio padding interno del botón (~12dp) ya hace
 * de separación — si además ese `IconButton` va dentro de un `Row` con `Arrangement.spacedBy`, el
 * hueco se duplica. Ponlo en su propio `Row` sin `horizontalArrangement` (spacing 0 por defecto)
 * junto al elemento anterior, para que solo el padding del botón separe ambos — ver `AccountRow`
 * en `AccountsTabScreen.kt`.
 */
fun trailingIconButtonPadding(size: BentoTileSize = BentoTileSize.Medium): PaddingValues {
    val edge = when (size) {
        BentoTileSize.Hero -> 20.dp
        BentoTileSize.Medium -> 16.dp
        BentoTileSize.Small -> 12.dp
    }
    return PaddingValues(start = edge, top = edge, bottom = edge, end = 4.dp)
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
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    fontWeight: FontWeight? = FontWeight.SemiBold,
    signed: Boolean = false,
    colorOverride: Color? = null,
    textAlign: TextAlign? = null,
) {
    val finance = LocalFinanceColors.current
    val color = colorOverride ?: when {
        !signed -> MaterialTheme.colorScheme.onSurface
        amountMinor >= 0 -> finance.income
        else -> finance.expense
    }
    // El símbolo y su posición ($ antes/después/ninguno) los aporta el formato global.
    val text = LocalMoneyFormat.current.format(amountMinor, currency, signed)
    Text(text, modifier = modifier, style = style, fontWeight = fontWeight, color = color, textAlign = textAlign)
}

/**
 * Texto que se encoge hasta caber en **una sola línea** dentro del ancho disponible. Mide con
 * [rememberTextMeasurer] bajando de [maxFontSize] a [minFontSize] hasta que entra. Útil para el balance
 * del Dashboard, que debe quedar en un renglón sin importar la magnitud.
 */
@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit = 40.sp,
    minFontSize: TextUnit = 16.sp,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    style: TextStyle = MaterialTheme.typography.displaySmall,
) {
    BoxWithConstraints(modifier) {
        val measurer = rememberTextMeasurer()
        val maxWidthPx = constraints.maxWidth
        val fitted = remember(text, maxWidthPx, maxFontSize, minFontSize, style) {
            var size = maxFontSize
            while (size.value > minFontSize.value) {
                val width = measurer.measure(
                    text = AnnotatedString(text),
                    style = style.copy(fontSize = size, fontWeight = fontWeight ?: style.fontWeight),
                    maxLines = 1,
                    softWrap = false,
                ).size.width
                if (width <= maxWidthPx) break
                size = (size.value - 1f).sp
            }
            size
        }
        Text(
            text,
            style = style,
            fontSize = fitted,
            fontWeight = fontWeight,
            color = color,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** Encabezado de sección consistente. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        // Sage.dc.html renderiza estos h6 en mayúsculas (el texto fuente va en minúsculas) sobre
        // un color atenuado (~55% del ink) y con la fuente display (Caprasimo), pero sin
        // font-size inline -- medí el cap-height real en screenshots/03-cuentas.png contra el H1
        // "Cuentas" (22px conocidos, misma fuente): ~15px de cap-height para 22px de font-size
        // (ratio 0.68) vs ~8-9px de cap-height para el h6 "TARJETAS DE CRÉDITO", lo que da un
        // font-size de mockup de ~12-13px -- muy lejos de los 20sp (rol `title`) que documenta
        // DESIGN.md, que está desactualizado. 13sp puro (rol `label`) igual se vio demasiado
        // chico en dispositivo real (Caprasimo tiene menos cap-height por punto que Figtree), así
        // que 15sp es el punto intermedio elegido en vez de reusar un rol tal cual.
        text.uppercase(),
        modifier = modifier.padding(top = LocalSpacing.current.xs),
        style = MaterialTheme.typography.labelLarge.copy(
            fontFamily = displayFontFamily(),
            fontSize = 15.sp,
            letterSpacing = 0.04.em,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Selector de mes con flechas prev/next, reutilizado por las pantallas que filtran por periodo.
 * Con [onLabelClick], la etiqueta central es tocable y vuelve al periodo actual ("hoy"). */
@Composable
fun MonthSelector(
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    onLabelClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = LocalSpacing.current.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrev) {
            Icon(LucideIcons.ChevronLeft, contentDescription = "Mes anterior", tint = MaterialTheme.colorScheme.primary)
        }
        SelectorLabel(label, onLabelClick, onClickLabel = "Volver al mes actual")
        IconButton(onClick = onNext) {
            Icon(LucideIcons.ChevronRight, contentDescription = "Mes siguiente", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Selector de anio con flechas prev/next, mismo patron que [MonthSelector] pero para pantallas
 * anuales (Resumen). */
@Composable
fun YearSelector(
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    onLabelClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = LocalSpacing.current.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrev) {
            Icon(LucideIcons.ChevronLeft, contentDescription = "Año anterior", tint = MaterialTheme.colorScheme.primary)
        }
        SelectorLabel(label, onLabelClick, onClickLabel = "Volver al año actual")
        IconButton(onClick = onNext) {
            Icon(LucideIcons.ChevronRight, contentDescription = "Año siguiente", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/**
 * Chip compacto de un toque que alterna entre modo Mensual y Anual — pensado para la esquina
 * superior derecha de [MainTabHeader] (`action`), a la altura del título; el selector prev/next del
 * periodo elegido sigue siendo [MonthSelector]/[YearSelector] debajo, sin cambios.
 */
@Composable
fun PeriodModeChip(mode: PeriodMode, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val label = when (mode) {
        PeriodMode.MONTH -> "Mes"
        PeriodMode.YEAR -> "Año"
    }
    val nextLabel = when (mode) {
        PeriodMode.MONTH -> "año"
        PeriodMode.YEAR -> "mes"
    }
    Surface(
        onClick = onToggle,
        modifier = modifier.semantics {
            role = Role.Switch
            contentDescription = "Vista: $label. Cambiar a vista de $nextLabel"
        },
        shape = PillShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = LocalSpacing.current.md, vertical = LocalSpacing.current.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs),
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Icon(
                LucideIcons.ChevronsUpDown,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Envuelve [content] en un gesto de deslizar-para-eliminar (deslizar de derecha a izquierda revela
 * un fondo terracota con el icono de papelera), el mismo patrón ya usado en Movimientos y Cuentas —
 * evita triplicarlo inline en cada pantalla de "Más" que tenía en su lugar un botón de papelera
 * suelto en la tarjeta. [onSwipeToDelete] decide si el gesto se consuma (`true`, la fila desaparece)
 * o si vuelve a su sitio (`false`) — p. ej. para abrir un diálogo de confirmación en vez de borrar
 * directo. [shape] recorta el fondo rojo al mismo radio que la tarjeta que envuelve (por defecto el
 * de [FinanceCard]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteRow(
    onSwipeToDelete: () -> Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) onSwipeToDelete() else false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    // Terracota (finance.expense*), no el rojo de error: DESIGN.md "No-Shame Rule".
                    .background(LocalFinanceColors.current.expenseContainer)
                    .padding(horizontal = LocalSpacing.current.lg),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(LucideIcons.Trash, contentDescription = "Eliminar", tint = LocalFinanceColors.current.expense)
            }
        },
        content = { content() },
    )
}

/** Etiqueta central de los selectores de periodo; tocable como boton cuando hay accion "hoy". */
@Composable
private fun SelectorLabel(label: String, onClick: (() -> Unit)?, onClickLabel: String) {
    val clickable = if (onClick != null) {
        Modifier
            .clip(PillShape)
            .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
            .padding(horizontal = LocalSpacing.current.md, vertical = LocalSpacing.current.xs)
    } else {
        Modifier
    }
    Text(label, modifier = clickable, style = MaterialTheme.typography.titleMedium)
}

/**
 * Icono "i" con tooltip (hover en desktop, toque/toque-largo en móvil). Buen patrón para explicar
 * un control o gráfica sin recargar la fila con texto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoTooltip(text: String, contentDescription: String = "Más información", modifier: Modifier = Modifier) {
    val state = rememberTooltipState(isPersistent = false)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(text) } },
        state = state,
        modifier = modifier,
    ) {
        IconButton(onClick = { scope.launch { state.show() } }) {
            Icon(
                LucideIcons.Info,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
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
    val animatedPct by animateFloatAsState(targetValue = pct.coerceIn(0f, 1f), animationSpec = motionTween(400))
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${(pct.coerceIn(0f, 1f) * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MoneyText(amountMinor, currency, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedPct)
                    .height(10.dp)
                    .clip(PillShape)
                    .background(color),
            )
        }
    }
}

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

/**
 * Inicial en negrilla y blanco dentro de un círculo del color de acento de Apariencia — mismo
 * tratamiento uniforme que [AccountTypeAvatar], la categoría/subcategoría se distingue por su
 * nombre, no por un icono propio. Avatar único.
 */
@Composable
fun CategoryAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(LocalAccentColor.current),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().take(1).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp,
        )
    }
}

/** Icono por tipo de cuenta (efectivo/débito/ahorros/crédito). */
fun accountTypeIcon(type: String): ImageVector = when (type) {
    "CASH" -> Icons.Outlined.Payments
    "SAVINGS" -> Icons.Outlined.Savings
    "DEBIT" -> Icons.Outlined.AccountBalance
    "CREDIT" -> Icons.Outlined.CreditCard
    else -> Icons.Outlined.AccountBalanceWallet
}

/** Etiqueta en español por tipo de cuenta (efectivo/débito/ahorros/crédito). */
fun accountTypeLabel(type: String): String = when (type) {
    "CASH" -> "Efectivo"
    "DEBIT" -> "Débito"
    "SAVINGS" -> "Ahorros"
    "CREDIT" -> "Crédito"
    else -> type
}

/** Etiqueta en español por tipo de movimiento (ingreso/gasto/transferencia). */
fun kindLabel(kind: String): String = when (kind) {
    "INCOME" -> "Ingreso"
    "TRANSFER" -> "Transferencia"
    "ADJUSTMENT" -> "Ajuste de saldo"
    else -> "Gasto"
}

/**
 * Avatar por tipo de movimiento: círculo del color de acento de Apariencia + flecha blanca, mismo
 * patrón que [CategoryAvatar]. Fallback cuando el movimiento no tiene categoría (transferencias,
 * o ingreso/gasto sin categoría asignada); el tipo se distingue por el glifo, no por el color.
 */
@Composable
fun KindAvatar(kind: String, modifier: Modifier = Modifier, size: Dp = 40.dp, icon: ImageVector? = null) {
    val resolvedIcon = icon ?: when (kind) {
        "INCOME" -> Icons.Outlined.ArrowUpward
        "TRANSFER" -> Icons.Outlined.SwapHoriz
        "ADJUSTMENT" -> Icons.Outlined.PriceChange
        else -> Icons.Outlined.ArrowDownward
    }
    Box(modifier.size(size).clip(CircleShape).background(LocalAccentColor.current), contentAlignment = Alignment.Center) {
        Icon(resolvedIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.5f))
    }
}

/**
 * Monto con signo correcto y color semántico según el tipo de movimiento, para filas de solo
 * lectura (Movimientos, Calendario, detalle de cuenta). ADJUSTMENT ya trae el signo correcto en
 * `amountMinor` (positivo = sube el saldo, negativo = lo baja) — a diferencia de EXPENSE, que se
 * niega aquí para mostrarse en rojo.
 */
fun signedAmountAndColor(kind: String, amountMinor: Long, finance: FinanceColors): Pair<Long, Color> {
    val isIncome = kind == "INCOME"
    val isAdjustment = kind == "ADJUSTMENT"
    val signedAmount = if (isIncome || isAdjustment) amountMinor else -amountMinor
    val color = when {
        isAdjustment -> if (amountMinor >= 0) finance.income else finance.expense
        isIncome -> finance.income
        else -> finance.expense
    }
    return signedAmount to color
}

/** Avatar por tipo de cuenta: círculo del color de acento de Apariencia + icono blanco del tipo,
 * mismo tratamiento que [CategoryAvatar]/[KindAvatar]. */
@Composable
fun AccountTypeAvatar(type: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier.size(size).clip(CircleShape).background(LocalAccentColor.current),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            accountTypeIcon(type),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/** Variación relativa de un KPI frente al período anterior ("+12% vs 2025"). [isGood] decide el
 * color semántico: subir es bueno en ingresos y malo en gastos. */
data class KpiDelta(val pct: Int, val isGood: Boolean, val vsLabel: String)

/** Delta porcentual [current] vs [previous]; null sin base de comparación (período anterior en 0). */
fun kpiDelta(current: Long, previous: Long, vsLabel: String, upIsGood: Boolean): KpiDelta? {
    if (previous <= 0L) return null
    val pct = ((current - previous) * 100.0 / previous).roundToInt()
    val isGood = if (pct >= 0) upIsGood else !upIsGood
    return KpiDelta(pct = pct, isGood = isGood, vsLabel = vsLabel)
}

/**
 * Avatar genérico (círculo del color de acento de Apariencia + icono blanco), para filas cuyo
 * icono no es una categoría ni un tipo de cuenta (suscripciones, inversiones) — mismo tratamiento
 * que [CategoryAvatar]/[KindAvatar]/[AccountTypeAvatar]. Un solo lugar en vez de un Box ad-hoc
 * copiado por pantalla.
 */
@Composable
fun IconAvatar(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier.size(size).clip(CircleShape).background(LocalAccentColor.current),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/**
 * FAB de captura rápida ("+ Movimiento", "+ Cuenta", "+ Presupuesto"...), unificado entre las 5
 * pantallas que lo usan. `Surface` a mano en vez de `ExtendedFloatingActionButton` de M3: el color
 * sólido de acento (primary/onPrimary, el mismo que el resto de los CTA de la app — no el
 * primaryContainer pálido por defecto), plano en reposo (regla Flat-By-Default, sin sombra), la
 * fuente display (Caprasimo, que Sage.dc.html usa también en los botones, no solo en los títulos
 * H1) a un tamaño bodyLarge (M3 solo da labelLarge/13sp para el texto del slot, sin forma de
 * agrandarlo), y un gap icono-texto de 6dp propio en vez del ~12dp fijo e inajustable del
 * ExtendedFloatingActionButton.
 */
@Composable
fun AddFab(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = PillShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 24.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(LucideIcons.Plus, null)
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                fontFamily = displayFontFamily(),
            )
        }
    }
}

/** Mensaje de validación bajo un campo. DESIGN.md: "Error state uses Warning Amber text + icon",
 * nunca solo color — el icono mantiene el error legible sin depender del tono. */
@Composable
fun FieldError(message: String, modifier: Modifier = Modifier) {
    val warning = LocalFinanceColors.current.warning
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs),
    ) {
        Icon(LucideIcons.AlertTriangle, contentDescription = null, tint = warning, modifier = Modifier.size(16.dp))
        Text(message, color = warning, style = MaterialTheme.typography.bodySmall)
    }
}

/** Colores del Switch para todos los ajustes. El default de M3 pinta borde y perilla apagados con
 * `outline`, que en la paleta Sage queda a ~1.2:1 sobre las tarjetas (WCAG 1.4.11 pide 3:1) y el
 * Switch apagado se volvía casi invisible. */
@Composable
fun sageSwitchColors(): SwitchColors = SwitchDefaults.colors(
    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
    uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
)

/** Estado vacío reutilizable: icono + título + subtítulo. [icon] es un `Painter` (LucideIcons) —
 * este chrome no es un glifo de categoría, así que siempre debe migrar al set Lucide. */
@Composable
fun EmptyState(
    icon: Painter,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md),
    ) {
        Surface(shape = PillShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.padding(LocalSpacing.current.lg).size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
