package com.finanzen.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.finanzen.App
import com.finanzen.di.initKoin
import com.finanzen.di.platformModule

fun main() {
    initKoin { modules(platformModule) }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "FinanZen — Preview",
            state = rememberWindowState(width = 412.dp, height = 892.dp),
        ) {
            App()
        }
    }
}
