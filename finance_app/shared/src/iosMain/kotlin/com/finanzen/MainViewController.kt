package com.finanzen

import androidx.compose.ui.window.ComposeUIViewController
import com.finanzen.di.initKoin
import com.finanzen.di.platformModule
import org.koin.mp.KoinPlatform

@Suppress("unused", "FunctionName")
fun MainViewController() = ComposeUIViewController {
    if (KoinPlatform.getKoinOrNull() == null) {
        initKoin { modules(platformModule) }
    }
    App()
}
