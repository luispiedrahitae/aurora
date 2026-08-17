package com.finanzen.data

import com.finanzen.db.FinanzenDb
import com.finanzen.platform.systemCountryCode

private const val KEY_SEEDED = "seeded"
private const val KEY_CURRENCIES_EXPANDED = "currencies_expanded"
private const val KEY_SYSTEM_CATEGORIES_ENSURED = "system_categories_ensured"
private const val KEY_LEGACY_SYSTEM_CATEGORIES_FIXED = "legacy_system_categories_fixed_v2"

/** Nombres de categorías raíz gestionadas por el sistema (sembradas más abajo) — no seleccionables
 * en pickers de categoría ni creables a mano (CategoriesScreen.kt las bloquea por nombre). */
val RESERVED_CATEGORY_NAMES = setOf("Transferencias", "Ajustes", "Suscripciones", "Inversiones")

private data class SystemCategorySeed(val kind: String, val parentName: String, val childName: String, val icon: String, val color: Long)

/** Categorías de sistema — usadas como fallback cuando un movimiento no tiene una categoría
 * explícita (transferencias, ajustes de saldo, suscripciones/inversiones). Resueltas en runtime
 * por nombre vía systemCategoryLeaf() en Repositories.kt. Única fuente de verdad: reutilizada
 * tanto en el seed inicial como en [ensureSystemCategoriesExist] para instalaciones existentes. */
private val SYSTEM_CATEGORY_SEEDS = listOf(
    SystemCategorySeed("TRANSFER", "Transferencias", "Transferencia entre cuentas", "other", 0L),
    SystemCategorySeed("ADJUSTMENT", "Ajustes", "Ajuste de saldo", "other", 0L),
    SystemCategorySeed("EXPENSE", "Suscripciones", "Suscripción", "card", 0L),
    SystemCategorySeed("EXPENSE", "Inversiones", "Inversión", "investment", 0L),
)

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
                // color = 0 -> sin color propio, cae al fallback por hash de colorForCategory() (ver
                // FinanceComponents.kt), que ya reparte tonos variados y theme-aware. Cada categoría
                // recibe una subcategoría "General" — categoryId ahora exige siempre una hoja (ver
                // Transaction.sq), así que ninguna categoría puede quedar sin al menos una.
                val expenseSeeds = listOf(
                    Triple("Alimentación", "restaurant", 0L),
                    Triple("Transporte", "car", 0L),
                    Triple("Vivienda", "home", 0L),
                    Triple("Salud", "health", 0L),
                    Triple("Ocio", "games", 0L),
                )
                expenseSeeds.forEach { (name, icon, color) -> insertWithGeneralChild(db, name, icon, color, "EXPENSE") }

                val incomeSeeds = listOf(
                    Triple("Salario", "salary", 0L),
                    Triple("Freelance", "work", 0L),
                    Triple("Otros", "other", 0L),
                )
                incomeSeeds.forEach { (name, icon, color) -> insertWithGeneralChild(db, name, icon, color, "INCOME") }

                SYSTEM_CATEGORY_SEEDS.forEach { seed ->
                    insertWithGeneralChild(db, seed.parentName, seed.icon, seed.color, seed.kind, seed.childName)
                }

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
    ensureSystemCategoriesExist(db)
    reassignLegacySystemCategories(db)
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

/**
 * Las categorías de sistema (Suscripciones/Inversiones) se agregaron a [SYSTEM_CATEGORY_SEEDS]
 * después del seed inicial de muchas instalaciones ya existentes, que nunca vuelven a pasar por el
 * bloque `if (KEY_SEEDED == null)` de [seedIfEmpty]. Sin esto, systemCategoryLeaf() lanzaría al
 * resolver una categoría que nunca se sembró. Mismo patrón idempotente que [expandCurrenciesIfNeeded].
 */
private fun ensureSystemCategoriesExist(db: FinanzenDb) {
    if (db.settingQueries.get(KEY_SYSTEM_CATEGORIES_ENSURED).executeAsOneOrNull() != null) return
    db.transaction {
        SYSTEM_CATEGORY_SEEDS.forEach { seed ->
            val parentExists = db.categoryQueries.selectByKind(seed.kind).executeAsList().any { it.parentId == null && it.name == seed.parentName }
            if (!parentExists) {
                insertWithGeneralChild(db, seed.parentName, seed.icon, seed.color, seed.kind, seed.childName)
            }
        }
        db.settingQueries.put(KEY_SYSTEM_CATEGORIES_ENSURED, "true")
    }
}

/**
 * Corrige, una sola vez, inversiones/suscripciones (y sus transacciones de aporte/retiro/cobro) que
 * hayan quedado con una categoría elegida a mano por un formulario viejo, antes de que
 * addInvestment/addSubscription empezaran a forzar siempre la categoría de sistema. La regla no
 * admite excepciones por fila, así que es un UPDATE masivo, no una iteración condicional.
 */
private fun reassignLegacySystemCategories(db: FinanzenDb) {
    if (db.settingQueries.get(KEY_LEGACY_SYSTEM_CATEGORIES_FIXED).executeAsOneOrNull() != null) return
    db.transaction {
        val investmentsCategoryId = systemCategoryLeaf(db, "EXPENSE", "Inversiones", "Inversión")
        db.investmentQueries.reassignCategory(investmentsCategoryId)
        db.transactionQueries.reassignInvestmentCategory(investmentsCategoryId)
        db.transactionQueries.reassignWithdrawalCategory(investmentsCategoryId)

        val subscriptionsCategoryId = systemCategoryLeaf(db, "EXPENSE", "Suscripciones", "Suscripción")
        db.subscriptionQueries.reassignCategory(subscriptionsCategoryId)
        db.transactionQueries.reassignSubscriptionCategory(subscriptionsCategoryId)

        db.settingQueries.put(KEY_LEGACY_SYSTEM_CATEGORIES_FIXED, "true")
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
