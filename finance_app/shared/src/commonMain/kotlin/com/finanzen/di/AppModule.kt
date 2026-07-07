package com.finanzen.di

import com.finanzen.data.AccountRepository
import com.finanzen.data.BudgetRepository
import com.finanzen.data.CardRepository
import com.finanzen.data.CategoryRepository
import com.finanzen.data.CurrencyRepository
import com.finanzen.data.InstallmentPlanRepository
import com.finanzen.data.RecurringExpenseRepository
import com.finanzen.data.SecurityRepository
import com.finanzen.data.SettingsRepository
import com.finanzen.data.SubscriptionRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.data.seedIfEmpty
import com.finanzen.db.FinanzenDb
import com.finanzen.platform.DriverFactory
import com.finanzen.viewmodel.AccountsViewModel
import com.finanzen.viewmodel.AnalysisViewModel
import com.finanzen.viewmodel.BackupViewModel
import com.finanzen.viewmodel.BudgetsViewModel
import com.finanzen.viewmodel.CategoriesViewModel
import com.finanzen.viewmodel.DashboardViewModel
import com.finanzen.viewmodel.ReportsViewModel
import com.finanzen.viewmodel.SecurityViewModel
import com.finanzen.viewmodel.SettingsViewModel
import com.finanzen.viewmodel.SubscriptionsViewModel
import com.finanzen.viewmodel.TransactionsViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun initKoin(extra: KoinAppDeclaration = {}) = startKoin {
    extra()
    modules(sharedModule)
}

val sharedModule: Module = module {
    single {
        val db = FinanzenDb(get<DriverFactory>().create())
        seedIfEmpty(db)
        db
    }
    single { TransactionRepository(get()) }
    single { AccountRepository(get()) }
    single { CategoryRepository(get()) }
    single { CurrencyRepository(get()) }
    single { CardRepository(get()) }
    single { InstallmentPlanRepository(get()) }
    single { SubscriptionRepository(get()) }
    single { RecurringExpenseRepository(get()) }
    single { SecurityRepository(get()) }
    single { SettingsRepository(get()) }
    single { BudgetRepository(get()) }

    viewModel { DashboardViewModel(get(), get(), get(), get()) }
    viewModel { TransactionsViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { CategoriesViewModel(get()) }
    viewModel { BudgetsViewModel(get(), get(), get()) }
    viewModel { AccountsViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { SubscriptionsViewModel(get(), get(), get(), get(), get()) }
    viewModel { AnalysisViewModel(get(), get()) }
    viewModel { ReportsViewModel(get(), get(), get(), get()) }
    viewModel { BackupViewModel(get(), get(), get()) }
    single { SecurityViewModel(get()) }
    single { SettingsViewModel(get(), get()) }
}
