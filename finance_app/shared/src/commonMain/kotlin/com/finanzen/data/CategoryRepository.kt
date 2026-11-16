package com.finanzen.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.finanzen.db.Account
import com.finanzen.db.Budget
import com.finanzen.db.Card
import com.finanzen.db.Category
import com.finanzen.db.Currency
import com.finanzen.db.FinanzenDb
import com.finanzen.db.InstallmentPlan
import com.finanzen.db.Investment
import com.finanzen.db.Subscription
import com.finanzen.db.TransactionRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val db: FinanzenDb) {
    fun observeAll(): Flow<List<Category>> = db.categoryQueries.selectAll().asFlow().mapToList(Dispatchers.Default)

    fun all(): List<Category> = db.categoryQueries.selectAll().executeAsList()

    fun byId(id: Long): Category? = db.categoryQueries.selectById(id).executeAsOneOrNull()

    fun byKind(kind: String): List<Category> = db.categoryQueries.selectByKind(kind).executeAsList()

    fun children(parentId: Long): List<Category> = db.categoryQueries.selectChildren(parentId).executeAsList()

    /** Resuelve una subcategoría de sistema (Transferencias/Ajustes/Suscripciones, sembradas por
     * DefaultSeed.kt) por nombre — fallback cuando el usuario no elige una categoría explícita. */
    fun systemLeaf(kind: String, parentName: String, childName: String): Long = systemCategoryLeaf(db, kind, parentName, childName)

    /** Toda categoría de nivel superior recibe una subcategoría "General" a la vez, para que
     * siempre exista al menos una hoja seleccionable (categoryId ahora exige una hoja). */
    fun add(name: String, kind: String, parentId: Long?, icon: String = "", color: Long = 0): Unit = db.transaction {
        db.categoryQueries.insert(parentId = parentId, name = name, icon = icon, color = color, kind = kind)
        if (parentId == null) {
            val newId = db.categoryQueries.lastInsertRowId().executeAsOne()
            db.categoryQueries.insert(parentId = newId, name = "General", icon = icon, color = color, kind = kind)
        }
    }

    /** Igual que [add] pero devuelve el id nuevo — usado para auto-seleccionar una subcategoría recién creada. */
    fun addAndGetId(name: String, kind: String, parentId: Long?, icon: String = "", color: Long = 0): Long {
        db.categoryQueries.insert(parentId = parentId, name = name, icon = icon, color = color, kind = kind)
        return db.categoryQueries.lastInsertRowId().executeAsOne()
    }

    fun update(id: Long, name: String, kind: String, parentId: Long?, icon: String = "", color: Long = 0) = db.categoryQueries.update(parentId = parentId, name = name, icon = icon, color = color, kind = kind, id = id)

    /** Borra la categoría (y, si es de nivel superior, sus subcategorías hijas) junto con todo lo
     * que la referencia: movimientos, presupuestos, suscripciones, inversiones y planes de cuotas.
     * SQLite no garantiza el enforcement de FKs en esta app (ver Platform.*.kt), así que la cascada
     * se hace a mano — mismo patrón que ya usaba CategoriesViewModel para Budget. */
    fun deleteCascade(id: Long) {
        val affectedIds = listOf(id) + db.categoryQueries.selectChildren(id).executeAsList().map { it.id }
        db.transaction {
            affectedIds.forEach { catId ->
                db.transactionQueries.deleteByCategory(catId)
                db.budgetQueries.deleteByCategory(catId)
                db.subscriptionQueries.deleteByCategory(catId)
                db.investmentQueries.deleteByCategory(catId)
                db.installmentPlanQueries.deleteByCategory(catId)
            }
            // Hijas primero (parentId apunta a la categoría que se borra al final).
            affectedIds.filter { it != id }.forEach { db.categoryQueries.delete(it) }
            db.categoryQueries.delete(id)
        }
    }
}

/** Resuelve el id de una subcategoría de sistema (Transferencias/Ajustes/Suscripciones, sembradas
 * por DefaultSeed.kt) por nombre de categoría padre + subcategoría. */
internal fun systemCategoryLeaf(db: FinanzenDb, kind: String, parentName: String, childName: String): Long {
    val parent = db.categoryQueries.selectByKind(kind).executeAsList().first { it.parentId == null && it.name == parentName }
    return db.categoryQueries.selectChildren(parent.id).executeAsList().first { it.name == childName }.id
}
