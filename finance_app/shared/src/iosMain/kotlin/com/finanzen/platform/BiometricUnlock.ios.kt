package com.finanzen.platform

import androidx.compose.runtime.Composable

// ponytail: stub iOS. Conectar a LocalAuthentication (LAContext) en el track iOS.
@Composable
actual fun rememberBiometricUnlock(): BiometricUnlock = BiometricUnlock(available = false, authenticate = {})
