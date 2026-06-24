package com.finanzen.android

import android.app.Application
import com.finanzen.di.initKoin
import com.finanzen.di.platformModule
import org.koin.android.ext.koin.androidContext

class FinanZenApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@FinanZenApplication)
            modules(platformModule)
        }
    }
}
