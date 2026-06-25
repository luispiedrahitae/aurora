package com.finanzen.platform

import app.cash.sqldelight.db.SqlDriver

expect val platformName: String

expect class DriverFactory {
    fun create(): SqlDriver
}

/**
 * Programa un recordatorio. Stub en todas las plataformas por ahora.
 * - Android: WorkManager + NotificationManager (TODO Phase 11/12)
 * - iOS: UNUserNotificationCenter (TODO)
 * - Desktop: solo log a consola (preview de desarrollo, no se distribuye)
 */
expect class NotificationScheduler {
    fun scheduleReminder(id: Long, title: String, body: String, atEpochDay: Long)
    fun cancel(id: Long)
}

/**
 * Escribe el contenido de un reporte al disco/store del SO. Devuelve la ruta o un mensaje de error.
 * - Desktop: ~/Downloads
 * - Android: TODO usar MediaStore (Phase de release Android)
 * - iOS: TODO usar UIActivityViewController + share sheet
 */
expect class ReportExporter {
    fun saveCsv(suggestedName: String, content: String): String
    fun savePdf(suggestedName: String, lines: List<String>): String
}

/**
 * Autenticación biométrica. Stub en todas las plataformas: el botón "Usar biometría" siempre devuelve false
 * en v1; cuando lleguemos a producción Android/iOS conectamos al BiometricPrompt / LocalAuthentication real.
 */
expect class BiometricAuth {
    /** true si el dispositivo soporta biometría y el usuario la tiene configurada. */
    fun isAvailable(): Boolean

    /** Pide al usuario autenticarse. Llama onResult(true) si OK. Por ahora siempre stub → false. */
    fun authenticate(reason: String, onResult: (Boolean) -> Unit)
}

/**
 * AES-GCM con clave derivada con PBKDF2-HMAC-SHA256 (100k iteraciones, 256 bits).
 * El envelope devuelto por [encrypt] es un JSON self-describing (salt + iv + ciphertext b64) y
 * puede ser leído por cualquier plataforma con la misma passphrase.
 */
expect class BackupCrypto {
    /** Devuelve el envelope JSON con todo lo necesario para descifrar (sal, IV, ciphertext). */
    fun encrypt(passphrase: String, plaintext: String): String

    /** Devuelve el texto descifrado o null si la passphrase es incorrecta / archivo manipulado. */
    fun decrypt(passphrase: String, envelope: String): String?
}

/**
 * Escribe archivos de backup en la plataforma (Android: MediaStore/Descargas; Desktop: ~/Downloads).
 * La lectura para importar se hace con [rememberBackupPicker], que entrega el contenido directamente.
 */
expect class BackupIO {
    fun writeBackup(filename: String, content: String): String
}
