package com.finanzen.ui.components

import com.finanzen.ui.theme.LightFinanceColors
import kotlin.test.Test
import kotlin.test.assertEquals

class FinanceComponentsTest {
    // FIX: TransactionsScreen y CalendarScreen negaban el monto de ADJUSTMENT como si fuera un
    // gasto (ya trae el signo correcto) y lo coloreaban siempre de rojo -- AccountsTabScreen sí lo
    // hacía bien. Esta función centraliza esa lógica para que las 3 pantallas queden consistentes.
    private val finance = LightFinanceColors

    @Test
    fun expenseSeNiegaYSeColoreaDeRojo() {
        val (signed, color) = signedAmountAndColor("EXPENSE", 1_000, finance)
        assertEquals(-1_000, signed)
        assertEquals(finance.expense, color)
    }

    @Test
    fun incomeMantienePositivoYSeColoreaDeVerde() {
        val (signed, color) = signedAmountAndColor("INCOME", 1_000, finance)
        assertEquals(1_000, signed)
        assertEquals(finance.income, color)
    }

    @Test
    fun adjustmentPositivoNoSeNiegaYSeColoreaComoIngreso() {
        val (signed, color) = signedAmountAndColor("ADJUSTMENT", 500, finance)
        assertEquals(500, signed)
        assertEquals(finance.income, color)
    }

    @Test
    fun adjustmentNegativoNoSeNiegaYSeColoreaComoGasto() {
        val (signed, color) = signedAmountAndColor("ADJUSTMENT", -500, finance)
        assertEquals(-500, signed)
        assertEquals(finance.expense, color)
    }
}
