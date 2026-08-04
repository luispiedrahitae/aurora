package com.finanzen.viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.data.AccountRepository
import com.finanzen.data.BudgetRepository
import com.finanzen.data.CategoryRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.db.FinanzenDb
import kotlin.test.Test
import kotlin.test.assertEquals

/** Edge cases QA (ver reporte de hallazgos): CategoriesViewModel no valida duplicados ni protege
 * contra borrar una categoría en uso. */
class CategoriesViewModelTest {
    private fun freshDb(): FinanzenDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanzenDb.Schema.create(driver)
        return FinanzenDb(driver)
    }

    @Test
    fun addDelViewModelNoValidaDuplicados() {
        // FIX (hallazgo medio #17): la validación de nombre duplicado ahora vive en CategoriesScreen
        // (mismo patrón "nameTaken" que AccountsTabScreen: se deriva en la UI y deshabilita el botón),
        // no en el ViewModel — igual que AccountsViewModel no valida nombres en su capa. Por eso
        // add() en este nivel sigue aceptando duplicados; sin la UI ya no es alcanzable por el
        // usuario real.
        val db = freshDb()
        val repo = CategoryRepository(db)
        val vm = CategoriesViewModel(repo)

        vm.add(name = "Comida", kind = "EXPENSE", parentId = null)
        vm.add(name = "Comida", kind = "EXPENSE", parentId = null)

        assertEquals(2, repo.byKind("EXPENSE").count { it.name == "Comida" })
    }

    @Test
    fun addCategoriaDeNivelSuperiorCreaSubcategoriaGeneralAutomatica() {
        // categoryId ahora exige siempre una hoja (Transaction.sq), así que ninguna categoría
        // nueva puede quedar sin al menos una subcategoría seleccionable.
        val db = freshDb()
        val repo = CategoryRepository(db)
        val vm = CategoriesViewModel(repo)

        vm.add(name = "Ocio", kind = "EXPENSE", parentId = null)

        val parent = repo.byKind("EXPENSE").first { it.parentId == null && it.name == "Ocio" }
        assertEquals(listOf("General"), repo.children(parent.id).map { it.name })
    }

    @Test
    fun deleteCategoriaConMovimientosYPresupuestoBorraTodoEnCascada() {
        // FIX (hallazgo alto #9): ni Budget.categoryId ni TransactionRow.categoryId cuentan con
        // enforcement de FKs garantizado en este proyecto (ver AccountsViewModel.delete()), así que
        // CategoryRepository.deleteCascade() borra a mano todo lo que referencia la categoría —
        // movimientos y presupuestos incluidos — en vez de dejarlos huérfanos o bloquear el borrado.
        val db = freshDb()
        val catRepo = CategoryRepository(db)
        val budgetRepo = BudgetRepository(db)
        val txRepo = TransactionRepository(db)
        val accountRepo = AccountRepository(db)
        catRepo.add(name = "Ocio", kind = "EXPENSE", parentId = null)
        val cat = catRepo.byKind("EXPENSE").first { it.name == "Ocio" }
        val sub = catRepo.children(cat.id).first()
        val accountId = accountRepo.add("Efectivo", "CASH", "USD")
        budgetRepo.setLimit(categoryId = sub.id, periodMonth = 202607, limitMinor = 50_000)
        txRepo.add(accountId, sub.id, 1_000, "USD", 0L, "Cine", "EXPENSE")

        val vm = CategoriesViewModel(catRepo)
        vm.delete(cat.id)

        assertEquals(true, catRepo.byKind("EXPENSE").none { it.name == "Ocio" }) // categoría y subcategoría borradas
        assertEquals(true, db.budgetQueries.selectAll().executeAsList().isEmpty()) // presupuesto, con ella
        assertEquals(true, txRepo.all().isEmpty()) // y el movimiento que la usaba
    }
}
