package com.finanzen.data

import com.finanzen.db.Category
import com.finanzen.db.TransactionRow
import com.finanzen.domain.Money

/**
 * Construye contenido de reportes en formato textual. Pure functions, sin I/O — el escribir queda
 * para [com.finanzen.platform.ReportExporter] por plataforma.
 */
object ReportBuilder {

    /** CSV RFC 4180-ish. Headers: id,date,account,category,kind,amount,currency,note */
    fun transactionsCsv(rows: List<TransactionRow>, categoryNameById: Map<Long, String>, accountNameById: Map<Long, String>): String {
        val sb = StringBuilder()
        sb.append("id,date,account,category,kind,amount,currency,note\n")
        for (r in rows) {
            sb.append(r.id).append(',')
                .append(r.date).append(',')
                .append(csv(accountNameById[r.accountId] ?: "?")).append(',')
                .append(csv(categoryNameById[r.categoryId ?: -1L] ?: "")).append(',')
                .append(r.kind).append(',')
                .append(Money(r.amountMinor, r.currency).format()).append(',')
                .append(r.currency).append(',')
                .append(csv(r.note))
                .append('\n')
        }
        return sb.toString()
    }

    /** Resumen mensual en líneas. Cada línea es texto plano (sin formato), se renderea como PDF en cada plataforma. */
    fun monthlySummaryLines(
        rows: List<TransactionRow>,
        categories: List<Category>,
        currency: String,
    ): List<String> {
        val income = rows.filter { it.kind == "INCOME" }.sumOf { it.amountMinor }
        val expense = rows.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor }
        val net = income - expense
        val catName = categories.associate { it.id to it.name }

        val byCat = rows.filter { it.kind == "EXPENSE" }
            .groupBy { it.categoryId }
            .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
            .entries.sortedByDescending { it.value }

        val lines = mutableListOf<String>()
        lines += "FinanZen — Resumen del periodo"
        lines += ""
        lines += "Ingresos: ${Money(income, currency).format()} $currency"
        lines += "Gastos:   ${Money(expense, currency).format()} $currency"
        lines += "Balance:  ${Money(net, currency).format()} $currency"
        lines += ""
        lines += "Top categorías de gasto:"
        if (byCat.isEmpty()) {
            lines += "  (sin datos)"
        } else {
            byCat.take(10).forEachIndexed { i, (catId, amount) ->
                val name = catName[catId ?: -1L] ?: "Sin categoría"
                val pct = if (expense > 0) (amount * 100 / expense) else 0L
                lines += "  ${i + 1}. $name — ${Money(amount, currency).format()} $currency ($pct%)"
            }
        }
        lines += ""
        lines += "Total de transacciones: ${rows.size}"
        return lines
    }

    private fun csv(s: String): String {
        if (s.isEmpty()) return ""
        val needsQuote = s.contains(',') || s.contains('"') || s.contains('\n')
        val escaped = s.replace("\"", "\"\"")
        return if (needsQuote) "\"$escaped\"" else escaped
    }
}
