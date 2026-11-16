package com.finanzen.viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.data.AccountRepository
import com.finanzen.data.CategoryRepository
import com.finanzen.data.InvestmentRepository
import com.finanzen.data.SettingsRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.data.seedIfEmpty
import com.finanzen.db.FinanzenDb
import kotlin.test.Test
import kotlin.test.assertEquals

class InvestmentContributeTest {
    @Test
    fun aporteSaleDeLaCuentaElegidaSoloSiEsDeLaMismaMoneda() {
        val db = FinanzenDb(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { FinanzenDb.Schema.create(it) })
        seedIfEmpty(db)
        val accountRepo = AccountRepository(db)
        val fondoAcc = accountRepo.add("Ahorros", "SAVINGS", "USD")
        val otraAcc = accountRepo.add("Nómina", "CHECKING", "USD")
        val euroAcc = accountRepo.add("Euros", "SAVINGS", "EUR")
        val vm = InvestmentsViewModel(InvestmentRepository(db), accountRepo, TransactionRepository(db), SettingsRepository(db), CategoryRepository(db))
        vm.addInvestment("Fondo", 10_000, fondoAcc, periodic = false, frequency = null, intervalCount = null, startEpochDay = 20_000)
        val invId = db.investmentQueries.selectAll().executeAsList().single().id

        vm.contribute(invId, 5_000, accountId = otraAcc)
        vm.contribute(invId, 5_000, accountId = euroAcc) // otra moneda: cae a la cuenta del fondo

        val aportes = db.transactionQueries.selectAll().executeAsList().filter { it.investmentId == invId }
        assertEquals(listOf(fondoAcc, otraAcc, fondoAcc).sorted(), aportes.map { it.accountId }.sorted())
        assertEquals(setOf("Aporte: Fondo"), aportes.map { it.note }.toSet())
    }
}
