package com.finanzen.platform

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.db.FinanzenDb
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** La BD de escritorio de antes de la primera migración (user_version 0) sube a la versión actual sin perder datos. */
class DesktopDatabaseMigrationTest {
    private val dir = Files.createTempDirectory("finanzen-migration").toFile()

    @AfterTest
    fun cleanup() {
        dir.deleteRecursively()
    }

    @Test
    fun bdV1SinVersionSeMigraALaActual() {
        val dbFile = File(dir, "finanzen.db")
        // Esquema v1 congelado por SQLDelight; las BD reales de esa época quedaron con user_version = 0.
        File("src/commonMain/sqldelight/databases/1.db").copyTo(dbFile)
        JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}").use { driver ->
            driver.execute(null, "PRAGMA user_version = 0", 0)
            driver.execute(null, "INSERT INTO Setting(key, value) VALUES ('marca', 'antes')", 0)
            assertEquals(0L, driver.long("SELECT count(*) FROM sqlite_master WHERE name = 'FundSimulation'"))
        }

        openDesktopDatabase(dbFile).use { driver ->
            assertEquals(FinanzenDb.Schema.version, driver.long("PRAGMA user_version"))
            assertEquals(1L, driver.long("SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = 'FundSimulation'"))
            assertEquals(1L, driver.long("SELECT count(*) FROM Setting WHERE key = 'marca'"))
        }
    }

    @Test
    fun instalacionNuevaQuedaEnLaVersionActual() {
        val dbFile = File(dir, "nueva.db")
        openDesktopDatabase(dbFile).use { driver ->
            assertEquals(FinanzenDb.Schema.version, driver.long("PRAGMA user_version"))
            assertTrue(driver.long("SELECT count(*) FROM sqlite_master WHERE name = 'FundSimulation'") == 1L)
        }
    }

    private fun SqlDriver.long(sql: String): Long = executeQuery(
        identifier = null,
        sql = sql,
        mapper = { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L) },
        parameters = 0,
    ).value
}
