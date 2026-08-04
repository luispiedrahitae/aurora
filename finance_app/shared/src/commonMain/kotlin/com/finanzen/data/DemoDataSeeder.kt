package com.finanzen.data

import com.finanzen.db.FinanzenDb
import com.finanzen.domain.InstallmentMath
import com.finanzen.domain.RecurrenceSchedule
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.random.Random

private const val KEY_DEMO_SEEDED = "demoDataSeeded"

/** Movimientos "sueltos" (ingreso/gasto/transferencia/ajuste) que genera [seedMonth] por mes. Junto
 * con los 3 movimientos recurrentes (suscripción, aporte de inversión, cuota) que arma
 * [seedRecurringInstruments] una sola vez, suman 23 movimientos/mes. */
private const val MONTH_MOVEMENT_COUNT = 20

private data class SubSeed(val name: String, val icon: String)

private data class CategoryChoice(val id: Long, val label: String, val topName: String)

private val EXPENSE_SUBCATEGORIES: Map<String, List<SubSeed>> = mapOf(
    "Alimentación" to listOf(SubSeed("Supermercado", "restaurant"), SubSeed("Restaurantes", "restaurant"), SubSeed("Delivery", "restaurant"), SubSeed("Cafetería", "restaurant")),
    "Transporte" to listOf(SubSeed("Gasolina", "car"), SubSeed("Transporte público", "car"), SubSeed("Taxi/Uber", "car"), SubSeed("Mantenimiento vehículo", "car")),
    "Vivienda" to listOf(SubSeed("Alquiler", "home"), SubSeed("Servicios públicos", "home"), SubSeed("Internet", "home"), SubSeed("Mantenimiento hogar", "home")),
    "Salud" to listOf(SubSeed("Consultas médicas", "health"), SubSeed("Farmacia", "health"), SubSeed("Seguro médico", "health"), SubSeed("Gimnasio", "health")),
    "Ocio" to listOf(SubSeed("Streaming", "games"), SubSeed("Cine", "games"), SubSeed("Videojuegos", "games"), SubSeed("Salidas", "games")),
)

private val INCOME_SUBCATEGORIES: Map<String, List<SubSeed>> = mapOf(
    "Salario" to listOf(SubSeed("Sueldo mensual", "salary"), SubSeed("Bono", "salary")),
    "Freelance" to listOf(SubSeed("Proyecto cliente", "work"), SubSeed("Consultoría", "work")),
    "Otros" to listOf(SubSeed("Reembolso", "other"), SubSeed("Regalo", "other"), SubSeed("Venta", "other")),
)

// Rangos en unidad mayor (antes de aplicar los decimales de la moneda de la cuenta).
private val EXPENSE_RANGES: Map<String, LongRange> = mapOf(
    "Alimentación" to 8L..120L,
    "Transporte" to 5L..80L,
    "Vivienda" to 50L..600L,
    "Salud" to 10L..200L,
    "Ocio" to 5L..90L,
)
private val INCOME_RANGES: Map<String, LongRange> = mapOf(
    "Salario" to 800L..2500L,
    "Freelance" to 100L..900L,
    "Otros" to 20L..300L,
)
private val TRANSFER_RANGE = 20L..500L
private val ADJUSTMENT_RANGE = 10L..150L

/**
 * Siembra movimientos de ejemplo para los últimos 48 meses, en subcategorías nuevas relacionadas
 * con las 8 categorías top-level que ya sembró [seedIfEmpty]: 20 movimientos sueltos por mes
 * (algunos días con más de un movimiento a propósito, ver [dayPoolWithDuplicates]) cubriendo los 7
 * tipos que distingue la UI (ingreso, gasto, transferencia, ajuste de saldo, suscripción, cuota de
 * plan de cuotas, aporte de inversión) — ver
 * [seedRecurringInstruments] para los tres últimos, que solo se crean una vez y se backfillean con
 * la misma lógica de recurrencia que usa la app en producción. Solo para poblar un dispositivo de
 * prueba — idempotente vía bandera en Setting, pero no pensado para correr en producción (ver
 * AppModule.kt).
 */
