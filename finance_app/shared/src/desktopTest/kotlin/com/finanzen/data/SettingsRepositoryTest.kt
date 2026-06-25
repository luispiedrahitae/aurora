package com.finanzen.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.db.FinanzenDb
import com.finanzen.viewmodel.SettingsViewModel
import com.finanzen.viewmodel.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsRepositoryTest {
    private fun freshDb(): FinanzenDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanzenDb.Schema.create(driver)
        return FinanzenDb(driver)
    }

    @Test
    fun defaultEsSystemYPersisteElCambio() {
        val repo = SettingsRepository(freshDb())
        // sin valor guardado → default
        assertEquals(SettingsRepository.THEME_SYSTEM, repo.themeMode())

        repo.setThemeMode(SettingsRepository.THEME_DARK)
        assertEquals(SettingsRepository.THEME_DARK, repo.themeMode())
    }

    @Test
    fun viewModelParseaYExponeElTemaPersistido() {
        val db = freshDb()
        SettingsRepository(db).setThemeMode(SettingsRepository.THEME_DARK)

        // un VM nuevo (como tras reiniciar la app) arranca con el tema guardado
        val vm = SettingsViewModel(SettingsRepository(db), CurrencyRepository(db))
        assertEquals(ThemeMode.DARK, vm.theme.value)

        vm.setTheme(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, vm.theme.value)
        assertEquals(SettingsRepository.THEME_LIGHT, SettingsRepository(db).themeMode())
    }

    @Test
    fun setBaseCurrencyReEtiquetaCuentasYTransacciones() {
        val db = freshDb()
        db.currencyQueries.upsert("USD", "$", 2, 1.0)
        db.currencyQueries.upsert("COP", "$", 0, 1.0)
        val accountRepo = AccountRepository(db)
        val accId = accountRepo.add(name = "Efectivo", type = "CASH", currency = "USD")
        db.transactionQueries.insert(accId, null, 1000, "USD", 0, "", "EXPENSE", null, null)

        val repo = SettingsRepository(db)
        assertEquals(SettingsRepository.DEFAULT_CURRENCY, repo.baseCurrency())

        repo.setBaseCurrency("COP")

        assertEquals("COP", repo.baseCurrency())
        assertEquals("COP", accountRepo.all().single().currency)
        assertEquals("COP", db.transactionQueries.selectAll().executeAsList().single().currency)
    }
}
