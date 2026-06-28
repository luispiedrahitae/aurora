package com.finanzen.ui.components

import androidx.compose.ui.graphics.toArgb
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryColorTest {
    @Test
    fun sinColorPropioUsaElHashYEsEstable() {
        // color = 0 -> fallback por hash del nombre; mismo nombre, mismo color siempre.
        assertEquals(colorForCategory("Alimentación"), categoryColor("Alimentación", 0))
        assertEquals(categoryColor("Ocio", 0), categoryColor("Ocio", 0))
    }

    @Test
    fun colorGuardadoSeRespetaYHaceRoundTrip() {
        val chosen = categoryColors[3]
        val stored = chosen.toArgb().toLong()
        assertEquals(chosen, categoryColor("loQueSea", stored))
    }
}
