package com.finanzen.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JvmAesGcmTest {
    @Test
    fun encryptaYDescifraConLaPassphraseCorrecta() {
        val envelope = JvmAesGcm.encrypt("correct horse battery staple", "hola mundo")
        assertEquals("hola mundo", JvmAesGcm.decrypt("correct horse battery staple", envelope))
    }

    @Test
    fun decryptDevuelveNullConPassphraseIncorrecta() {
        val envelope = JvmAesGcm.encrypt("passphrase-correcta", "dato secreto")
        assertNull(JvmAesGcm.decrypt("passphrase-incorrecta", envelope))
    }

    @Test
    fun envelopeSinCampoItSigueDescifrandoConElDefault() {
        // Formato "viejo": generado antes de versionar las iteraciones PBKDF2 en el envelope.
        // Sin el campo `it`, debe caer al default (600_000), que coincide con ITERATIONS.
        val fresh = JvmAesGcm.encrypt("mi-passphrase", "contenido de backup")
        val oldFormatEnvelope = fresh.replace(Regex(""","it":\d+"""), "")

        assertEquals("contenido de backup", JvmAesGcm.decrypt("mi-passphrase", oldFormatEnvelope))
    }
}
