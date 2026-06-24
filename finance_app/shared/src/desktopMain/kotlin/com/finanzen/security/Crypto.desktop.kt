package com.finanzen.security

import java.security.MessageDigest

actual fun sha256Hex(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.encodeToByteArray())
    return bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
}
