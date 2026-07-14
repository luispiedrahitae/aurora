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

    // ---- Edge cases QA (ver reporte de hallazgos) ----

    @Test
    fun restoreRemapeaIdsYPreservaReferenciasCruzadasEnRestoresSucesivos() {
        // FIX (hallazgo crítico #4): TODAS las tablas declaran `id INTEGER PRIMARY KEY AUTOINCREMENT`,
        // y SQLite nunca reutiliza un id (lo trackea en sqlite_sequence), así que cada restore asigna
        // ids nuevos. restore() ahora captura el id nuevo de cada fila padre (accountIdMap,
        // subscriptionIdMap...) y traduce las FKs de las filas hijas antes de insertarlas, así que la
        // referencia sobrevive sin importar cuántas veces se restaure el mismo snapshot.
        val db = freshDb()
        db.currencyQueries.upsert("USD", "$", 2, 1.0, "US Dollar", ".", ",")
        val accountRepo = AccountRepository(db)
        val accId = accountRepo.add(name = "Efectivo", type = "CASH", currency = "USD")
        db.subscriptionQueries.insert("Streaming", 1_500, "USD", null, accId, "MONTHLY", 15, 0, 3, 1)
        val subscription = db.subscriptionQueries.selectAll().executeAsList().first()
        db.transactionQueries.insert(accId, null, 1_500, "USD", 0, "cargo streaming", "EXPENSE", null, null, null, subscription.id)

        val snapshot = BackupSerializer.snapshotOf(db, 0)
        BackupSerializer.restore(db, snapshot) // primer restore
        BackupSerializer.restore(db, snapshot) // segundo restore del MISMO snapshot: el contador ya avanzó

        val restoredSub = db.subscriptionQueries.selectAll().executeAsList().single()
        val restoredTx = db.transactionQueries.selectAll().executeAsList().single()
        // Aunque el id real de la suscripción ya no coincide con el id=1 original del snapshot, la
        // transacción sigue apuntando a la fila correcta gracias al remapeo.
        assertEquals(restoredSub.id, restoredTx.subscriptionId)
    }

    @Test
    fun restoreConSnapshotDeVersionFuturaNoLoRechaza() {
        // BackupSnapshot.version existe pero nunca se lee/valida en restore() — es decorativo. Un
        // backup marcado con una versión futura (con el set de campos actual) importa igual, sin
        // ningún aviso de incompatibilidad.
        val db = freshDb()
        seedIfEmpty(db)
        val snapshot = BackupSerializer.snapshotOf(db, 0).copy(version = 99)

        BackupSerializer.restore(db, snapshot)

        assertEquals(true, db.currencyQueries.selectAll().executeAsList().isNotEmpty())
    }
}
