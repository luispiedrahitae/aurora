package com.finanzen.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.finanzen.ui.theme.SageMotion
import com.finanzen.ui.theme.motionTween

/** Contenido plegable de una tarjeta (día en Movimientos, cuenta, inversión): crece/encoge junto con la
 * tarjeta en vez de aparecer de golpe, así el pie no se ve antes que el resto. Respeta "reducir animaciones". */
@Composable
fun ExpandableSection(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(motionTween(SageMotion.MEDIUM)) + fadeIn(motionTween(SageMotion.MEDIUM)),
        exit = shrinkVertically(motionTween(SageMotion.SHORT)) + fadeOut(motionTween(SageMotion.SHORT)),
    ) {
        Column { content() }
    }
}

/** Montos de la cabecera que se ocultan al abrir la tarjeta: solo se desvanecen, sin cambiar de tamaño. */
@Composable
fun FadeSection(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(motionTween(SageMotion.SHORT)), exit = fadeOut(motionTween(SageMotion.FAST))) {
        content()
    }
}
