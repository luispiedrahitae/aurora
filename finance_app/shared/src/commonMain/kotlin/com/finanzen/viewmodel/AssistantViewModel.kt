package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.AccountRepository
import com.finanzen.data.AssistantRepository
import com.finanzen.data.BudgetRepository
import com.finanzen.data.CURRENCY_LOCALE_INFO
import com.finanzen.data.CategoryRepository
import com.finanzen.data.CurrencyLocaleInfo
import com.finanzen.data.DEFAULT_LOCALE_INFO
import com.finanzen.data.SettingsRepository
import com.finanzen.data.SubscriptionRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.db.Account
import com.finanzen.db.Budget
import com.finanzen.db.Category
import com.finanzen.db.Subscription
import com.finanzen.db.TransactionRow
import com.finanzen.domain.Money
import com.finanzen.platform.DownloadState
import com.finanzen.ui.format.diaSemana
import com.finanzen.ui.format.monthPeriod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

enum class ModelState { CHECKING, UNSUPPORTED_DEVICE, NOT_DOWNLOADED, DOWNLOADING, LOADING, READY, CORRUPT, ERROR }
enum class Role { USER, ASSISTANT }
data class ChatMessage(val id: Long, val role: Role, val text: String)

data class AssistantUiState(
    val modelState: ModelState = ModelState.CHECKING,
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val error: String? = null,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
)

/**
 * Asistente conversacional 100% on-device. Sin `combine(...).stateIn(...)` reactivo como el resto
 * de los ViewModels: cada pregunta relee los repositorios una sola vez (`.first()`) para armar un
 * contexto financiero fresco — no tiene sentido recomputar en cada emisión de la DB mientras el
 * usuario escribe. Ver [AssistantRepository] para el ciclo de vida del motor de inferencia.
 */
