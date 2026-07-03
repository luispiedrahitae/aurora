package com.finanzen.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.db.FinanzenDb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Verifica la heurística de import: un snapshot plano parsea; un envelope cifrado no. */
class BackupFormatTest {
    private fun freshDb(): FinanzenDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanzenDb.Schema.create(driver)
        return FinanzenDb(driver)
    }

    @Test
    fun snapshotPlanoParseaYEnvelopeCifradoNo() {
        val db = freshDb()
        seedIfEmpty(db)
        val plain = BackupSerializer.toJson(BackupSerializer.snapshotOf(db, 0))
        // El JSON plano se reconoce como snapshot (rama sin passphrase del import).
        assertNotNull(runCatching { BackupSerializer.fromJson(plain) }.getOrNull())
        // Un envelope cifrado (claves s/i/c) NO parsea como snapshot → fuerza la rama de descifrado.
        val envelope = """{"s":"00","i":"11","c":"deadbeef"}"""
        assertNull(runCatching { BackupSerializer.fromJson(envelope) }.getOrNull())
    }

    @Test
    fun restauraBackupViejoSinCamposDeMonedaNuevos() {
        // JSON de un backup anterior a agregar name/decimalSeparator/groupSeparator a CurrencyDto:
        // solo trae los 4 campos originales. ignoreUnknownKeys no ayuda aquí (esas claves faltan,
        // no sobran) - lo que hace que esto decodifique son los defaults de CurrencyDto.
        val oldFormatJson = """
            {
                "version": 1,
                "createdAt": 0,
                "currencies": [{"code": "USD", "symbol": "${'$'}", "decimals": 2, "rateToBase": 1.0}],
                "accounts": [], "cards": [], "categories": [], "transactions": [],
                "installmentPlans": [], "subscriptions": [], "recurringExpenses": [],
                "budgets": [], "settings": []
            }
        """.trimIndent()

        val snapshot = BackupSerializer.fromJson(oldFormatJson)
        assertEquals(1, snapshot.currencies.size)
        val usd = snapshot.currencies.first()
        assertEquals("USD", usd.code)
        assertEquals("", usd.name)
        assertEquals(",", usd.decimalSeparator)
        assertEquals(".", usd.groupSeparator)

        // El round-trip completo (restore a una DB real) tampoco debe fallar.
        val db = freshDb()
        BackupSerializer.restore(db, snapshot)
        assertEquals("USD", db.currencyQueries.selectByCode("USD").executeAsOne().code)
    }
}
