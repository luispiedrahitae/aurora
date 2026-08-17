@file:OptIn(ExperimentalForeignApi::class)

package com.finanzen.security

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault

actual fun sha256Hex(input: String): String = memScoped {
    val data = input.encodeToByteArray()
    val digest = allocArray<kotlinx.cinterop.UByteVar>(CC_SHA256_DIGEST_LENGTH)
    CC_SHA256(data.refTo(0), data.size.convert(), digest)
    buildString {
        for (i in 0 until CC_SHA256_DIGEST_LENGTH) {
            val b = digest[i].toInt() and 0xFF
            append(b.toString(16).padStart(2, '0'))
        }
    }
}

actual fun secureRandomBytes(size: Int): ByteArray {
    val bytes = ByteArray(size)
    bytes.usePinned { SecRandomCopyBytes(kSecRandomDefault, size.convert(), it.addressOf(0)) }
    return bytes
}

private val HEX_CHARS = "0123456789abcdef".toCharArray()

actual fun sha256HexIterated(seed: String, iterations: Int): String = memScoped {
    val digestBuf = allocArray<kotlinx.cinterop.UByteVar>(CC_SHA256_DIGEST_LENGTH)
    var current = seed.encodeToByteArray()
    var hex = ""
    repeat(iterations) {
        CC_SHA256(current.refTo(0), current.size.convert(), digestBuf)
        val out = CharArray(CC_SHA256_DIGEST_LENGTH * 2)
        for (i in 0 until CC_SHA256_DIGEST_LENGTH) {
            val v = digestBuf[i].toInt() and 0xFF
            out[i * 2] = HEX_CHARS[v ushr 4]
            out[i * 2 + 1] = HEX_CHARS[v and 0x0F]
        }
        hex = String(out)
        current = hex.encodeToByteArray()
    }
    hex
}
