package com.finanzen.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.db.FinanzenDb
import com.finanzen.platform.NotificationScheduler
import com.finanzen.viewmodel.AccountsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountRepositoryTest {
    private fun freshDb(): FinanzenDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanzenDb.Schema.create(driver)
        return FinanzenDb(driver)
    }

    @Test
    fun updateBasicsCambiaNombreYMontoInicialSinTocarElTipo() {
        val db = freshDb()
        db.currencyQueries.upsert("USD", "$", 2, 1.0, "US Dollar", ".", ",")
        val repo = AccountRepository(db)
        val id = repo.add(name = "Efectivo", type = "CASH", currency = "USD", openingBalanceMinor = 500)

        repo.updateBasics(id, "Billetera", 1000)

        val account = repo.all().single { it.id == id }
        assertEquals("Billetera", account.name)
        assertEquals("CASH", account.type)
        assertEquals(1000, account.openingBalanceMinor)
    }

    // ---- Edge cases QA (ver reporte de hallazgos) ----

    @Test
    fun deleteCuentaBorraSusTransaccionesYPlanesDeCuotas() {
        // FIX (hallazgo crítico #3): el diálogo de confirmación en AccountsTabScreen.kt promete
        // "Se eliminarán también sus movimientos y planes de cuotas asociados" — AccountsViewModel.
        // delete(id) ahora sí borra las transacciones de la cuenta (y los planes de cuotas de su
        // tarjeta, si es de crédito) antes de borrar la Card y la cuenta misma.
        val db = freshDb()
        db.currencyQueries.upsert("USD", "$", 2, 1.0, "US Dollar", ".", ",")
        val accountRepo = AccountRepository(db)
        val txRepo = TransactionRepository(db)
        val cardRepo = CardRepository(db)
        val planRepo = InstallmentPlanRepository(db)
        val vm = AccountsViewModel(accountRepo, txRepo, cardRepo, planRepo, SettingsRepository(db), NotificationScheduler())
        val accId = accountRepo.add(name = "Efectivo", type = "CASH", currency = "USD")
        val catId = CategoryRepository(db).addAndGetId("Comida", "EXPENSE", null)
        txRepo.add(accountId = accId, categoryId = catId, amountMinor = 1_000, currency = "USD", epochDay = 0, note = "super", kind = "EXPENSE")

        vm.delete(accId)

        assertEquals(0, accountRepo.all().size) // la cuenta desapareció
        assertEquals(0, txRepo.all().size) // y su transacción, con ella
    }

    @Test
    fun addDevuelveElIdDeLaCuentaNuevaAunqueNoOrdeneUltimaAlfabeticamente() {
        // FIX (hallazgo crítico): add() resolvía el id nuevo con selectAll().last(), pero selectAll
        // ordena por name -- si la cuenta nueva no ordena última alfabéticamente, .last() devolvía
        // el id de OTRA cuenta ya existente. "Ahorros" ordena antes que "Efectivo"/"Tarjeta...".
        val db = freshDb()
        db.currencyQueries.upsert("USD", "$", 2, 1.0, "US Dollar", ".", ",")
        val repo = AccountRepository(db)
        repo.add(name = "Efectivo", type = "CASH", currency = "USD")
        repo.add(name = "Tarjeta de Débito", type = "DEBIT", currency = "USD")
        val savingsId = repo.add(name = "Ahorros", type = "SAVINGS", currency = "USD")

        val savings = repo.all().single { it.id == savingsId }
        assertEquals("Ahorros", savings.name)
        assertEquals("SAVINGS", savings.type)
    }

    @Test
    fun deleteCuentaDeCreditoBorraPlanesDeCuotasDeSuTarjeta() {
        val db = freshDb()
        db.currencyQueries.upsert("USD", "$", 2, 1.0, "US Dollar", ".", ",")
        val accountRepo = AccountRepository(db)
        val txRepo = TransactionRepository(db)
        val cardRepo = CardRepository(db)
        val planRepo = InstallmentPlanRepository(db)
        val vm = AccountsViewModel(accountRepo, txRepo, cardRepo, planRepo, SettingsRepository(db), NotificationScheduler())
        val accId = accountRepo.add(name = "Tarjeta", type = "CREDIT", currency = "USD")
        cardRepo.add(accountId = accId, last4 = "1234", network = "OTRA", creditLimitMinor = 100_000, cutoffDay = null, dueDay = null)
        val cardId = cardRepo.byAccount(accId)!!.id
        val catId = CategoryRepository(db).addAndGetId("Compras", "EXPENSE", null)
        planRepo.add(cardId = cardId, categoryId = catId, totalAmountMinor = 30_000, installments = 3, interestRate = 0.0, startDateEpochDay = 0, description = "compra")

        vm.delete(accId)

        assertEquals(0, accountRepo.all().size)
        assertEquals(0, db.installmentPlanQueries.selectAll().executeAsList().size)
    }
}