fun seedDemoTransactions(db: FinanzenDb, today: LocalDate) {
    if (db.settingQueries.get(KEY_DEMO_SEEDED).executeAsOneOrNull() != null) return

    db.transaction {
        db.settingQueries.put(KEY_DEMO_SEEDED, "true")

        val accountRepo = AccountRepository(db)
        val categoryRepo = CategoryRepository(db)
        val txRepo = TransactionRepository(db)
        val cardRepo = CardRepository(db)
        val planRepo = InstallmentPlanRepository(db)
        val subsRepo = SubscriptionRepository(db)
        val investRepo = InvestmentRepository(db)

        val cashAccount = accountRepo.all().firstOrNull { it.type == "CASH" } ?: accountRepo.all().first()
        val currency = cashAccount.currency
        val decimals = db.currencyQueries.selectByCode(currency).executeAsOneOrNull()?.decimals ?: 2L
        val scale = scaleFor(decimals)

        val debitId = accountRepo.add("Tarjeta de Débito", "DEBIT", currency)
        val savingsId = accountRepo.add("Ahorros", "SAVINGS", currency)
        val creditId = accountRepo.add("Tarjeta de Crédito", "CREDIT", currency)
        val accountIds = listOf(cashAccount.id, debitId, savingsId)

        val topExpenseCats = categoryRepo.byKind("EXPENSE").filter { it.parentId == null }
        val expenseChoices = buildChoices(categoryRepo, topExpenseCats, EXPENSE_SUBCATEGORIES, "EXPENSE")
        val incomeChoices = buildChoices(categoryRepo, categoryRepo.byKind("INCOME").filter { it.parentId == null }, INCOME_SUBCATEGORIES, "INCOME")

        val todayEpochDay = today.toEpochDays().toLong()
        seedRecurringInstruments(
            txRepo = txRepo,
            cardRepo = cardRepo,
            planRepo = planRepo,
            subsRepo = subsRepo,
            investRepo = investRepo,
            expenseChoices = expenseChoices,
            investmentCategoryId = topExpenseCats.first().id,
            creditAccountId = creditId,
            debitAccountId = debitId,
            savingsAccountId = savingsId,
            currency = currency,
            scale = scale,
            todayEpochDay = todayEpochDay,
        )

        val random = Random(20250101)
        val historyStart = today.plus(DatePeriod(months = -47))
        var year = historyStart.year
        var month = historyStart.monthNumber
        while (year < today.year || (year == today.year && month <= today.monthNumber)) {
            val lastDay = if (year == today.year && month == today.monthNumber) today.dayOfMonth else daysInMonth(year, month)
            seedMonth(txRepo, random, accountIds, currency, scale, expenseChoices, incomeChoices, year, month, lastDay)
            month++
            if (month > 12) {
                month = 1
                year++
            }
        }
    }
}

/**
 * Crea una suscripción, una inversión periódica y un plan de cuotas (uno de cada, arrancando en
 * enero de 2025) y backfillea sus movimientos con [RecurrenceSchedule]/[InstallmentMath] — la misma
 * lógica que usan [com.finanzen.viewmodel.SubscriptionCatchUp], `InvestmentsViewModel.postDueContributions`
 * y `AccountsViewModel.postDueInstallments` en producción. Al dejar `nextChargeDate`/
 * `nextContributionDate` avanzados y el conteo de cuotas ya al día, esos catch-ups reales no vuelven
 * a postear los mismos movimientos la primera vez que el usuario navega a esas pantallas.
 */
