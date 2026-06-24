package com.finanzen.security

import kotlin.random.Random

/** SHA-256 hex. Implementación por plataforma (java.security en JVM/Android, CommonCrypto en iOS). */
expect fun sha256Hex(input: String): String

/** Hash de PIN: 10.000 iteraciones de SHA-256 con sal. Comparable al PBKDF2 mínimo. */
object PinHasher {
    private const val ITERATIONS = 10_000

    fun hash(pin: String, salt: String): String {
        var current = sha256Hex(salt + pin)
        repeat(ITERATIONS - 1) { current = sha256Hex(current) }
        return current
    }

    fun verify(pin: String, salt: String, expectedHash: String): Boolean = hash(pin, salt) == expectedHash

    /** Genera una sal de 32 bytes en hex. Lo suficientemente único — v1 no requiere SecureRandom. */
    fun newSalt(): String {
        val bytes = ByteArray(32)
        Random.nextBytes(bytes)
        return bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    }
}
