package com.finanzen.data

import com.finanzen.db.FinanzenDb
import kotlinx.serialization.json.Json

object BackupSerializer {

    val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    fun snapshotOf(db: FinanzenDb, createdAt: Long): BackupSnapshot = BackupSnapshot(
        version = 1,
        createdAt = createdAt,
        currencies = db.currencyQueries.selectAll().executeAsList().map {
            CurrencyDto(it.code, it.symbol, it.decimals, it.rateToBase, it.name, it.decimalSeparator, it.groupSeparator)
        },
        accounts = db.accountQueries.selectAllAny().executeAsList().map {
            AccountDto(it.id, it.name, it.type, it.currency, it.openingBalanceMinor, it.color, it.archived)
        },
        cards = db.cardQueries.selectAll().executeAsList().map {
            CardDto(it.id, it.accountId, it.last4, it.network, it.creditLimitMinor, it.cutoffDay, it.dueDay, it.interestRate)
        },
        categories = db.categoryQueries.selectAll().executeAsList().map {
            CategoryDto(it.id, it.parentId, it.name, it.icon, it.color, it.kind)
        },
        transactions = db.transactionQueries.selectAll().executeAsList().map {
            TransactionDto(
                it.id, it.accountId, it.categoryId, it.amountMinor, it.currency, it.date,
                it.note, it.kind, it.transferAccountId, it.installmentPlanId, it.investmentId, it.subscriptionId,
            )
        },
        installmentPlans = db.installmentPlanQueries.selectAll().executeAsList().map {
            InstallmentPlanDto(it.id, it.cardId, it.categoryId, it.totalAmountMinor, it.installments, it.interestRate, it.startDate, it.description, it.settled)
        },
        subscriptions = db.subscriptionQueries.selectAll().executeAsList().map {
            SubscriptionDto(
                it.id, it.name, it.amountMinor, it.currency, it.categoryId, it.accountId,
                it.frequency, it.intervalCount, it.nextChargeDate, it.remindDaysBefore, it.active,
            )
        },
        investments = (db.investmentQueries.selectOpen().executeAsList() + db.investmentQueries.selectClosed().executeAsList()).map {
            InvestmentDto(
                it.id, it.name, it.amountMinor, it.currency, it.accountId, it.categoryId, it.periodic,
                it.frequency, it.intervalCount, it.nextContributionDate, it.startDate, it.status,
                it.withdrawnAmountMinor, it.closedDate, it.yieldMinor,
            )
        },
        recurringExpenses = db.recurringExpenseQueries.selectAll().executeAsList().map {
            RecurringExpenseDto(
                it.id, it.name, it.amountMinor, it.currency, it.categoryId, it.accountId,
                it.frequency, it.intervalCount, it.nextChargeDate, it.remindDaysBefore, it.active,
            )
        },
        budgets = db.budgetQueries.selectAll().executeAsList().map {
            BudgetDto(it.id, it.categoryId, it.periodMonth, it.limitMinor)
        },
        settings = db.settingQueries.selectAll().executeAsList().map {
            SettingDto(it.key, it.value_)
        },
    )

    fun toJson(snapshot: BackupSnapshot): String = json.encodeToString(BackupSnapshot.serializer(), snapshot)

    fun fromJson(text: String): BackupSnapshot = json.decodeFromString(BackupSnapshot.serializer(), text)

