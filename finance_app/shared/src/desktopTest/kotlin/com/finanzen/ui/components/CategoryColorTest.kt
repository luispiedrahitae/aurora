package com.finanzen.ui.components

import androidx.compose.ui.graphics.toArgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun glyphForKeyResuelveGeneralMarcaYFallback() {
        // Clave general -> Vec; clave de marca -> Res.
        assertTrue(glyphForKey("restaurant") is CategoryGlyph.Vec)
        assertTrue(glyphForKey("brand_netflix") is CategoryGlyph.Res)
        // Clave desconocida o emoji legacy -> fallback genérico (Vec), nunca crash.
        assertEquals(glyphForKey("other"), glyphForKey("🍔"))
        assertEquals(glyphForKey("other"), glyphForKey("no_existe"))
    }

    @Test
    fun los5GruposGeneralesTienenAlMenos10IconosYSinClavesDuplicadas() {
        val generalTitles = listOf("Alimentación", "Transporte", "Hogar y servicios", "Ocio y entretenimiento", "Compras y otros")
        val general = iconGroups.filter { it.title in generalTitles }
        assertEquals(generalTitles.size, general.size)
        general.forEach { group -> assertTrue(group.icons.size >= 10, "${group.title} tiene ${group.icons.size} (<10)") }
        // Ninguna clave repetida en todo el catálogo.
        val keys = categoryIcons.map { it.first }
        assertEquals(keys.size, keys.toSet().size)
    }
}
