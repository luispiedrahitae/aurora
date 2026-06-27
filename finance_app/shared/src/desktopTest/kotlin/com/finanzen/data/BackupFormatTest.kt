package com.finanzen.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.db.FinanzenDb
import kotlin.test.Test
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
}
