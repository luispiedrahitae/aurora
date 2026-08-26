package com.finanzen.data

import com.finanzen.db.Category
import com.finanzen.db.Investment
import com.finanzen.db.TransactionRow
import com.finanzen.domain.Money
import com.finanzen.platform.ReportLine
import com.finanzen.ui.format.formatFechaLarga
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/** Una fila de presupuesto ya agregada para el periodo del reporte (mes o suma de un año). */
data class BudgetSummaryRow(
    val categoryName: String,
    val parentName: String?,
    val limitMinor: Long,
    val spentMinor: Long,
)

/**
 * Construye contenido de reportes en formato textual/estructurado. Pure functions, sin I/O — el
 * escribir queda para [com.finanzen.platform.ReportExporter] por plataforma.
 */
object ReportBuilder {

    /** CSV RFC 4180-ish. Headers: id,date,account,category,subcategory,kind,amount,currency,note */
    fun transactionsCsv(rows: List<TransactionRow>, categories: List<Category>, accountNameById: Map<Long, String>): String {
        val catById = categories.associateBy { it.id }
        val sb = StringBuilder()
        sb.append("id,date,account,category,subcategory,kind,amount,currency,note\n")
        for (r in rows) {
            val leaf = catById[r.categoryId]
            val parent = leaf?.parentId?.let { catById[it] }
            val category = parent?.name ?: leaf?.name ?: ""
            val subcategory = if (parent != null) leaf?.name ?: "" else ""
            sb.append(r.id).append(',')
                .append(r.date).append(',')
                .append(csv(accountNameById[r.accountId] ?: "?")).append(',')
                .append(csv(category)).append(',')
                .append(csv(subcategory)).append(',')
                .append(r.kind).append(',')
                .append(Money(r.amountMinor, r.currency).format()).append(',')
                .append(r.currency).append(',')
                .append(csv(r.note))
                .append('\n')
        }
        return sb.toString()
    }

    /**
     * Resumen enriquecido del periodo (mes o año, ya filtrado por el caller), como líneas
     * estructuradas ([ReportLine]) — cada plataforma decide cómo dibujar cada rol.
     */
    fun periodSummaryPdfLines(
        periodLabel: String,
        rows: List<TransactionRow>,
        categories: List<Category>,
        currency: String,
        budgetRows: List<BudgetSummaryRow>,
        netWorthMinor: Long,
        openInvestments: List<Investment>,
    ): List<ReportLine> {
        val income = rows.filter { it.kind == "INCOME" }.sumOf { it.amountMinor }
        val expense = rows.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor }
        val net = income - expense
        val catName = categories.associate { it.id to it.name }
        val catById = categories.associateBy { it.id }

        /** Ingresos/gastos por categoría, anidado: la categoría padre con su total (% del total del
         * periodo) y debajo cada subcategoría con su % dentro de esa categoría (no del total). Las
         * transacciones siempre apuntan a una subcategoría (hoja), así que agrupar por `parentId`
         * reconstruye la jerarquía sin depender de un campo adicional. */
        fun breakdownLines(kind: String, total: Long, emptyMessage: String): List<ReportLine> {
            val byLeaf = rows.filter { it.kind == kind }
                .groupBy { it.categoryId }
                .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
            if (byLeaf.isEmpty()) return listOf(ReportLine.Row(emptyMessage))

            val byParent = byLeaf.entries.groupBy { (catId, _) -> catById[catId]?.parentId ?: catId }
                .mapValues { (_, leaves) -> leaves.sumOf { it.value } }
                .entries.sortedByDescending { it.value }

            val lines = mutableListOf<ReportLine>()
            byParent.forEach { (parentId, parentTotal) ->
                val parentPct = if (total > 0) (parentTotal * 100 / total) else 0L
                lines += ReportLine.Row(catName.getValue(parentId), "${money(parentTotal, currency)} ($parentPct%)", emphasis = true)

                byLeaf.entries
                    .filter { (catId, _) -> catId != parentId && (catById[catId]?.parentId ?: catId) == parentId }
                    .sortedByDescending { it.value }
                    .forEach { (catId, amount) ->
                        val childPct = if (parentTotal > 0) (amount * 100 / parentTotal) else 0L
                        lines += ReportLine.Row("   › ${catName.getValue(catId)}", "${money(amount, currency)} ($childPct%)")
                    }
            }
            return lines
        }

