package com.finanzen.data

import com.finanzen.db.FinanzenDb

private const val KEY_SEEDED = "seeded"

/**
 * Inserta datos mínimos en la primera apertura de la DB. Idempotente: una bandera persistente marca
 * que ya se sembró. No se infiere de la cuenta porque el usuario puede borrarla y dispararía un
 * re-seed con categorías/monedas duplicadas.
 */
fun seedIfEmpty(db: FinanzenDb) {
    if (db.settingQueries.get(KEY_SEEDED).executeAsOneOrNull() != null) return

    // DB previa a esta bandera que ya tenía datos: márcala como sembrada y no insertes de nuevo.
    if (db.currencyQueries.selectAll().executeAsList().isNotEmpty()) {
        db.settingQueries.put(KEY_SEEDED, "true")
        return
    }

    db.transaction {
        db.settingQueries.put(KEY_SEEDED, "true")
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