    /**
     * Wipe + restore. Tablas hijo primero por FKs, luego padres al borrar. Al reinsertar, cada tabla
     * con AUTOINCREMENT recibe un id nuevo (SQLite nunca reutiliza los viejos, ni siquiera tras borrar
     * todas las filas) — por eso cada FK se traduce con el mapa id-viejo→id-nuevo del padre antes de
     * insertar la fila hija. Sin esto, un segundo restore del mismo snapshot (o restaurar sobre un
     * dispositivo con datos previos) deja las FKs apuntando a filas que ya no son las correctas.
     */
    fun restore(db: FinanzenDb, snap: BackupSnapshot) {
        db.transaction {
            // Borrar en orden inverso al de dependencias
            db.transactionQueries.selectAll().executeAsList().forEach { db.transactionQueries.delete(it.id) }
            db.investmentQueries.selectOpen().executeAsList().forEach { db.investmentQueries.delete(it.id) }
            db.investmentQueries.selectClosed().executeAsList().forEach { db.investmentQueries.delete(it.id) }
            db.installmentPlanQueries.selectAll().executeAsList().forEach { db.installmentPlanQueries.delete(it.id) }
            db.subscriptionQueries.selectAll().executeAsList().forEach { db.subscriptionQueries.delete(it.id) }
            db.recurringExpenseQueries.selectAll().executeAsList().forEach { db.recurringExpenseQueries.delete(it.id) }
            db.budgetQueries.selectAll().executeAsList().forEach { db.budgetQueries.delete(it.id) }
            db.cardQueries.selectAll().executeAsList().forEach { db.cardQueries.delete(it.id) }
            db.categoryQueries.selectAll().executeAsList().forEach { db.categoryQueries.delete(it.id) }
            db.accountQueries.selectAllAny().executeAsList().forEach { db.accountQueries.delete(it.id) }
            db.currencyQueries.selectAll().executeAsList().forEach { db.currencyQueries.delete(it.code) }
            db.settingQueries.selectAll().executeAsList().forEach { db.settingQueries.delete(it.key) }

            // Insertar en orden directo, capturando el id nuevo de cada padre para traducir las FKs
            // de sus hijos. Currency.code es clave natural (no autoincrement): no necesita remapeo.
            snap.currencies.forEach {
                db.currencyQueries.upsert(it.code, it.symbol, it.decimals, it.rateToBase, it.name, it.decimalSeparator, it.groupSeparator)
            }

            val accountIdMap = mutableMapOf<Long, Long>()
            snap.accounts.forEach {
                db.accountQueries.insert(it.name, it.type, it.currency, it.openingBalanceMinor, it.color, it.archived)
                accountIdMap[it.id] = db.accountQueries.lastInsertRowId().executeAsOne()
            }

            // Category.parentId es auto-referencial y el snapshot no garantiza que el padre aparezca
            // antes que el hijo en la lista: se inserta todo sin padre primero (mapa completo), luego
            // se corrige parentId en una segunda pasada ya con todos los ids nuevos disponibles.
            val categoryIdMap = mutableMapOf<Long, Long>()
            snap.categories.forEach {
                db.categoryQueries.insert(null, it.name, it.icon, it.color, it.kind)
                categoryIdMap[it.id] = db.categoryQueries.lastInsertRowId().executeAsOne()
            }
            snap.categories.forEach { dto ->
                val newParentId = dto.parentId?.let { categoryIdMap[it] } ?: return@forEach
                db.categoryQueries.update(newParentId, dto.name, dto.icon, dto.color, dto.kind, categoryIdMap.getValue(dto.id))
            }

            // Backups viejos (pre categoryId-obligatorio, o de antes de que existieran las
            // categorías de sistema) pueden traer categoryId = null, o no incluir "Transferencias/
            // Ajustes/Suscripciones" entre snap.categories — se aseguran aquí y se reasignan las
            // filas huérfanas ahí en vez de fallar el import (ver DefaultSeed.kt).
            fun ensureSystemCategory(kind: String, parentName: String, childName: String) {
                val kindCats = db.categoryQueries.selectByKind(kind).executeAsList()
                val parent = kindCats.firstOrNull { it.parentId == null && it.name == parentName }
                val childExists = parent != null && kindCats.any { it.parentId == parent.id && it.name == childName }
                if (childExists) return
                val parentId = parent?.id ?: run {
                    db.categoryQueries.insert(null, parentName, "other", 0, kind)
                    db.categoryQueries.lastInsertRowId().executeAsOne()
                }
                db.categoryQueries.insert(parentId, childName, "other", 0, kind)
            }
            ensureSystemCategory("TRANSFER", "Transferencias", "Transferencia entre cuentas")
            ensureSystemCategory("ADJUSTMENT", "Ajustes", "Ajuste de saldo")
            ensureSystemCategory("EXPENSE", "Suscripciones", "Suscripción")
            val transferFallback = systemCategoryLeaf(db, "TRANSFER", "Transferencias", "Transferencia entre cuentas")
            val adjustmentFallback = systemCategoryLeaf(db, "ADJUSTMENT", "Ajustes", "Ajuste de saldo")
            val expenseFallback = systemCategoryLeaf(db, "EXPENSE", "Suscripciones", "Suscripción")
            fun resolvedCategoryId(oldId: Long?, fallback: Long): Long = oldId?.let { categoryIdMap[it] } ?: fallback

            val cardIdMap = mutableMapOf<Long, Long>()
            snap.cards.forEach {
                db.cardQueries.insert(accountIdMap.getValue(it.accountId), it.last4, it.network, it.creditLimitMinor, it.cutoffDay, it.dueDay, it.interestRate)
                cardIdMap[it.id] = db.cardQueries.lastInsertRowId().executeAsOne()
            }

            val planIdMap = mutableMapOf<Long, Long>()
            snap.installmentPlans.forEach {
                db.installmentPlanQueries.insert(
                    cardIdMap.getValue(it.cardId),
                    resolvedCategoryId(it.categoryId, expenseFallback),
                    it.totalAmountMinor,
                    it.installments,
                    it.interestRate,
                    it.startDate,
                    it.description,
                    it.settled,
                )
                planIdMap[it.id] = db.installmentPlanQueries.lastInsertRowId().executeAsOne()
            }

            val investmentIdMap = mutableMapOf<Long, Long>()
            snap.investments.forEach {
                db.investmentQueries.insertFull(
                    it.name, it.amountMinor, it.currency, it.accountId?.let { a -> accountIdMap[a] }, resolvedCategoryId(it.categoryId, expenseFallback),
                    it.periodic, it.frequency, it.intervalCount, it.nextContributionDate, it.startDate,
                    it.status, it.withdrawnAmountMinor, it.closedDate, it.yieldMinor,
                )
                investmentIdMap[it.id] = db.investmentQueries.lastInsertRowId().executeAsOne()
            }

            val subscriptionIdMap = mutableMapOf<Long, Long>()
            snap.subscriptions.forEach {
                db.subscriptionQueries.insert(
                    it.name, it.amountMinor, it.currency, resolvedCategoryId(it.categoryId, expenseFallback), it.accountId?.let { a -> accountIdMap[a] },
                    it.frequency, it.intervalCount, it.nextChargeDate, it.remindDaysBefore, it.active,
                )
                subscriptionIdMap[it.id] = db.subscriptionQueries.lastInsertRowId().executeAsOne()
            }

            snap.transactions.forEach {
                val fallback = when (it.kind) {
                    "TRANSFER" -> transferFallback
                    "ADJUSTMENT" -> adjustmentFallback
                    else -> expenseFallback
                }
                db.transactionQueries.insert(
                    accountIdMap.getValue(it.accountId),
                    resolvedCategoryId(it.categoryId, fallback),
                    it.amountMinor, it.currency, it.date, it.note, it.kind,
                    it.transferAccountId?.let { a -> accountIdMap[a] },
                    it.installmentPlanId?.let { p -> planIdMap[p] },
                    it.investmentId?.let { i -> investmentIdMap[i] },
                    it.subscriptionId?.let { s -> subscriptionIdMap[s] },
                )
            }
            snap.recurringExpenses.forEach {
                db.recurringExpenseQueries.insert(
                    it.name, it.amountMinor, it.currency, it.categoryId?.let { c -> categoryIdMap[c] }, it.accountId?.let { a -> accountIdMap[a] },
                    it.frequency, it.intervalCount, it.nextChargeDate, it.remindDaysBefore, it.active,
                )
            }
            snap.budgets.forEach { db.budgetQueries.upsert(categoryIdMap.getValue(it.categoryId), it.periodMonth, it.limitMinor) }
            snap.settings.forEach { db.settingQueries.put(it.key, it.value) }
        }
    }
}