        val lines = mutableListOf<ReportLine>()
        lines += ReportLine.Title("Cauce — Resumen de $periodLabel")
        lines += ReportLine.Row("Generado el ${nowLabel()}")
        lines += ReportLine.Blank

        lines += ReportLine.Section("Balance del periodo")
        lines += ReportLine.Row("Ingresos", money(income, currency))
        lines += ReportLine.Row("Gastos", money(expense, currency))
        lines += ReportLine.Row("Neto", money(net, currency), emphasis = true)
        lines += ReportLine.Row("Patrimonio neto a cierre del periodo", money(netWorthMinor, currency), emphasis = true)
        lines += ReportLine.Blank

        lines += ReportLine.Section("Ingresos por categoría")
        lines += breakdownLines("INCOME", income, "(sin ingresos en el periodo)")
        lines += ReportLine.Blank

        lines += ReportLine.Section("Gastos por categoría")
        lines += breakdownLines("EXPENSE", expense, "(sin gastos en el periodo)")
        lines += ReportLine.Blank

        lines += ReportLine.Section("Presupuestos del periodo")
        val budgeted = budgetRows.filter { it.limitMinor > 0 }
        if (budgeted.isEmpty()) {
            lines += ReportLine.Row("(sin presupuestos configurados)")
        } else {
            // Categorías primero, luego subcategorías — mismo formato de label de siempre
            // ("Ocio" / "Ocio › Streaming"), solo se reordena para no mezclar los dos niveles.
            val categoryLevel = budgeted.filter { it.parentName == null }.sortedByDescending { it.spentMinor }
            val subcategoryLevel = budgeted.filter { it.parentName != null }.sortedByDescending { it.spentMinor }
            (categoryLevel + subcategoryLevel).forEach { b ->
                val baseLabel = b.parentName?.let { "$it › ${b.categoryName}" } ?: b.categoryName
                val pct = if (b.limitMinor > 0) (b.spentMinor * 100 / b.limitMinor) else 0L
                val over = b.spentMinor > b.limitMinor
                val label = if (over) "$baseLabel $WARNING_SYMBOL" else baseLabel
                lines += ReportLine.Row(
                    label,
                    "${money(b.spentMinor, currency)} de ${money(b.limitMinor, currency)} ($pct%)",
                    emphasis = over,
                )
            }
        }
        lines += ReportLine.Blank

        lines += ReportLine.Section("Inversiones abiertas")
        if (openInvestments.isEmpty()) {
            lines += ReportLine.Row("(sin inversiones abiertas)")
        } else {
            openInvestments.forEach { inv ->
                lines += ReportLine.Row(inv.name, money(inv.amountMinor, inv.currency))
            }
            val total = openInvestments.sumOf { it.amountMinor }
            lines += ReportLine.Row("Total invertido", money(total, currency), emphasis = true)
        }
        lines += ReportLine.Divider

        lines += ReportLine.Row("Total de movimientos del periodo", rows.size.toString())

        if (budgeted.any { it.spentMinor > it.limitMinor }) {
            lines += ReportLine.Blank
            lines += ReportLine.Row("$WARNING_SYMBOL Presupuesto excedido en el periodo")
        }
        return lines
    }

    private const val WARNING_SYMBOL = "⚠"

    private fun money(amountMinor: Long, currency: String): String = "${Money(amountMinor, currency).format()} $currency"

    private fun nowLabel(): String = formatFechaLarga(Clock.System.todayIn(TimeZone.currentSystemDefault()))

    private fun csv(s: String): String {
        if (s.isEmpty()) return ""
        val needsQuote = s.contains(',') || s.contains('"') || s.contains('\n')
        val escaped = s.replace("\"", "\"\"")
        return if (needsQuote) "\"$escaped\"" else escaped
    }
}
