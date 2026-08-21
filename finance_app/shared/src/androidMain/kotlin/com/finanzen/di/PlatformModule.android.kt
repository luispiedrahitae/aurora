package com.finanzen.di

import com.finanzen.platform.BackupCrypto
import com.finanzen.platform.BackupIO
import com.finanzen.platform.DriverFactory
import com.finanzen.platform.NotificationScheduler
import com.finanzen.platform.ReportExporter
import org.koin.core.module.Module
import org.koin.dsl.module

val platformModule: Module = module {
    single { DriverFactory(get()) }
    single { NotificationScheduler(get()) }
    single { ReportExporter(get()) }
    single { BackupCrypto() }
    single { BackupIO(get()) }
}
