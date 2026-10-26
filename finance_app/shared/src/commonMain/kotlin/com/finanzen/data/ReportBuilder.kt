package com.finanzen.data

import com.finanzen.db.Category
import com.finanzen.db.Investment
import com.finanzen.db.Subscription
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

    /** CSV RFC 4180-ish, en español para lectura directa en hojas de cálculo. Headers:
     * Fecha,Tipo,Categoría,Subcategoría,Cuenta,Monto,Moneda,Nota (sin id: no hay reimportación). */
    fun transactionsCsv(
        rows: List<TransactionRow>,
        categories: List<Category>,
        accountNameById: Map<Long, String>,
        subscriptions: List<Subscription> = emptyList(),
        investments: List<Investment> = emptyList(),
    ): String {
        val catById = categories.associateBy { it.id }
        val subById = subscriptions.associateBy { it.id }
        val invById = investments.associateBy { it.id }
        val sb = StringBuilder()
        sb.append("Fecha,Tipo,Categoría,Subcategoría,Cuenta,Monto,Moneda,Nota\n")
        for (r in rows) {
            val leaf = catById[r.categoryId]
            val parent = leaf?.parentId?.let { catById[it] }
            val category = parent?.name ?: leaf?.name ?: ""
            val subcategory = when {
                r.subscriptionId != null -> subById[r.subscriptionId]?.name ?: leaf?.name ?: ""
                r.investmentId != null -> invById[r.investmentId]?.name ?: leaf?.name ?: ""
                parent != null -> leaf?.name ?: ""
                else -> ""
            }
            sb.append(r.date).append(',')
                .append(kindLabel(r.kind)).append(',')
                .append(csv(category)).append(',')
                .append(csv(subcategory)).append(',')
                .append(csv(accountNameById[r.accountId] ?: "?")).append(',')
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
        subscriptions: List<Subscription> = emptyList(),
        investments: List<Investment> = emptyList(),
    ): List<ReportLine> {
        val income = rows.filter { it.kind == "INCOME" }.sumOf { it.amountMinor }
        val expense = rows.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor }
        val net = income - expense
        val catName = categories.associate { it.id to it.name }
        val catById = categories.associateBy { it.id }
        val subscriptionById = subscriptions.associateBy { it.id }
        val investmentById = investments.associateBy { it.id }

        /** Ingresos/gastos por categoría, anidado: la categoría padre con su total y debajo cada
         * subcategoría. Las transacciones siempre apuntan a una subcategoría (hoja), así que agrupar
         * por `parentId` reconstruye la jerarquía sin depender de un campo adicional.
         *
         * Caso especial "Suscripciones"/"Inversiones": todas comparten una única subcategoría de
         * sistema (ver [DefaultSeed]), así que agruparían en una sola fila genérica. En su lugar, se
         * desglosa por suscripción/inversión individual (nombre propio), igual que
         * [com.finanzen.viewmodel.DashboardViewModel.computeCategoryBreakdown] hace para suscripciones
         * en el dashboard. */
        fun breakdownLines(kind: String, emptyMessage: String): List<ReportLine> {
            val tone = if (kind == "INCOME") ReportLine.RowTone.Positive else ReportLine.RowTone.Negative
            val filtered = rows.filter { it.kind == kind }
            if (filtered.isEmpty()) return listOf(ReportLine.Row(emptyMessage))

            val byParent = filtered.groupBy { catById[it.categoryId]?.parentId ?: it.categoryId }
                .entries.sortedByDescending { (_, list) -> list.sumOf { it.amountMinor } }

            val lines = mutableListOf<ReportLine>()
            byParent.forEach { (parentId, parentRows) ->
                val parentName = catName.getValue(parentId)
                val parentTotal = parentRows.sumOf { it.amountMinor }
                lines += ReportLine.Row(parentName, money(parentTotal, currency), emphasis = true, tone = tone)

                val childTotals: Map<String, Long> = when (parentName) {
                    "Suscripciones" -> parentRows.groupBy { tx -> tx.subscriptionId?.let { subscriptionById[it] }?.name ?: catById[tx.categoryId]?.name ?: "Suscripción" }
                        .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
                    "Inversiones" -> parentRows.groupBy { tx -> tx.investmentId?.let { investmentById[it] }?.name ?: catById[tx.categoryId]?.name ?: "Inversión" }
                        .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
                    else -> parentRows.groupBy { it.categoryId }
                        .filterKeys { it != parentId }
                        .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
                        .mapKeys { (catId, _) -> catName.getValue(catId) }
                }
                childTotals.entries.sortedByDescending { it.value }.forEach { (name, amount) ->
                    lines += ReportLine.Row("   › $name", money(amount, currency), tone = tone)
                }
            }
            return lines
        }

        val lines = mutableListOf<ReportLine>()
        lines += ReportLine.Title("Sage — Resumen de $periodLabel")
        lines += ReportLine.Row("Generado el ${nowLabel()}")
        lines += ReportLine.Blank

        lines += ReportLine.Section("Balance del periodo")
        lines += ReportLine.Row("Ingresos", money(income, currency), tone = ReportLine.RowTone.Positive)
        lines += ReportLine.Row("Gastos", money(expense, currency), tone = ReportLine.RowTone.Negative)
        lines += ReportLine.Row(
            "Neto",
            money(net, currency),
            emphasis = true,
            tone = if (net >= 0) ReportLine.RowTone.Positive else ReportLine.RowTone.Negative,
        )
        lines += ReportLine.Row("Patrimonio", money(netWorthMinor, currency), emphasis = true)
        lines += ReportLine.Blank

        lines += ReportLine.Section("Ingresos por categoría")
        lines += breakdownLines("INCOME", "(sin ingresos en el periodo)")
        lines += ReportLine.Blank

        lines += ReportLine.Section("Gastos por categoría")
        lines += breakdownLines("EXPENSE", "(sin gastos en el periodo)")
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
                val over = b.spentMinor > b.limitMinor
                val label = if (over) "$baseLabel $WARNING_SYMBOL" else baseLabel
                lines += ReportLine.Row(
                    label,
                    "${money(b.spentMinor, currency)} de ${money(b.limitMinor, currency)}",
                    emphasis = over,
                    tone = if (over) ReportLine.RowTone.Warning else ReportLine.RowTone.Neutral,
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
            lines += ReportLine.Row("$WARNING_SYMBOL Presupuesto excedido en el periodo", tone = ReportLine.RowTone.Warning)
        }
        return lines
    }

    private const val WARNING_SYMBOL = "⚠"

    private fun kindLabel(kind: String): String = when (kind) {
        "INCOME" -> "Ingreso"
        "EXPENSE" -> "Gasto"
        "TRANSFER" -> "Transferencia"
        else -> kind
    }

    private fun money(amountMinor: Long, currency: String): String = "${Money(amountMinor, currency).format()} $currency"

    private fun nowLabel(): String = formatFechaLarga(Clock.System.todayIn(TimeZone.currentSystemDefault()))

    private fun csv(s: String): String {
        if (s.isEmpty()) return ""
        val needsQuote = s.contains(',') || s.contains('"') || s.contains('\n')
        val escaped = s.replace("\"", "\"\"")
        return if (needsQuote) "\"$escaped\"" else escaped
    }
}
