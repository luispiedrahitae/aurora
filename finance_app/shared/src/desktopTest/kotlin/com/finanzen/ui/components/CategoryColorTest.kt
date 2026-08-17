package com.finanzen.ui.components

import androidx.compose.ui.graphics.toArgb
import com.finanzen.ui.theme.LightCategoryColors
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
        val chosen = LightCategoryColors[3]
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
    fun gruposEsperadosTamanoMinimoYSinClavesDuplicadas() {
        val core = listOf(
            "Alimentación", "Transporte", "Hogar y servicios", "Ocio y entretenimiento", "Compras y otros",
            "Streaming", "Movilidad", "Google", "Estudio", "Videojuegos",
        )
        val nuevos = listOf("Finanzas y pagos", "Compras y viajes", "Domicilios y comida", "Redes sociales")
        assertEquals(core + nuevos, iconGroups.map { it.title })
        // Los 10 grupos base mantienen >=10; los nuevos solo deben existir y no estar vacíos.
        val byTitle = iconGroups.associate { it.title to it.icons.size }
        core.forEach { assertTrue(byTitle.getValue(it) >= 10, "$it tiene ${byTitle[it]} (<10)") }
        nuevos.forEach { assertTrue(byTitle.getValue(it) >= 1, "$it vacío") }
        // Ninguna clave repetida en todo el catálogo.
        val keys = categoryIcons.map { it.first }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun gruposDeMarcaEmpiezanConIconoGenerico() {
        val marca = listOf("Streaming", "Movilidad", "Finanzas y pagos", "Compras y viajes", "Domicilios y comida", "Redes sociales")
        val byTitle = iconGroups.associateBy { it.title }
        marca.forEach { title ->
            val first = byTitle.getValue(title).icons.first()
            assertTrue(first.second is CategoryGlyph.Vec, "$title no empieza con genérico")
        }
    }
}
