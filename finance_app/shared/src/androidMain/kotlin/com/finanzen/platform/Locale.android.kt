package com.finanzen.platform

import java.util.Locale

actual fun systemCountryCode(): String = Locale.getDefault().country
