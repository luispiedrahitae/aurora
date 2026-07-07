package com.finanzen.viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.data.AccountRepository
import com.finanzen.data.CardRepository
import com.finanzen.data.InstallmentPlanRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.data.seedIfEmpty
import com.finanzen.db.FinanzenDb
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountsMovementsTest {
    private fun freshDb(): FinanzenDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanzenDb.Schema.create(driver)
        return FinanzenDb(driver)
    }

    @Test
    fun groupByAccountAgrupaMovimientosPorCuenta() {
        val db = freshDb()
        seedIfEmpty(db)
        val accountRepo = AccountRepository(db)
        val a = accountRepo.all().first()
        accountRepo.add("Zetacuenta", "SAVINGS", a.currency, openingBalanceMinor = 0)
        val b = accountRepo.all().first { it.name == "Zetacuenta" }.id

        val tx = TransactionRepository(db)
        tx.add(a.id, null, 1000, a.currency, 20000, "café", "EXPENSE")
        tx.add(b, null, 2000, a.currency, 20001, "taxi", "EXPENSE")

        val grouped = AccountsViewModel.groupByAccount(db.transactionQueries.selectAll().executeAsList())

        assertEquals(1, grouped[a.id]?.size)
        assertEquals(1, grouped[b]?.size)
        assertEquals("café", grouped[a.id]?.first()?.note)
    }

    @Test
    fun installmentIndexByTransactionNumeraCadaPlanPorSeparado() {
        val db = freshDb()
        seedIfEmpty(db)
        val accountRepo = AccountRepository(db)
        val cardRepo = CardRepository(db)
        val planRepo = InstallmentPlanRepository(db)
        val txRepo = TransactionRepository(db)

        val accId = accountRepo.add("Visa", "CREDIT", accountRepo.all().first().currency, openingBalanceMinor = 0)
        cardRepo.add(accId, "", "OTRA", 1_000_000L, null, null, 0.0)
        val cardId = cardRepo.byAccount(accId)!!.id

        val plan1 = planRepo.add(cardId, null, 90_000, 3, 0.0, startDateEpochDay = 100, description = "Plan1")
        val plan2 = planRepo.add(cardId, null, 60_000, 2, 0.0, startDateEpochDay = 105, description = "Plan2")

        // Fechas intercaladas entre los dos planes a propósito: cada uno debe numerarse solo, sin cruzarse.
        txRepo.add(accId, null, 30_000, "USD", 100, "Plan1", "EXPENSE", installmentPlanId = plan1)
        txRepo.add(accId, null, 30_000, "USD", 105, "Plan2", "EXPENSE", installmentPlanId = plan2)
        txRepo.add(accId, null, 30_000, "USD", 130, "Plan1", "EXPENSE", installmentPlanId = plan1)
        txRepo.add(accId, null, 30_000, "USD", 135, "Plan2", "EXPENSE", installmentPlanId = plan2)
        txRepo.add(accId, null, 30_000, "USD", 160, "Plan1", "EXPENSE", installmentPlanId = plan1)

        val movements = db.transactionQueries.selectAll().executeAsList()
        val index = AccountsViewModel.installmentIndexByTransaction(movements)

        val plan1Txs = movements.filter { it.installmentPlanId == plan1 }.sortedBy { it.date }
        val plan2Txs = movements.filter { it.installmentPlanId == plan2 }.sortedBy { it.date }

        assertEquals(listOf(1L, 2L, 3L), plan1Txs.map { index[it.id] })
        assertEquals(listOf(1L, 2L), plan2Txs.map { index[it.id] })
    }
}
