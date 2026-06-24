package com.finanzen.data

import com.finanzen.db.FinanzenDb

/**
 * Inserta datos mínimos en la primera apertura de la DB. Idempotente: si ya hay monedas, no hace nada.
 */
fun seedIfEmpty(db: FinanzenDb) {
    // Checkpoint = cuenta. Si existe, todo lo anterior (currencies, categorías) también — el seed va en una sola transacción.
    val seeded = db.accountQueries.selectAll().executeAsList().isNotEmpty()
    if (seeded) return

    db.transaction {
        db.currencyQueries.upsert("USD", "$", 2, 1.0)
        db.currencyQueries.upsert("EUR", "€", 2, 1.0)
        db.currencyQueries.upsert("COP", "$", 0, 1.0)
        db.currencyQueries.upsert("MXN", "$", 2, 1.0)

        val expenseSeeds = listOf(
            "Alimentación" to "🍔",
            "Transporte" to "🚗",
            "Vivienda" to "🏠",
            "Suscripciones" to "📺",
            "Salud" to "🩺",
            "Ocio" to "🎮",
        )
        expenseSeeds.forEach { (name, icon) ->
            db.categoryQueries.insert(parentId = null, name = name, icon = icon, color = 0, kind = "EXPENSE")
        }

        val incomeSeeds = listOf("Salario" to "💼", "Freelance" to "💻", "Otros" to "💰")
        incomeSeeds.forEach { (name, icon) ->
            db.categoryQueries.insert(parentId = null, name = name, icon = icon, color = 0, kind = "INCOME")
        }

        db.accountQueries.insert(
            name = "Efectivo",
            type = "CASH",
            currency = "USD",
            openingBalanceMinor = 0,
            color = 0,
            archived = 0,
        )
    }
}
