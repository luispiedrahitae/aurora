package com.finanzen.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.finanzen.App

// FragmentActivity (no ComponentActivity) porque androidx.biometric.BiometricPrompt lo exige.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        // En navegación de 3 botones, Android superpone un scrim gris genérico sobre la barra de
        // navegación del sistema para legibilidad — no el tema de la app (Apariencia > Tema/Acento).
        // Lo desactivamos para que se vea el contenedor real de NavigationBar (Compose) detrás.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            App()
        }
    }

    // Android 13+ exige permiso en runtime para mostrar notificaciones de recordatorios.
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFICATIONS)
        }
    }

    private companion object {
        const val REQ_NOTIFICATIONS = 1001
    }
}
