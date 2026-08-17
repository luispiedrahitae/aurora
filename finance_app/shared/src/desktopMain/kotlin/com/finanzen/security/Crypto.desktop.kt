package com.finanzen.security

import java.security.MessageDigest
import java.security.SecureRandom

private val HEX_CHARS = "0123456789abcdef".toCharArray()

private fun ByteArray.toHex(): String {
    val out = CharArray(size * 2)
    for (i in indices) {
        val v = this[i].toInt() and 0xFF
        out[i * 2] = HEX_CHARS[v ushr 4]
        out[i * 2 + 1] = HEX_CHARS[v and 0x0F]
    }
    return String(out)
}

actual fun sha256Hex(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.encodeToByteArray())
    return bytes.toHex()
}

actual fun secureRandomBytes(size: Int): ByteArray = ByteArray(size).also { SecureRandom().nextBytes(it) }

actual fun sha256HexIterated(seed: String, iterations: Int): String {
    val digest = MessageDigest.getInstance("SHA-256")
    var current = seed.encodeToByteArray()
    var hex = ""
    repeat(iterations) {
        hex = digest.digest(current).toHex()
        current = hex.encodeToByteArray()
    }
    return hex
}
