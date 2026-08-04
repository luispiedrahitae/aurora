package com.finanzen.viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.data.AccountRepository
import com.finanzen.data.CardRepository
import com.finanzen.data.InstallmentPlanRepository
import com.finanzen.data.SettingsRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.data.seedIfEmpty
import com.finanzen.db.FinanzenDb
import com.finanzen.platform.NotificationScheduler
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountsViewModelTest {
    private fun freshDb(): FinanzenDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanzenDb.Schema.create(driver)
        return FinanzenDb(driver)
    }

    private fun buildVm(db: FinanzenDb) = AccountsViewModel(
        AccountRepository(db),
        TransactionRepository(db),
        CardRepository(db),
        InstallmentPlanRepository(db),
        SettingsRepository(db),
        NotificationScheduler(),
    )

    @Test
    fun addAccountConMontoInicialLoRegistraComoMovimientoAdjustmentNoComoSeedCrudo() {
        val db = freshDb()
        seedIfEmpty(db)
        val vm = buildVm(db)

        val id = vm.addAccount(type = "CASH", name = "Ahorros", amountMinor = 50_000)

        val account = db.accountQueries.selectAll().executeAsList().first { it.id == id }
        assertEquals(0L, account.openingBalanceMinor) // el monto ya no vive en la columna cruda

        val txs = db.transactionQueries.selectAll().executeAsList()
        assertEquals(1, txs.size)
        assertEquals("ADJUSTMENT", txs.first().kind)
        assertEquals(50_000L, txs.first().amountMinor)
        assertEquals("Saldo inicial", txs.first().note)

        val balances = AccountsViewModel.computeBalances(listOf(account), txs)
        assertEquals(50_000L, balances[id])
    }

    @Test
    fun addAccountConMontoCeroNoCreaMovimiento() {
        val db = freshDb()
        seedIfEmpty(db)
        val vm = buildVm(db)

        vm.addAccount(type = "CASH", name = "Efectivo", amountMinor = 0)

        assertEquals(0, db.transactionQueries.selectAll().executeAsList().size)
    }

    @Test
    fun updateAccountRegistraSoloElDeltaComoNuevoAjusteYElSaldoCierra() {
        val db = freshDb()
        seedIfEmpty(db)
        val vm = buildVm(db)
        val id = vm.addAccount(type = "CASH", name = "Ahorros", amountMinor = 50_000)!!

        vm.updateAccount(id, "Ahorros", 70_000)

        val txs = db.transactionQueries.selectAll().executeAsList()
        assertEquals(2, txs.size) // el "Saldo inicial" original + el nuevo ajuste
        val adjustment = txs.first { it.note == "Ajuste de saldo" }
        assertEquals(20_000L, adjustment.amountMinor) // delta: 70000 - 50000

        val account = db.accountQueries.selectAll().executeAsList().first { it.id == id }
        val balances = AccountsViewModel.computeBalances(listOf(account), txs)
        assertEquals(70_000L, balances[id])
    }

    @Test
    fun updateAccountConMontoDisminuidoRegistraAjusteNegativo() {
        val db = freshDb()
        seedIfEmpty(db)
        val vm = buildVm(db)
        val id = vm.addAccount(type = "CASH", name = "Ahorros", amountMinor = 50_000)!!

        vm.updateAccount(id, "Ahorros", 30_000)

        val adjustment = db.transactionQueries.selectAll().executeAsList().first { it.note == "Ajuste de saldo" }
        assertEquals(-20_000L, adjustment.amountMinor)
    }
}
