package com.finanzen.platform

import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale

// ponytail: implementación real (no stub) pero no verificable desde Windows — confirmar en Mac/CI
// que NSLocale.currentLocale.countryCode viene poblado en dispositivos reales.
actual fun systemCountryCode(): String = NSLocale.currentLocale.countryCode ?: ""