class AssistantViewModel(
    private val repo: AssistantRepository,
    private val txRepo: TransactionRepository,
    private val accountRepo: AccountRepository,
    private val categoryRepo: CategoryRepository,
    private val budgetRepo: BudgetRepository,
    private val subscriptionRepo: SubscriptionRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AssistantUiState())
    val state: StateFlow<AssistantUiState> = _state

    private var nextMessageId = 0L
    private fun nextId(): Long = nextMessageId++

    /**
     * Chequea el dispositivo y, si ya hay un modelo descargado, lo carga directo; si no, se
     * suscribe al progreso de la descarga (ver [ModelDownloader][com.finanzen.platform.ModelDownloader])
     * para reflejarlo en vivo. Llamar al entrar a la pantalla.
     */
    fun onScreenEntered() {
        viewModelScope.launch {
            _state.update { it.copy(modelState = ModelState.CHECKING, error = null) }
            if (!repo.deviceCheck().meetsRequirements) {
                _state.update { it.copy(modelState = ModelState.UNSUPPORTED_DEVICE) }
                return@launch
            }
            if (repo.modelPath() != null) {
                loadModel()
                return@launch
            }
            repo.downloadState().collect { download ->
                when (download) {
                    DownloadState.Idle -> _state.update { it.copy(modelState = ModelState.NOT_DOWNLOADED) }
                    is DownloadState.InProgress -> _state.update {
                        it.copy(
                            modelState = ModelState.DOWNLOADING,
                            downloadedBytes = download.bytesDownloaded,
                            totalBytes = download.totalBytes,
                        )
                    }
                    DownloadState.Completed -> {
                        val alreadyHandled = _state.value.modelState == ModelState.READY || _state.value.modelState == ModelState.LOADING
                        if (!alreadyHandled) loadModel()
                    }
                    is DownloadState.Failed -> _state.update { it.copy(modelState = ModelState.ERROR, error = download.message) }
                }
            }
        }
    }

    /**
     * Libera el motor de inferencia. Llamar al salir de la pantalla o al pasar a segundo plano.
     * NO cancela la descarga de WorkManager: la descarga sigue en segundo plano aunque el usuario
     * navegue a otra pestaña, y el chat solo se habilita cuando termina y el modelo carga.
     */
    fun onScreenLeft() = repo.release()

    /** Arranca la descarga automática del modelo (WorkManager, ver [AssistantRepository]). */
    fun startDownload() {
        repo.startDownload()
        settingsRepo.setAssistantOptIn(true)
    }

    private suspend fun loadModel() {
        _state.update { it.copy(modelState = ModelState.LOADING) }
        val result = repo.loadModel()
        if (result.isSuccess) {
            settingsRepo.setAssistantModelState(SettingsRepository.STATE_READY)
            _state.update { it.copy(modelState = ModelState.READY) }
        } else {
            settingsRepo.setAssistantModelState(SettingsRepository.STATE_CORRUPT)
            repo.deleteModel()
            _state.update { it.copy(modelState = ModelState.CORRUPT, error = "El modelo importado no se pudo cargar.") }
        }
    }

    fun ask(question: String) {
        if (question.isBlank() || _state.value.isGenerating || _state.value.modelState != ModelState.READY) return
        val userMsg = ChatMessage(nextId(), Role.USER, question.trim())
        val assistantId = nextId()
        _state.update {
            it.copy(messages = it.messages + userMsg + ChatMessage(assistantId, Role.ASSISTANT, ""), isGenerating = true, error = null)
        }
        viewModelScope.launch {
            val prompt = buildPrompt(question.trim())
            val temperature = inferTemperature(question.trim())
            val result = withTimeoutOrNull(GENERATION_TIMEOUT_MS) {
                repo.ask(prompt, temperature) { partial ->
                    _state.update { s ->
                        s.copy(messages = s.messages.map { if (it.id == assistantId) it.copy(text = it.text + partial) else it })
                    }
                }
            }
            _state.update {
                if (result == null || result.isFailure) {
                    it.copy(isGenerating = false, error = "No se pudo generar una respuesta. Intenta de nuevo.")
                } else {
                    it.copy(isGenerating = false)
                }
            }
        }
    }

    private suspend fun buildPrompt(question: String): String {
        val txs = txRepo.observeAll().first()
        val accounts = accountRepo.observeAll().first()
        val cats = categoryRepo.observeAll().first()
        val budgets = budgetRepo.observeAll().first()
        val subscriptions = subscriptionRepo.observeActive().first()
        val baseCurrency = settingsRepo.baseCurrency()
        val locale = CURRENCY_LOCALE_INFO[baseCurrency] ?: DEFAULT_LOCALE_INFO
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val keyword = extractKeyword(question, cats)
        val context = buildFinancialContext(txs, accounts, cats, budgets, subscriptions, today, baseCurrency, locale, keyword)
        val history = recentHistoryText()
        return buildString {
            append(SYSTEM_PROMPT)
            append("\n\nDATOS\n")
            append(context)
            if (history.isNotBlank()) {
                append("\n\nCONVERSACIÓN_PREVIA\n")
                append(history)
            }
            append("\n\nPREGUNTA\n")
            append(question)
        }
    }

    private fun recentHistoryText(): String = _state.value.messages
        .filter { it.text.isNotBlank() }
        .dropLast(1) // el placeholder de la respuesta en curso, todavía vacío
        .takeLast(MAX_HISTORY_TURNS * 2)
        .joinToString("\n") { "${if (it.role == Role.USER) "Usuario" else "Asistente"}: ${it.text}" }

    companion object {
        internal const val MAX_HISTORY_TURNS = 3
        internal const val MAX_CONTEXT_CHARS = 6000
        internal const val TOP_CATEGORIES = 5
        private const val GENERATION_TIMEOUT_MS = 30_000L

        /**
         * Ver plan de implementación del asistente IA (Fase 4). Reglas no negociables: solo usa
         * DATOS, nunca inventa cifras, responde únicamente lo preguntado, tono neutral, sin
         * disclaimers, breve, sin conversión de moneda, se niega a responder fuera de alcance.
         */
        internal const val SYSTEM_PROMPT = """
Eres el asistente financiero de FinanZen. Te ejecutas completamente en el dispositivo del
usuario, sin conexión a internet.

REGLAS ESTRICTAS (no negociables):
1. Usa ÚNICAMENTE los datos que aparecen en la sección DATOS de este mensaje. No uses
   conocimiento general ni inventes cifras, cuentas, categorías o fechas que no estén ahí. Esto
   es sobre hechos específicos del usuario — no te prohíbe dar sugerencias o ideas financieras
   generales cuando el usuario las pide explícitamente (ver regla 3); en ese caso, apóyalas en
   las cifras reales de DATOS en vez de inventar montos nuevos.
2. Si la pregunta necesita un dato que NO aparece en DATOS, responde exactamente que no tienes
   esa información disponible. Nunca estimes, redondees "a ojo" ni completes con un supuesto.
   Si la pregunta combina algo que sí está en DATOS con algo que no, responde la parte que sí
   puedes calcular y aclara en una frase qué parte no puedes determinar — no te niegues por
   completo si una parte de la pregunta es respondible.
3. Responde ÚNICAMENTE lo que se pregunta. No agregues explicaciones no solicitadas, contexto
   adicional, consejos financieros generales, opiniones ni resúmenes de lo que el usuario ya
   sabe, a menos que lo pida explícitamente (por ejemplo: "¿qué me recomiendas?", "¿cómo voy con
   mis finanzas?", "¿qué me dirías si fueras mi asesor?", "dame ideas para ahorrar/reducir
   gastos"). En esos casos no te niegues por no tener una sección de "ideas" en DATOS — genera
   sugerencias de sentido común financiero apoyadas en las cifras reales del usuario (por
   ejemplo, la categoría con más gasto o un presupuesto superado).
4. No juzgues los gastos del usuario ni uses un tono de alarma, culpa o vergüenza (nunca digas
   cosas como "gastaste demasiado", "deberías ahorrar más" o "estás fallando"). Reporta los
   números con neutralidad, igual que el resto de la app. Si el usuario pide explícitamente una
   evaluación de su situación financiera (por ejemplo: "¿estoy gastando demasiado?", "¿administro
   bien mi dinero?", "¿cuál es mi mayor error?"), sí puedes responder con una evaluación —
   constrúyela siempre con hechos concretos de DATOS (una categoría que superó su presupuesto, la
   categoría que más creció respecto al mes anterior, gastos que superan los ingresos) y en tono
   calmado e informativo, nunca alarmista ni de culpa. Nunca agregues una evaluación si no te la
   pidieron.
5. No te describas como "una IA" ni des disclaimers ("como modelo de lenguaje..."). No expliques
   cómo llegaste a la respuesta salvo que se te pida.
6. Sé directo y breve: una o dos frases cuando el dato lo permita. No repitas la pregunta. Si el
   usuario pide explícitamente otro estilo o extensión (más detalle, una explicación simple "como
   si tuviera 15 años", etc.), respeta ese pedido en vez de la brevedad por defecto.
7. Los montos ya vienen formateados en la moneda base del usuario. Nunca hagas conversión de
   divisas ni asumas una tasa de cambio.
8. Si el usuario solo saluda o inicia la conversación sin una pregunta financiera concreta,
   responde con un saludo breve y ofrece ayuda con sus finanzas — no lo trates como una pregunta
   fuera de alcance. Si la pregunta sí tiene contenido pero no tiene relación con los datos
   financieros entregados, dilo brevemente y no intentes responder de otra forma.
9. Responde siempre en prosa natural y conversacional, como lo haría una persona. Nunca copies
   los encabezados de la sección DATOS (RESUMEN_MES, CUENTAS, PRESUPUESTOS, etc.) ni el formato
   de línea "Clave: valor" tal cual — integra las cifras en frases legibles para un humano.

FORMATO DE LOS DATOS
Los datos llegan en secciones con encabezados en mayúsculas, por ejemplo:
RESUMEN_MES: ingresos, gastos, ahorro del mes actual
CUENTAS: nombre y saldo de cada cuenta
PRESUPUESTOS: límite y gasto por categoría
SUSCRIPCIONES: próximos cobros
GASTO_CATEGORIA_MES_ACTUAL, GASTO_CATEGORIA_MES_ANTERIOR_VS_ACTUAL, GASTO_CATEGORIA_ULTIMOS_3_MESES,
GASTO_PROMEDIO_MENSUAL, TOP_10_COMPRAS_ANIO, GASTO_POR_DIA_SEMANA: agregados ya calculados, listados
de mayor a menor cuando aplica
MOVIMIENTOS: transacciones específicas que coinciden con la pregunta (si aplica)
Trata cada valor como el único registro válido de esa cifra.
"""

        /**
         * Agregación pura, reutiliza [DashboardViewModel.computeDashboard],
         * [BudgetsViewModel.computeBudgets] y [AnalysisViewModel.computeSubscriptionMonthlyCost] —
         * sin RAG ni recomputar lo que esas funciones ya resuelven. [keyword], si viene, agrega una
         * sección MOVIMIENTOS con transacciones puntuales que coinciden (ver [findMatchingTransactions]).
         */
        internal fun buildFinancialContext(
            txs: List<TransactionRow>,
            accounts: List<Account>,
            cats: List<Category>,
            budgets: List<Budget>,
            subscriptions: List<Subscription>,
            today: LocalDate,
            baseCurrency: String,
            locale: CurrencyLocaleInfo = DEFAULT_LOCALE_INFO,
            keyword: String? = null,
        ): String {
            val firstOfMonth = LocalDate(today.year, today.month, 1)
            val dashboard = DashboardViewModel.computeDashboard(txs, accounts, cats, budgets, firstOfMonth, today.year, baseCurrency, locale)
            val budgetsData = BudgetsViewModel.computeBudgets(budgets, cats, txs, monthPeriod(firstOfMonth))
            val activeSubs = subscriptions.filter { it.active == 1L }
            val subsMonthlyCost = AnalysisViewModel.computeSubscriptionMonthlyCost(activeSubs)
            val monthIdx = (today.monthNumber - 1).coerceIn(0, 11)
            val monthIncome = dashboard.incomeByMonth.getOrNull(monthIdx)?.amountMinor ?: 0L
            val monthExpense = dashboard.expenseByMonth.getOrNull(monthIdx)?.amountMinor ?: 0L
            val monthSavingsRate = DashboardViewModel.computeSavingsRate(monthIncome, monthExpense)

            fun money(minor: Long) = Money(minor, baseCurrency).format(2)

            val sb = StringBuilder()
            sb.appendLine("RESUMEN_MES")
            sb.appendLine("Ingresos: ${money(monthIncome)}")
            sb.appendLine("Gastos: ${money(monthExpense)}")
            sb.appendLine("Ahorro: ${(monthSavingsRate * 100).toInt()}%")
            sb.appendLine("Balance total (todas las cuentas): ${money(dashboard.totalBalanceMinor)}")

            sb.appendLine("CUENTAS")
            dashboard.accounts.forEach { sb.appendLine("${it.name} (${it.type}): ${money(it.balanceMinor)}") }

            val budgetRows = budgetsData.rows.filter { it.limitMinor > 0 }
            if (budgetRows.isNotEmpty()) {
                sb.appendLine("PRESUPUESTOS")
                budgetRows.forEach { sb.appendLine("${it.categoryName}: gastado ${money(it.spentMinor)} de ${money(it.limitMinor)}") }
            }

            if (activeSubs.isNotEmpty()) {
                sb.appendLine("SUSCRIPCIONES")
                sb.appendLine("Costo mensual total: ${money(subsMonthlyCost)}")
                activeSubs.forEach {
                    sb.appendLine("${it.name}: ${money(it.amountMinor)} (próximo cobro: ${LocalDate.fromEpochDays(it.nextChargeDate.toInt())})")
                }
            }

            if (dashboard.donut.isNotEmpty()) {
                sb.appendLine("CATEGORIAS_TOP_GASTO_ANIO")
                dashboard.donut.take(TOP_CATEGORIES).forEach { sb.appendLine("${it.name}: ${money(it.amountMinor)} (${(it.pct * 100).toInt()}%)") }
            }

            val todayEpoch = today.toEpochDays().toLong()
            val monthStartEpoch = firstOfMonth.toEpochDays().toLong()
            val prevMonthNumber = if (today.monthNumber == 1) 12 else today.monthNumber - 1
            val prevMonthYear = if (today.monthNumber == 1) today.year - 1 else today.year
            val prevMonthStart = LocalDate(prevMonthYear, prevMonthNumber, 1)
            val prevMonthStartEpoch = prevMonthStart.toEpochDays().toLong()
            val prevMonthEndEpoch = monthStartEpoch - 1

            val spendThisMonth = categorySpendBetween(txs, cats, monthStartEpoch, todayEpoch)
            if (spendThisMonth.isNotEmpty()) {
                sb.appendLine("GASTO_CATEGORIA_MES_ACTUAL")
                spendThisMonth.take(TOP_CATEGORIES).forEach { (name, amount) -> sb.appendLine("$name: ${money(amount)}") }
            }

            val spendPrevMonth = categorySpendBetween(txs, cats, prevMonthStartEpoch, prevMonthEndEpoch)
            val prevByName = spendPrevMonth.toMap()
            val increases = spendThisMonth
                .map { (name, amount) -> Triple(name, amount, amount - (prevByName[name] ?: 0L)) }
                .filter { (_, _, delta) -> delta > 0 }
                .sortedByDescending { (_, _, delta) -> delta }
            if (increases.isNotEmpty()) {
                sb.appendLine("GASTO_CATEGORIA_MES_ANTERIOR_VS_ACTUAL")
                increases.take(TOP_CATEGORIES).forEach { (name, amount, delta) ->
                    val prev = prevByName[name] ?: 0L
                    sb.appendLine("$name: mes anterior ${money(prev)}, mes actual ${money(amount)} (+${money(delta)})")
                }
            }

            val last3MonthsStartEpoch = todayEpoch - 90
            val spendLast3Months = categorySpendBetween(txs, cats, last3MonthsStartEpoch, todayEpoch)
            if (spendLast3Months.isNotEmpty()) {
                sb.appendLine("GASTO_CATEGORIA_ULTIMOS_3_MESES")
                spendLast3Months.take(TOP_CATEGORIES).forEach { (name, amount) -> sb.appendLine("$name: ${money(amount)}") }
            }

            val monthsElapsed = today.monthNumber
            val avgMonthlyExpense = dashboard.expenseByMonth.take(monthsElapsed).sumOf { it.amountMinor } / monthsElapsed
            sb.appendLine("GASTO_PROMEDIO_MENSUAL")
            sb.appendLine("Promedio mensual ($monthsElapsed meses transcurridos de ${today.year}): ${money(avgMonthlyExpense)}")

            val yearStartEpoch = LocalDate(today.year, 1, 1).toEpochDays().toLong()
            val catNameById = cats.associate { it.id to it.name }
            val topPurchases = txs.asSequence()
                .filter { it.kind == "EXPENSE" && it.date in yearStartEpoch..todayEpoch }
                .sortedByDescending { it.amountMinor }
                .take(10)
                .toList()
            if (topPurchases.isNotEmpty()) {
                sb.appendLine("TOP_10_COMPRAS_ANIO")
                topPurchases.forEach { tx ->
                    val cat = catNameById[tx.categoryId] ?: "Sin categoría"
                    val note = tx.note.ifBlank { "(sin nota)" }
                    sb.appendLine("${LocalDate.fromEpochDays(tx.date.toInt())} · $cat · $note: ${money(tx.amountMinor)}")
                }
            }

            val spendByWeekday = txs.asSequence()
                .filter { it.kind == "EXPENSE" && it.date in last3MonthsStartEpoch..todayEpoch }
                .groupBy { diaSemana(it.date) }
                .map { (day, list) -> day to list.sumOf { it.amountMinor } }
                .sortedByDescending { it.second }
            if (spendByWeekday.isNotEmpty()) {
                sb.appendLine("GASTO_POR_DIA_SEMANA")
                spendByWeekday.forEach { (day, amount) -> sb.appendLine("${day.replaceFirstChar { it.uppercase() }}: ${money(amount)}") }
            }

            if (keyword != null) {
                val matches = findMatchingTransactions(txs, cats, keyword)
                if (matches.isNotEmpty()) {
                    sb.appendLine("MOVIMIENTOS")
                    matches.forEach { tx ->
                        val cat = catNameById[tx.categoryId] ?: "Sin categoría"
                        val note = tx.note.ifBlank { "(sin nota)" }
                        sb.appendLine("${LocalDate.fromEpochDays(tx.date.toInt())} · $cat · $note: ${money(tx.amountMinor)}")
                    }
                }
            }

            val text = sb.toString()
            return if (text.length > MAX_CONTEXT_CHARS) text.take(MAX_CONTEXT_CHARS) + "\n[...contexto truncado...]" else text
        }

        /** Gasto total por categoría entre dos epoch-days (inclusive), solo EXPENSE, orden desc. */
        internal fun categorySpendBetween(
            txs: List<TransactionRow>,
            cats: List<Category>,
            startEpochDay: Long,
            endEpochDay: Long,
        ): List<Pair<String, Long>> {
            val catNameById = cats.associate { it.id to it.name }
            return txs.asSequence()
                .filter { it.kind == "EXPENSE" && it.date in startEpochDay..endEpochDay }
                .groupBy { catNameById[it.categoryId] ?: "Sin categoría" }
                .map { (name, list) -> name to list.sumOf { it.amountMinor } }
                .sortedByDescending { it.second }
                .toList()
        }

        /** Filtro determinístico por palabra clave — no es un sistema de recuperación general. */
        internal fun findMatchingTransactions(
            txs: List<TransactionRow>,
            cats: List<Category>,
            keyword: String,
            sinceEpochDay: Long? = null,
            maxResults: Int = 20,
        ): List<TransactionRow> {
            val catNameById = cats.associate { it.id to it.name }
            return txs.asSequence()
                .filter { sinceEpochDay == null || it.date >= sinceEpochDay }
                .filter { tx ->
                    tx.note.contains(keyword, ignoreCase = true) ||
                        catNameById[tx.categoryId]?.contains(keyword, ignoreCase = true) == true
                }
                .sortedByDescending { it.date }
                .take(maxResults)
                .toList()
        }

        private val QUESTION_STOPWORDS = setOf(
            "cuanto", "cuánto", "cuanta", "cuánta", "gaste", "gasté", "gasto", "gastos",
            "el", "la", "los", "las", "de", "del", "en", "mes", "pasado", "este", "que", "qué",
            "mi", "mis", "tengo", "cuenta", "cuentas", "cuál", "cual", "cómo", "como", "es",
        )

        /** Heurístico de substring (sin NLP, igual que [extractKeyword]): preguntas más abiertas/
         * creativas usan temperatura más alta; preguntas de cifras exactas se quedan en 0.0 para
         * máxima consistencia. Default 0.0 si no matchea nada — más preguntas reales son de este tipo. */
        internal fun inferTemperature(question: String): Double {
            val q = question.lowercase()
            return when {
                listOf("idea", "sugerencia", "tip", "truco", "forma de", "manera de").any { q.contains(it) } -> 0.6
                listOf("recomien", "consejo", "asesor", "podría", "podria", "debería", "deberia").any { q.contains(it) } -> 0.4
                listOf("hábito", "habito", "patrón", "patron", "detect", "error", "administr", "evalú", "evalu").any { q.contains(it) } -> 0.2
                listOf("compar", "compár", "respecto", "diferencia", "versus", " vs ", "aumentó", "aumento", "creció", "crecio").any { q.contains(it) } -> 0.1
                else -> 0.0
            }
        }

        /** Heurístico simple de substring (sin NLP): prioriza nombres de categoría existentes;
         * si no hay coincidencia, la palabra más larga de la pregunta que no sea una palabra vacía. */
        internal fun extractKeyword(question: String, cats: List<Category>): String? {
            val lower = question.lowercase()
            cats.firstOrNull { it.name.isNotBlank() && lower.contains(it.name.lowercase()) }?.let { return it.name }
            return Regex("\\w+").findAll(lower)
                .map { it.value }
                .filter { it.length > 3 && it !in QUESTION_STOPWORDS }
                .maxByOrNull { it.length }
        }
    }
}
