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

        // (nombre, clave de icono, color ARGB). Las claves deben existir en categoryIcons (UI);
        // el color es un ARGB de la paleta para que el seed inicial se vea variado.
        val expenseSeeds = listOf(
            Triple("Alimentación", "restaurant", 0xFFE53935),
            Triple("Transporte", "car", 0xFF1E88E5),
            Triple("Vivienda", "home", 0xFF00897B),
            Triple("Salud", "health", 0xFF43A047),
            Triple("Ocio", "games", 0xFFF4511E),
        )
        expenseSeeds.forEach { (name, icon, color) ->
            db.categoryQueries.insert(parentId = null, name = name, icon = icon, color = color, kind = "EXPENSE")
        }

        val incomeSeeds = listOf(
            Triple("Salario", "salary", 0xFF3949AB),
            Triple("Freelance", "work", 0xFF00838F),
            Triple("Otros", "other", 0xFF546E7A),
        )
        incomeSeeds.forEach { (name, icon, color) ->
            db.categoryQueries.insert(parentId = null, name = name, icon = icon, color = color, kind = "INCOME")
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
