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
        val vm = SettingsViewModel(SettingsRepository(db))
        assertEquals(ThemeMode.DARK, vm.theme.value)

        vm.setTheme(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, vm.theme.value)
        assertEquals(SettingsRepository.THEME_LIGHT, SettingsRepository(db).themeMode())
    }
}
