package com.finanzen.platform

import androidx.compose.runtime.Composable

// Desktop preview: no hay biometría.
@Composable
actual fun rememberBiometricUnlock(): BiometricUnlock = BiometricUnlock(available = false, authenticate = {})
