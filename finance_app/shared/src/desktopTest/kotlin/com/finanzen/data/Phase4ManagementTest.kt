package com.finanzen.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.db.FinanzenDb
import com.finanzen.domain.InstallmentMath
import com.finanzen.platform.NotificationScheduler
import com.finanzen.ui.format.monthPeriod
import com.finanzen.viewmodel.AccountsViewModel
import com.finanzen.viewmodel.BudgetsViewModel
import com.finanzen.viewmodel.TransactionsViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Phase4ManagementTest {
    private fun freshDb(): FinanzenDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanzenDb.Schema.create(driver)
        return FinanzenDb(driver)
    }

    @Test
    fun categoriaSeAgregaYElimina() {
        val db = freshDb()
        val repo = CategoryRepository(db)
        repo.add(name = "Mascotas", kind = "EXPENSE", parentId = null)
        val cat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first { it.name == "Mascotas" }
        assertEquals("EXPENSE", cat.kind)

        repo.delete(cat.id)
        assertNull(db.categoryQueries.selectById(cat.id).executeAsOneOrNull())
    }

    @Test
    fun transaccionSeActualiza() {
        val db = freshDb()
        seedIfEmpty(db)
        val tx = TransactionRepository(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        tx.add(account.id, null, 1000, account.currency, 20000, "café", "EXPENSE")
        val id = db.transactionQueries.selectAll().executeAsList().first().id

        tx.update(id, account.id, null, 2500, account.currency, 20000, "café grande", "EXPENSE")
        val updated = db.transactionQueries.selectById(id).executeAsOne()
        assertEquals(2500, updated.amountMinor)
        assertEquals("café grande", updated.note)
    }

    @Test
    fun crearCuentaDeCreditoCreaTarjetaYBorrarlaLaElimina() {
        val db = freshDb()
        seedIfEmpty(db)
        val vm = AccountsViewModel(AccountRepository(db), TransactionRepository(db), CardRepository(db), InstallmentPlanRepository(db), SettingsRepository(db), NotificationScheduler())
        val accountsBefore = db.accountQueries.selectAll().executeAsList().size

        // Crear una cuenta de crédito también crea su tarjeta con cupo/corte/pago/interés.
        val accId = vm.addAccount(
            type = "CREDIT",
            name = "Visa Oro",
            creditLimitMinor = 500_000L,
            cutoffDay = 15L,
            dueDay = 5L,
            interestRate = 2.5,
        )
        assertEquals(accountsBefore + 1, db.accountQueries.selectAll().executeAsList().size)
        val card = db.cardQueries.selectAll().executeAsList().first()
        assertEquals(accId, card.accountId)
        assertEquals(500_000L, card.creditLimitMinor)
        assertEquals(2.5, card.interestRate)

        // Borrar la cuenta borra también su tarjeta de respaldo.
        vm.delete(accId!!)
        assertNull(db.cardQueries.selectById(card.id).executeAsOneOrNull())
        assertEquals(accountsBefore, db.accountQueries.selectAll().executeAsList().size)
    }

    @Test
    fun gastoConCreditoYCuotasCreaPlanYRegistraSoloLaCuota1() {
        val db = freshDb()
        seedIfEmpty(db)
        val accVm = AccountsViewModel(AccountRepository(db), TransactionRepository(db), CardRepository(db), InstallmentPlanRepository(db), SettingsRepository(db), NotificationScheduler())
        // El interés se toma de la tarjeta (2.0%), ya no se pide en el formulario de movimientos.
        val accId = accVm.addAccount(type = "CREDIT", name = "Visa", creditLimitMinor = 1_000_000L, interestRate = 2.0)!!
        val txVm = TransactionsViewModel(
            TransactionRepository(db),
            AccountRepository(db),
            CategoryRepository(db),
            CardRepository(db),
            InstallmentPlanRepository(db),
            BudgetRepository(db),
            SettingsRepository(db),
            NotificationScheduler(),
        )

        txVm.save(
            id = null,
            accountId = accId,
            categoryId = null,
            amountMinor = 120_000,
            kind = "EXPENSE",
            note = "TV",
            dateEpochDay = 20_000,
            installments = 12,
        )

        val plans = db.installmentPlanQueries.selectAll().executeAsList()
        assertEquals(1, plans.size)
        assertEquals(12L, plans.first().installments)
        assertEquals(2.0, plans.first().interestRate)
        assertEquals(120_000L, plans.first().totalAmountMinor)

        // La transacción registrada es la cuota 1 (con interés), no el monto total de la compra.
        val tx = db.transactionQueries.selectAll().executeAsList().first { it.note == "TV" }
        assertEquals(plans.first().id, tx.installmentPlanId)
        val expectedCuota = InstallmentMath.monthlyPaymentMinor(120_000, 12, 2.0)
        assertEquals(expectedCuota, tx.amountMinor)
    }

    @Test
    fun gastoConCreditoYUnaSolaCuotaNoCreaPlan() {
        val db = freshDb()
        seedIfEmpty(db)
        val accVm = AccountsViewModel(AccountRepository(db), TransactionRepository(db), CardRepository(db), InstallmentPlanRepository(db), SettingsRepository(db), NotificationScheduler())
        val accId = accVm.addAccount(type = "CREDIT", name = "Visa", creditLimitMinor = 1_000_000L, interestRate = 2.0)!!
        val txVm = TransactionsViewModel(
            TransactionRepository(db),
            AccountRepository(db),
            CategoryRepository(db),
            CardRepository(db),
            InstallmentPlanRepository(db),
            BudgetRepository(db),
            SettingsRepository(db),
            NotificationScheduler(),
        )

        txVm.save(id = null, accountId = accId, categoryId = null, amountMinor = 50_000, kind = "EXPENSE", note = "Cena", dateEpochDay = 20_000, installments = 1)

        assertEquals(0, db.installmentPlanQueries.selectAll().executeAsList().size)
        val tx = db.transactionQueries.selectAll().executeAsList().first { it.note == "Cena" }
        assertEquals(50_000L, tx.amountMinor)
        assertNull(tx.installmentPlanId)
    }

    @Test
    fun postDueInstallmentsGeneraLasCuotasFaltantesHastaHoySinDuplicar() {
        val db = freshDb()
        seedIfEmpty(db)
        val accountRepo = AccountRepository(db)
        val cardRepo = CardRepository(db)
        val planRepo = InstallmentPlanRepository(db)
        val txRepo = TransactionRepository(db)

        val accId = accountRepo.add("Visa", "CREDIT", "USD", openingBalanceMinor = 0)
        cardRepo.add(accId, "", "OTRA", 1_000_000L, null, null, 0.0)
        val card = cardRepo.byAccount(accId)!!

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val start = today.minus(DatePeriod(months = 3))
        val startEpoch = start.toEpochDays().toLong()
        val planId = planRepo.add(card.id, categoryId = null, totalAmountMinor = 120_000, installments = 12, interestRate = 0.0, startDateEpochDay = startEpoch, description = "TV")
        // La cuota 1 ya se registra al crear el plan (lo hace TransactionsViewModel.save en producción).
        val cuota = InstallmentMath.monthlyPaymentMinor(120_000, 12, 0.0)
        txRepo.add(accId, null, cuota, "USD", startEpoch, "TV", "EXPENSE", installmentPlanId = planId)

        // Construir el ViewModel dispara el catch-up en su init.
        AccountsViewModel(accountRepo, txRepo, cardRepo, planRepo, SettingsRepository(db), NotificationScheduler())

        val expected = (InstallmentMath.elapsedInstallments(startEpoch, today.toEpochDays().toLong(), 12) + 1).coerceAtMost(12)
        assertEquals(expected, db.transactionQueries.countByInstallmentPlan(planId).executeAsOne())

        // Abrir la pantalla de Cuentas otra vez el mismo día no debe duplicar cuotas.
        AccountsViewModel(accountRepo, txRepo, cardRepo, planRepo, SettingsRepository(db), NotificationScheduler())
        assertEquals(expected, db.transactionQueries.countByInstallmentPlan(planId).executeAsOne())
    }

    @Test
    fun payOffCardTransfiereYLiberaElCupo() {
        val db = freshDb()
        seedIfEmpty(db)
        val accountRepo = AccountRepository(db)
        val cardRepo = CardRepository(db)
        val planRepo = InstallmentPlanRepository(db)
        val txRepo = TransactionRepository(db)
        val vm = AccountsViewModel(accountRepo, txRepo, cardRepo, planRepo, SettingsRepository(db), NotificationScheduler())

        val savingsId = accountRepo.add("Ahorros", "SAVINGS", "USD", openingBalanceMinor = 500_000)
        val creditId = vm.addAccount(type = "CREDIT", name = "Visa", creditLimitMinor = 1_000_000L, interestRate = 0.0)!!
        val card = cardRepo.byAccount(creditId)!!
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays().toLong()
        val planId = planRepo.add(card.id, categoryId = null, totalAmountMinor = 120_000, installments = 12, interestRate = 0.0, startDateEpochDay = today, description = "TV")
        txRepo.add(creditId, null, InstallmentMath.monthlyPaymentMinor(120_000, 12, 0.0), "USD", today, "TV", "EXPENSE", installmentPlanId = planId)

        vm.payOffCard(card.id, savingsId, 10_000)

        val plan = db.installmentPlanQueries.selectById(planId).executeAsOne()
        assertEquals(1L, plan.settled)
        val transfer = db.transactionQueries.selectAll().executeAsList().first { it.kind == "TRANSFER" }
        assertEquals(savingsId, transfer.accountId)
        assertEquals(creditId, transfer.transferAccountId)
        assertEquals(10_000L, transfer.amountMinor)
    }

    @Test
    fun computeBalancesIncluyeIngresosGastosYTransferencias() {
        val db = freshDb()
        db.currencyQueries.upsert("USD", "$", 2, 1.0, "US Dollar", ".", ",")
        val accountRepo = AccountRepository(db)
        val a = accountRepo.add("A", "CASH", "USD", openingBalanceMinor = 10_000)
        val b = accountRepo.add("B", "DEBIT", "USD", openingBalanceMinor = 0)
        val tx = TransactionRepository(db)
        tx.add(a, null, 5_000, "USD", 0, "sueldo", "INCOME") // A: +5000
        tx.add(a, null, 2_000, "USD", 0, "café", "EXPENSE") // A: -2000
        tx.addTransfer(a, b, 3_000, "USD", 0, "ahorro") // A: -3000, B: +3000

        val balances = com.finanzen.viewmodel.AccountsViewModel.computeBalances(
            accounts = db.accountQueries.selectAll().executeAsList(),
            txs = db.transactionQueries.selectAll().executeAsList(),
        )
        assertEquals(10_000L, balances[a]) // 10000 + 5000 - 2000 - 3000
        assertEquals(3_000L, balances[b]) // 0 + 3000
    }

    @Test
    fun computeBudgetsCalculaGastoYLimiteDelMes() {
        val db = freshDb()
        seedIfEmpty(db)
        val budgetRepo = BudgetRepository(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val cat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()

        val period = BudgetsViewModel.currentPeriodMonth()
        budgetRepo.setLimit(cat.id, period, 50_000)

        // gasto en el mes actual (epochDay derivado del periodo no es trivial; usamos hoy)
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays().toLong()
        TransactionRepository(db).add(account.id, cat.id, 12_000, account.currency, today, "compra", "EXPENSE")

        val data = BudgetsViewModel.computeBudgets(
            budgets = db.budgetQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            txs = db.transactionQueries.selectAll().executeAsList(),
            period = period,
        )
        val row = data.rows.first { it.categoryId == cat.id }
        assertEquals(50_000, row.limitMinor)
        assertEquals(12_000, row.spentMinor)
        assertTrue(data.rows.all { it.spentMinor >= 0 })
    }

    @Test
    fun computeBudgetsHeredaElLimiteDelMesAnteriorSinAlterarloRetroactivamente() {
        val db = freshDb()
        seedIfEmpty(db)
        val budgetRepo = BudgetRepository(db)
        val cat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val monthM = LocalDate(today.year, today.month, 1)
        val monthM1 = monthM.plus(DatePeriod(months = 1))
        val periodM = monthPeriod(monthM)
        val periodM1 = monthPeriod(monthM1)

        budgetRepo.setLimit(cat.id, periodM, 50_000)

        fun limitAt(period: Long) = BudgetsViewModel.computeBudgets(
            budgets = db.budgetQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            txs = db.transactionQueries.selectAll().executeAsList(),
            period = period,
        ).rows.first { it.categoryId == cat.id }.limitMinor

        // M+1 no tiene fila propia: hereda el límite de M.
        assertEquals(50_000, limitAt(periodM1))

        // Se cambia explícitamente el límite en M+1.
        budgetRepo.setLimit(cat.id, periodM1, 80_000)
        assertEquals(80_000, limitAt(periodM1))

        // M no debió alterarse retroactivamente.
        assertEquals(50_000, limitAt(periodM))
    }
}
