package com.finanzen.security

import com.finanzen.data.EncryptedEnvelope
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Misma implementación que [com.finanzen.security.JvmAesGcm] en desktopMain.
 * Duplicada porque KMP no comparte sourceSets entre `androidMain` y `jvm("desktop")` por defecto.
 * Si esto crece, mover a `jvmMain` con `KotlinSourceSetTree` o un `commonJvmMain` hierarchical.
 */
@OptIn(ExperimentalEncodingApi::class)
object AndroidAesGcm {
    // OWASP 2023 recomienda ≥600k para PBKDF2-HMAC-SHA256.
    // Las iteraciones van versionadas en el envelope (EncryptedEnvelope.it), así que subir este
    // valor a futuro no rompe el descifrado de backups ya creados.
    private const val ITERATIONS = 600_000
    private const val KEY_LEN_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val SALT_LEN = 16
    private const val IV_LEN = 12

    private val json = Json { ignoreUnknownKeys = true }

    fun encrypt(passphrase: String, plaintext: String): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LEN).also(random::nextBytes)
        val iv = ByteArray(IV_LEN).also(random::nextBytes)
        val key = deriveKey(passphrase, salt, ITERATIONS)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        val ct = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val env = EncryptedEnvelope(s = salt.toHex(), i = iv.toHex(), c = Base64.encode(ct), it = ITERATIONS)
        return json.encodeToString(EncryptedEnvelope.serializer(), env)
    }

    fun decrypt(passphrase: String, envelope: String): String? = runCatching {
        val env = json.decodeFromString(EncryptedEnvelope.serializer(), envelope)
        val salt = env.s.fromHex()
        val iv = env.i.fromHex()
        val ct = Base64.decode(env.c)
        val key = deriveKey(passphrase, salt, env.it)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        cipher.doFinal(ct).toString(Charsets.UTF_8)
    }.getOrNull()

    private fun deriveKey(passphrase: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, iterations, KEY_LEN_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun ByteArray.toHex(): String = joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    private fun String.fromHex(): ByteArray = ByteArray(length / 2) { i ->
        ((Character.digit(this[i * 2], 16) shl 4) + Character.digit(this[i * 2 + 1], 16)).toByte()
    }
}
