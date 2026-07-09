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
        db.currencyQueries.upsert("USD", "$", 2, 1.0, "US Dollar", ".", ",")
        db.currencyQueries.upsert("COP", "$", 0, 1.0, "Colombian Peso", ",", ".")
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

    @Test
    fun setBaseCurrencyReescalaMontosDeMenosAMasDecimales() {
        // COP (0 decimales) -> USD (2 decimales): todo se multiplica por 100 para que el número que
        // el usuario ve se mantenga igual (40.000 sigue siendo 40.000, ahora en USD).
        val db = freshDb()
        db.currencyQueries.upsert("COP", "$", 0, 1.0, "Colombian Peso", ",", ".")
        db.currencyQueries.upsert("USD", "$", 2, 1.0, "US Dollar", ".", ",")
        db.settingQueries.put(SettingsRepository.KEY_CURRENCY, "COP")

        val accountRepo = AccountRepository(db)
        val accId = accountRepo.add(name = "Efectivo", type = "CREDIT", currency = "COP", openingBalanceMinor = 40000)
        db.transactionQueries.insert(accId, null, 5000, "COP", 0, "", "EXPENSE", null, null)
        db.categoryQueries.insert(parentId = null, name = "Gastos", icon = "", color = 0, kind = "EXPENSE")
        val categoryId = db.categoryQueries.selectAll().executeAsList().single().id
        db.budgetQueries.upsert(categoryId = categoryId, periodMonth = 202607, limitMinor = 100000)
        db.cardQueries.insert(accId, "", "OTRA", 200000, null, null, null)
        val cardId = db.cardQueries.selectByAccount(accId).executeAsList().single().id
        db.installmentPlanQueries.insert(cardId, null, 300000, 3, 0.0, 0, "", 0)

        SettingsRepository(db).setBaseCurrency("USD")

        assertEquals(4000000, accountRepo.all().single().openingBalanceMinor)
        assertEquals(500000, db.transactionQueries.selectAll().executeAsList().single().amountMinor)
        assertEquals(10000000, db.budgetQueries.selectAll().executeAsList().single().limitMinor)
        assertEquals(20000000, db.cardQueries.selectById(cardId).executeAsOne().creditLimitMinor)
        assertEquals(30000000, db.installmentPlanQueries.selectAll().executeAsList().single().totalAmountMinor)
    }

    @Test
    fun setBaseCurrencyReescalaMontosDeMasAMenosDecimales() {
        // USD (2 decimales) -> COP (0 decimales): se divide entre 100 (trunca residuo de centavos).
        val db = freshDb()
        db.currencyQueries.upsert("USD", "$", 2, 1.0, "US Dollar", ".", ",")
        db.currencyQueries.upsert("COP", "$", 0, 1.0, "Colombian Peso", ",", ".")
        db.settingQueries.put(SettingsRepository.KEY_CURRENCY, "USD")

        val accountRepo = AccountRepository(db)
        accountRepo.add(name = "Efectivo", type = "CASH", currency = "USD", openingBalanceMinor = 100000)

        SettingsRepository(db).setBaseCurrency("COP")

        assertEquals(1000, accountRepo.all().single().openingBalanceMinor)
    }

    @Test
    fun setBaseCurrencyConLaMismaMonedaNoAlteraNada() {
        val db = freshDb()
        db.currencyQueries.upsert("USD", "$", 2, 1.0, "US Dollar", ".", ",")
        db.settingQueries.put(SettingsRepository.KEY_CURRENCY, "USD")
        val accountRepo = AccountRepository(db)
        accountRepo.add(name = "Efectivo", type = "CASH", currency = "USD", openingBalanceMinor = 12345)

        SettingsRepository(db).setBaseCurrency("USD")

        assertEquals(12345, accountRepo.all().single().openingBalanceMinor)
    }

    @Test
    fun symbolPositionDefaultEsAuto() {
        val repo = SettingsRepository(freshDb())
        // sin valor guardado → "auto" (CLDR por moneda), ya no "prefix" fijo.
        assertEquals("auto", repo.symbolPosition())

        repo.setSymbolPosition("suffix")
        assertEquals("suffix", repo.symbolPosition())
    }

    @Test
    fun savingsGoalPctDefaultEs20YPersisteElCambio() {
        val repo = SettingsRepository(freshDb())
        assertEquals(20L, repo.savingsGoalPct())

        repo.setSavingsGoalPct(35L)
        assertEquals(35L, repo.savingsGoalPct())
    }
}
