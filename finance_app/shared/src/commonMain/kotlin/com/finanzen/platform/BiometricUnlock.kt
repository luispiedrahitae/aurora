package com.finanzen.platform

import androidx.compose.runtime.Composable

/**
 * Desbloqueo biométrico para la pantalla de bloqueo. Es un atajo sobre el PIN (que sigue siendo
 * la credencial primaria), así que [authenticate] solo confirma identidad vía el SO.
 * - Android: BiometricPrompt (huella/rostro). [available] true si hay biometría enrolada.
 * - Desktop/iOS: [available] = false por ahora.
 */
class BiometricUnlock(
    val available: Boolean,
    val authenticate: (onSuccess: () -> Unit) -> Unit,
)

@Composable
expect fun rememberBiometricUnlock(): BiometricUnlock