private fun seedRecurringInstruments(
    txRepo: TransactionRepository,
    cardRepo: CardRepository,
    planRepo: InstallmentPlanRepository,
    subsRepo: SubscriptionRepository,
    investRepo: InvestmentRepository,
    expenseChoices: List<CategoryChoice>,
    investmentCategoryId: Long,
    creditAccountId: Long,
    debitAccountId: Long,
    savingsAccountId: Long,
    currency: String,
    scale: Long,
    todayEpochDay: Long,
) {
    val streamingCat = expenseChoices.first { it.label == "Streaming" }
    val homeMaintCat = expenseChoices.first { it.label == "Mantenimiento hogar" }

    // Suscripción: cobro el día 6 de cada mes desde enero 2025.
    val subChargeMinor = 15L * scale
    val subStart = LocalDate(2025, 1, 6).toEpochDays().toLong()
    val subId = subsRepo.add(
        name = "Streaming Plus",
        amountMinor = subChargeMinor,
        currency = currency,
        categoryId = streamingCat.id,
        accountId = debitAccountId,
        frequency = "MONTHLY",
        intervalCount = 6,
        nextChargeDateEpochDay = subStart,
        remindDaysBefore = 3,
    )
    val subDue = RecurrenceSchedule.occurrencesDueUpTo(subStart, todayEpochDay, "MONTHLY", 6)
    subDue.dates.forEach { day ->
        txRepo.add(debitAccountId, streamingCat.id, subChargeMinor, currency, day, "Streaming Plus", "EXPENSE", subscriptionId = subId)
    }
    subsRepo.updateNextCharge(subId, subDue.next)

    // Inversión periódica: aporte el día 9 de cada mes desde enero 2025.
    val investMinor = 100L * scale
    val investStart = LocalDate(2025, 1, 9).toEpochDays().toLong()
    val investId = investRepo.add(
        name = "Fondo indexado",
        amountMinor = investMinor,
        currency = currency,
        accountId = savingsAccountId,
        categoryId = investmentCategoryId,
        periodic = true,
        frequency = "MONTHLY",
        intervalCount = 9,
        nextContributionDate = investStart,
        startDate = investStart,
    )
    val investDue = RecurrenceSchedule.occurrencesDueUpTo(investStart, todayEpochDay, "MONTHLY", 9)
    investDue.dates.forEach { day ->
        txRepo.add(savingsAccountId, investmentCategoryId, investMinor, currency, day, "Fondo indexado", "EXPENSE", investmentId = investId)
    }
    investRepo.updateNextContribution(investId, investDue.next)

    // Plan de cuotas: compra financiada desde enero 2025 en la tarjeta de crédito nueva, cuota el día 15.
    cardRepo.add(creditAccountId, "4242", "VISA", creditLimitMinor = 5000L * scale, cutoffDay = 20L, dueDay = 5L, interestRate = 45.0)
    val card = cardRepo.byAccount(creditAccountId) ?: error("tarjeta recién creada no encontrada")
    val planStart = LocalDate(2025, 1, 15).toEpochDays().toLong()
    val totalAmountMinor = 1200L * scale
    val installments = 24L
    val planId = planRepo.add(
        cardId = card.id,
        categoryId = homeMaintCat.id,
        totalAmountMinor = totalAmountMinor,
        installments = installments,
        interestRate = 18.0,
        startDateEpochDay = planStart,
        description = "Electrodoméstico",
    )
    val monthly = InstallmentMath.monthlyPaymentMinor(totalAmountMinor, installments, 18.0)
    val target = (InstallmentMath.elapsedInstallments(planStart, todayEpochDay, installments) + 1).coerceAtMost(installments)
    for (i in 0 until target) {
        txRepo.add(
            accountId = creditAccountId,
            categoryId = homeMaintCat.id,
            amountMinor = monthly,
            currency = currency,
            epochDay = InstallmentMath.installmentDueDate(planStart, i),
            note = "Electrodoméstico",
            kind = "EXPENSE",
            installmentPlanId = planId,
        )
    }
}

