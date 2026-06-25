package com.finanzen.platform

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private const val AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

@Composable
actual fun rememberBiometricUnlock(): BiometricUnlock {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val canAuth = remember(context) {
        BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    }
    return BiometricUnlock(
        available = canAuth && activity != null,
        authenticate = auth@{ onSuccess ->
            if (activity == null) return@auth
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onSuccess()
                    }
                },
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Desbloquear FinanZen")
                .setSubtitle("Usa tu huella o rostro")
                .setNegativeButtonText("Usar PIN")
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build()
            prompt.authenticate(info)
        },
    )
}
