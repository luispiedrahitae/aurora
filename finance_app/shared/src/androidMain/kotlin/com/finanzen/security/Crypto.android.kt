package com.finanzen.security

import java.security.MessageDigest
import java.security.SecureRandom

actual fun sha256Hex(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.encodeToByteArray())
    return bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
}

actual fun secureRandomBytes(size: Int): ByteArray = ByteArray(size).also { SecureRandom().nextBytes(it) }
