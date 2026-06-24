package com.finanzen

import androidx.compose.ui.window.ComposeUIViewController
import com.finanzen.di.initKoin
import com.finanzen.di.platformModule
import org.koin.core.context.GlobalContext

@Suppress("unused", "FunctionName")
fun MainViewController() = ComposeUIViewController {
    if (GlobalContext.getOrNull() == null) {
        initKoin { modules(platformModule) }
    }
    App()
}
