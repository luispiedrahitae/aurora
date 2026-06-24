package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import com.finanzen.data.AccountRepository
import com.finanzen.data.CategoryRepository
import com.finanzen.data.ReportBuilder
import com.finanzen.db.FinanzenDb
import com.finanzen.platform.ReportExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ExportStatus(val message: String, val isError: Boolean)

class ReportsViewModel(
    private val db: FinanzenDb,
    private val accountRepo: AccountRepository,
    private val categoryRepo: CategoryRepository,
    private val exporter: ReportExporter,
) : ViewModel() {

    private val _status = MutableStateFlow<ExportStatus?>(null)
    val status: StateFlow<ExportStatus?> = _status.asStateFlow()

    fun exportTransactionsCsv() {
        val rows = db.transactionQueries.selectAll().executeAsList()
        val cats = db.categoryQueries.selectAll().executeAsList().associate { it.id to it.name }
        val accs = accountRepo.all().associate { it.id to it.name }
        val csv = ReportBuilder.transactionsCsv(rows, cats, accs)
        val result = exporter.saveCsv("finanzen-transacciones", csv)
        _status.value = ExportStatus(result, isError = result.startsWith("error") || result.startsWith("stub"))
    }

    fun exportMonthlySummaryPdf() {
        val rows = db.transactionQueries.selectAll().executeAsList()
        val categories = db.categoryQueries.selectAll().executeAsList()
        val currency = rows.firstOrNull()?.currency ?: "USD"
        val lines = ReportBuilder.monthlySummaryLines(rows, categories, currency)
        val result = exporter.savePdf("finanzen-resumen", lines)
        _status.value = ExportStatus(result, isError = result.startsWith("error") || result.startsWith("stub"))
    }

    fun clearStatus() {
        _status.value = null
    }
}
