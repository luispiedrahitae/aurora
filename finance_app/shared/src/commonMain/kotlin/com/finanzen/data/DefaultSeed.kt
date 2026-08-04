package com.finanzen.data

import com.finanzen.db.FinanzenDb
import com.finanzen.platform.systemCountryCode

private const val KEY_SEEDED = "seeded"
private const val KEY_CURRENCIES_EXPANDED = "currencies_expanded"

/**
 * Inserta datos mínimos en la primera apertura de la DB. Idempotente: una bandera persistente marca
 * que ya se sembró. No se infiere de la cuenta porque el usuario puede borrarla y dispararía un
 * re-seed con categorías/monedas duplicadas.
 */
fun seedIfEmpty(db: FinanzenDb) {
    if (db.settingQueries.get(KEY_SEEDED).executeAsOneOrNull() == null) {
        // DB previa a esta bandera que ya tenía datos: márcala como sembrada y no insertes de nuevo.
        if (db.currencyQueries.selectAll().executeAsList().isNotEmpty()) {
            db.settingQueries.put(KEY_SEEDED, "true")
        } else {
            db.transaction {
                db.settingQueries.put(KEY_SEEDED, "true")

                // Las monedas van primero: la cuenta "Efectivo" de más abajo referencia esta moneda
                // por FK, así que Currency debe tener filas antes de insertar cuentas (SQLite no
                // exige FKs por defecto, pero no hay que depender de eso).
                upsertWorldCurrencies(db)
                db.settingQueries.put(KEY_CURRENCIES_EXPANDED, "true")

                // Moneda inicial según el país del locale del sistema (sin pedir permisos: el
                // locale del SO es información pública). Sin match conocido, USD como antes.
                val initialCurrency = COUNTRY_TO_CURRENCY[systemCountryCode()] ?: SettingsRepository.DEFAULT_CURRENCY
                db.settingQueries.put(SettingsRepository.KEY_CURRENCY, initialCurrency)

                // (nombre, clave de icono, color ARGB). Las claves deben existir en categoryIcons (UI);
                // el color es un ARGB de la paleta para que el seed inicial se vea variado. Cada
                // categoría recibe una subcategoría "General" — categoryId ahora exige siempre una
                // hoja (ver Transaction.sq), así que ninguna categoría puede quedar sin al menos una.
                val expenseSeeds = listOf(
                    Triple("Alimentación", "restaurant", 0xFFE53935),
                    Triple("Transporte", "car", 0xFF1E88E5),
                    Triple("Vivienda", "home", 0xFF00897B),
                    Triple("Salud", "health", 0xFF43A047),
                    Triple("Ocio", "games", 0xFFF4511E),
                )
                expenseSeeds.forEach { (name, icon, color) -> insertWithGeneralChild(db, name, icon, color, "EXPENSE") }

                val incomeSeeds = listOf(
                    Triple("Salario", "salary", 0xFF3949AB),
                    Triple("Freelance", "work", 0xFF00838F),
                    Triple("Otros", "other", 0xFF546E7A),
                )
                incomeSeeds.forEach { (name, icon, color) -> insertWithGeneralChild(db, name, icon, color, "INCOME") }

                // Categorías de sistema — usadas como fallback cuando un movimiento no tiene una
                // categoría explícita (transferencias, ajustes de saldo, suscripciones sin categoría
                // elegida). Resueltas en runtime por nombre vía systemCategoryLeaf() en Repositories.kt.
                insertWithGeneralChild(db, "Transferencias", "other", 0xFF757575, "TRANSFER", "Transferencia entre cuentas")
                insertWithGeneralChild(db, "Ajustes", "other", 0xFF757575, "ADJUSTMENT", "Ajuste de saldo")
                insertWithGeneralChild(db, "Suscripciones", "card", 0xFF8E24AA, "EXPENSE", "Suscripción")

                db.accountQueries.insert(
                    name = "Efectivo",
                    type = "CASH",
                    currency = initialCurrency,
                    openingBalanceMinor = 0,
                    color = 0,
                    archived = 0,
                )
            }
        }
    }
    expandCurrenciesIfNeeded(db)
}

/**
 * Expande la tabla Currency a las ~149 monedas de [WORLD_CURRENCIES]. Bandera propia
 * (separada de KEY_SEEDED) para que instalaciones existentes que ya sembraron las 4 monedas
 * originales también reciban la lista completa, sin re-sembrar categorías/cuenta. INSERT OR
 * REPLACE por code (PK) también rellena name/decimalSeparator/groupSeparator en esas 4 filas
 * originales con los valores correctos del dataset.
 */
private fun expandCurrenciesIfNeeded(db: FinanzenDb) {
    if (db.settingQueries.get(KEY_CURRENCIES_EXPANDED).executeAsOneOrNull() != null) return
    db.transaction {
        upsertWorldCurrencies(db)
        db.settingQueries.put(KEY_CURRENCIES_EXPANDED, "true")
    }
}

private fun upsertWorldCurrencies(db: FinanzenDb) {
    WORLD_CURRENCIES.forEach { c ->
        db.currencyQueries.upsert(c.code, c.symbol, c.decimals.toLong(), 1.0, c.name, c.decimalSeparator, c.groupSeparator)
    }
}

private fun insertWithGeneralChild(db: FinanzenDb, name: String, icon: String, color: Long, kind: String, childName: String = "General") {
    db.categoryQueries.insert(parentId = null, name = name, icon = icon, color = color, kind = kind)
    val parentId = db.categoryQueries.lastInsertRowId().executeAsOne()
    db.categoryQueries.insert(parentId = parentId, name = childName, icon = icon, color = color, kind = kind)
}
