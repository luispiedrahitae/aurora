package com.finanzen.data

import com.finanzen.db.FinanzenDb
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.random.Random

private const val KEY_DEMO_SEEDED = "demoDataSeeded"

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

/**
 * Siembra movimientos de ejemplo (ingreso/gasto/transferencia) para cada mes desde enero del
 * año pasado hasta el mes actual, en subcategorías nuevas relacionadas con las 8 categorías
 * top-level que ya sembró [seedIfEmpty]. Solo para poblar un dispositivo de prueba — idempotente
 * vía bandera en Setting, pero no pensado para correr en producción (ver AppModule.kt).
 */
fun seedDemoTransactions(db: FinanzenDb, today: LocalDate) {
    if (db.settingQueries.get(KEY_DEMO_SEEDED).executeAsOneOrNull() != null) return

    db.transaction {
        db.settingQueries.put(KEY_DEMO_SEEDED, "true")

        val accountRepo = AccountRepository(db)
        val categoryRepo = CategoryRepository(db)
        val txRepo = TransactionRepository(db)

        val cashAccount = accountRepo.all().firstOrNull { it.type == "CASH" } ?: accountRepo.all().first()
        val currency = cashAccount.currency
        val decimals = db.currencyQueries.selectByCode(currency).executeAsOneOrNull()?.decimals ?: 2L
        val scale = scaleFor(decimals)

        val debitId = accountRepo.add("Tarjeta de Débito", "DEBIT", currency)
        val savingsId = accountRepo.add("Ahorros", "SAVINGS", currency)
        val accountIds = listOf(cashAccount.id, debitId, savingsId)

        val expenseChoices = buildChoices(categoryRepo, categoryRepo.byKind("EXPENSE").filter { it.parentId == null }, EXPENSE_SUBCATEGORIES, "EXPENSE")
        val incomeChoices = buildChoices(categoryRepo, categoryRepo.byKind("INCOME").filter { it.parentId == null }, INCOME_SUBCATEGORIES, "INCOME")

        val random = Random(20250101)
        var year = today.year - 1
        var month = 1
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
    val count = random.nextInt(10, 16)
    val kinds = mutableListOf("EXPENSE", "INCOME", "TRANSFER")
    repeat(count - kinds.size) { kinds += randomKind(random) }

    kinds.forEach { kind ->
        val day = random.nextInt(1, lastDay + 1)
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
            else -> {
                val choice = incomeChoices.random(random)
                val amount = randomAmountMinor(INCOME_RANGES[choice.topName] ?: 50L..500L, scale, random)
                txRepo.add(fromAccount, choice.id, amount, currency, epochDay, choice.label, "INCOME")
            }
        }
    }
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
