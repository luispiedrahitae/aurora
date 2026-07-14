package com.finanzen.ui.theme

import android.os.Build

actual fun platformBlurSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
