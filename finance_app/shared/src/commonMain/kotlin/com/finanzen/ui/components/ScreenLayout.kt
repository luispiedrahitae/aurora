package com.finanzen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.finanzen.ui.theme.LocalSpacing

/**
 * Lista desplazable con el ritmo de página Sage: 16 de margen lateral, 8 arriba, 24 entre bloques y
 * [bottomPadding] abajo (más alto en pantallas con FAB para que el último elemento no quede tapado).
 */
@Composable
fun ScreenList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    gap: Dp = LocalSpacing.current.xl,
    bottomPadding: Dp = 24.dp,
    content: LazyListScope.() -> Unit,
) {
    val spacing = LocalSpacing.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content,
    )
}

/** Variante no perezosa de [ScreenList] para formularios y pantallas cortas. */
@Composable
fun ScreenColumn(modifier: Modifier = Modifier, gap: Dp = LocalSpacing.current.xl, content: @Composable ColumnScope.() -> Unit) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content,
    )
}

/** Alto extra al final de la lista cuando hay un FAB encima (56 del botón + 16 de margen + aire). */
val FabClearance: Dp = 96.dp
