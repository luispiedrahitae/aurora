package com.finanzen.viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.data.BudgetRepository
import com.finanzen.data.CategoryRepository
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
        val vm = CategoriesViewModel(repo, BudgetRepository(db))

        vm.add(name = "Comida", kind = "EXPENSE", parentId = null)
        vm.add(name = "Comida", kind = "EXPENSE", parentId = null)

        assertEquals(2, repo.byKind("EXPENSE").count { it.name == "Comida" })
    }

    @Test
    fun deleteCategoriaConPresupuestoActivoBorraElPresupuestoEnCascada() {
        // FIX (hallazgo alto #9): Budget.categoryId sí declara ON DELETE CASCADE en el schema, pero
        // el enforcement de FKs de SQLite no está garantizado en este proyecto (ver
        // AccountsViewModel.delete()), así que CategoriesViewModel.delete() ahora borra los
        // presupuestos de la categoría a mano antes de borrarla, en vez de dejarlos huérfanos.
        val db = freshDb()
        val catRepo = CategoryRepository(db)
        val budgetRepo = BudgetRepository(db)
        catRepo.add(name = "Ocio", kind = "EXPENSE", parentId = null)
        val cat = catRepo.byKind("EXPENSE").first()
        budgetRepo.setLimit(categoryId = cat.id, periodMonth = 202607, limitMinor = 50_000)

        val vm = CategoriesViewModel(catRepo, budgetRepo)
        vm.delete(cat.id)

        assertEquals(true, catRepo.byKind("EXPENSE").isEmpty()) // la categoría se borró
        assertEquals(true, db.budgetQueries.selectAll().executeAsList().isEmpty()) // y su presupuesto, con ella
    }
}