private fun buildChoices(
    categoryRepo: CategoryRepository,
    topCategories: List<com.finanzen.db.Category>,
    subcategorySeeds: Map<String, List<SubSeed>>,
    kind: String,
): List<CategoryChoice> = topCategories.flatMap { cat ->
    val subChoices = subcategorySeeds[cat.name].orEmpty().map { sub ->
        val subId = categoryRepo.addAndGetId(sub.name, kind, cat.id, sub.icon, cat.color)
        CategoryChoice(subId, sub.name, cat.name)
    }
    listOf(CategoryChoice(cat.id, cat.name, cat.name)) + subChoices
}

private fun seedMonth(
    txRepo: TransactionRepository,
    random: Random,
    accountIds: List<Long>,
    currency: String,
    scale: Long,
    expenseChoices: List<CategoryChoice>,
    incomeChoices: List<CategoryChoice>,
    year: Int,
    month: Int,
    lastDay: Int,
) {
    val kinds = mutableListOf("EXPENSE", "INCOME", "TRANSFER", "ADJUSTMENT")
    repeat(MONTH_MOVEMENT_COUNT - kinds.size) { kinds += randomKind(random) }
    val days = dayPoolWithDuplicates(random, lastDay, kinds.size)

    kinds.forEachIndexed { index, kind ->
        val day = days[index]
        val epochDay = LocalDate(year, month, day).toEpochDays().toLong()
        val fromAccount = accountIds.random(random)
        when (kind) {
            "TRANSFER" -> {
                val toAccount = accountIds.filter { it != fromAccount }.random(random)
                val amount = randomAmountMinor(TRANSFER_RANGE, scale, random)
                txRepo.addTransfer(fromAccount, toAccount, amount, currency, epochDay, "Transferencia")
            }
            "EXPENSE" -> {
                val choice = expenseChoices.random(random)
                val amount = randomAmountMinor(EXPENSE_RANGES[choice.topName] ?: 10L..100L, scale, random)
                txRepo.add(fromAccount, choice.id, amount, currency, epochDay, choice.label, "EXPENSE")
            }
            "ADJUSTMENT" -> {
                val amount = randomAmountMinor(ADJUSTMENT_RANGE, scale, random) * if (random.nextBoolean()) 1L else -1L
                txRepo.addAdjustment(fromAccount, amount, currency, epochDay, "Ajuste de saldo")
            }
            else -> {
                val choice = incomeChoices.random(random)
                val amount = randomAmountMinor(INCOME_RANGES[choice.topName] ?: 50L..500L, scale, random)
                txRepo.add(fromAccount, choice.id, amount, currency, epochDay, choice.label, "INCOME")
            }
        }
    }
}

/** Arma un pool de días para el mes con 2-4 duplicados deliberados en un mismo "día caliente",
 * en vez de dejar que las colisiones de día ocurran solo por azar. */
private fun dayPoolWithDuplicates(random: Random, lastDay: Int, count: Int): List<Int> {
    val days = MutableList(count) { random.nextInt(1, lastDay + 1) }
    val hotDay = random.nextInt(1, lastDay + 1)
    val duplicateCount = random.nextInt(2, 5) // 2..4
    days.indices.shuffled(random).take(duplicateCount).forEach { days[it] = hotDay }
    return days
}

private fun randomKind(random: Random): String {
    val roll = random.nextInt(100)
    return when {
        roll < 55 -> "EXPENSE"
        roll < 80 -> "INCOME"
        else -> "TRANSFER"
    }
}

private fun randomAmountMinor(range: LongRange, scale: Long, random: Random): Long = random.nextLong(range.first, range.last + 1) * scale + random.nextLong(0, scale)

private fun daysInMonth(year: Int, month: Int): Int {
    val first = LocalDate(year, month, 1)
    return first.plus(DatePeriod(months = 1)).toEpochDays() - first.toEpochDays()
}

private fun scaleFor(decimals: Long): Long {
    var r = 1L
    repeat(decimals.toInt()) { r *= 10 }
    return r
}
