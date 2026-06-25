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
