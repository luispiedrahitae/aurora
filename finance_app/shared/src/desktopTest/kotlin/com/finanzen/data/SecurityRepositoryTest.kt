package com.finanzen.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.db.FinanzenDb
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecurityRepositoryTest {
    private fun freshDb(): FinanzenDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanzenDb.Schema.create(driver)
        return FinanzenDb(driver)
    }

    @Test
    fun cincoIntentosFallidosBloqueanElPin() = runTest {
        // FIX (hallazgo alto #6): antes no había límite de intentos; ahora el 5º fallo seguido
        // bloquea, y el PIN correcto tampoco pasa mientras el bloqueo esté activo.
        val repo = SecurityRepository(freshDb())
        repo.enableLockWithPin("1234")
        val now = 0L

        repeat(4) { assertFalse(repo.verifyPin("0000", now)) }
        assertEquals(0L, repo.lockedUntilMs()) // aún no llega al 5º intento

        assertFalse(repo.verifyPin("0000", now)) // 5º fallo: dispara el bloqueo
        assertTrue(repo.lockedUntilMs() > now)

        // Aunque el PIN sea correcto, mientras dure el bloqueo se rechaza sin verificar.
        assertFalse(repo.verifyPin("1234", now))
    }

    @Test
    fun elBloqueoExpiraYUnPinCorrectoReseteaLosIntentos() = runTest {
        val repo = SecurityRepository(freshDb())
        repo.enableLockWithPin("1234")
        repeat(5) { repo.verifyPin("0000", 0L) }
        val lockedUntil = repo.lockedUntilMs()
        assertTrue(lockedUntil > 0L)

        // Después de que expira el bloqueo, el PIN correcto funciona y limpia el estado de intentos.
        assertTrue(repo.verifyPin("1234", lockedUntil))
        assertEquals(0L, repo.lockedUntilMs())
    }
}
