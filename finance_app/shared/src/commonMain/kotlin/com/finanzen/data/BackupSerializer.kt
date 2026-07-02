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
            CurrencyDto(it.code, it.symbol, it.decimals, it.rateToBase)
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
                it.note, it.kind, it.transferAccountId, it.installmentPlanId,
            )
        },
        installmentPlans = db.installmentPlanQueries.selectAll().executeAsList().map {
            InstallmentPlanDto(it.id, it.cardId, it.totalAmountMinor, it.installments, it.interestRate, it.startDate, it.description)
        },
        subscriptions = db.subscriptionQueries.selectAll().executeAsList().map {
            SubscriptionDto(
                it.id, it.name, it.amountMinor, it.currency, it.categoryId, it.accountId,
                it.frequency, it.intervalCount, it.nextChargeDate, it.remindDaysBefore, it.active,
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

    /** Wipe + restore. Tablas hijo primero por FKs, luego padres. */
    fun restore(db: FinanzenDb, snap: BackupSnapshot) {
        db.transaction {
            // Borrar en orden inverso al de dependencias
            db.transactionQueries.selectAll().executeAsList().forEach { db.transactionQueries.delete(it.id) }
            db.installmentPlanQueries.selectAll().executeAsList().forEach { db.installmentPlanQueries.delete(it.id) }
            db.subscriptionQueries.selectAll().executeAsList().forEach { db.subscriptionQueries.delete(it.id) }
            db.recurringExpenseQueries.selectAll().executeAsList().forEach { db.recurringExpenseQueries.delete(it.id) }
            db.budgetQueries.selectAll().executeAsList().forEach { db.budgetQueries.delete(it.id) }
            db.cardQueries.selectAll().executeAsList().forEach { db.cardQueries.delete(it.id) }
            db.categoryQueries.selectAll().executeAsList().forEach { db.categoryQueries.delete(it.id) }
            db.accountQueries.selectAllAny().executeAsList().forEach { db.accountQueries.delete(it.id) }
            db.currencyQueries.selectAll().executeAsList().forEach { db.currencyQueries.delete(it.code) }
            db.settingQueries.selectAll().executeAsList().forEach { db.settingQueries.delete(it.key) }

            // Insertar en orden directo
            snap.currencies.forEach { db.currencyQueries.upsert(it.code, it.symbol, it.decimals, it.rateToBase) }
            snap.accounts.forEach {
                db.accountQueries.insert(it.name, it.type, it.currency, it.openingBalanceMinor, it.color, it.archived)
            }
            snap.categories.forEach {
                db.categoryQueries.insert(it.parentId, it.name, it.icon, it.color, it.kind)
            }
            snap.cards.forEach {
                db.cardQueries.insert(it.accountId, it.last4, it.network, it.creditLimitMinor, it.cutoffDay, it.dueDay, it.interestRate)
            }
            snap.installmentPlans.forEach {
                db.installmentPlanQueries.insert(it.cardId, it.totalAmountMinor, it.installments, it.interestRate, it.startDate, it.description)
            }
            snap.transactions.forEach {
                db.transactionQueries.insert(
                    it.accountId, it.categoryId, it.amountMinor, it.currency, it.date,
                    it.note, it.kind, it.transferAccountId, it.installmentPlanId,
                )
            }
            snap.subscriptions.forEach {
                db.subscriptionQueries.insert(
                    it.name, it.amountMinor, it.currency, it.categoryId, it.accountId,
                    it.frequency, it.intervalCount, it.nextChargeDate, it.remindDaysBefore, it.active,
                )
            }
            snap.recurringExpenses.forEach {
                db.recurringExpenseQueries.insert(
                    it.name, it.amountMinor, it.currency, it.categoryId, it.accountId,
                    it.frequency, it.intervalCount, it.nextChargeDate, it.remindDaysBefore, it.active,
                )
            }
            snap.budgets.forEach { db.budgetQueries.upsert(it.categoryId, it.periodMonth, it.limitMinor) }
            snap.settings.forEach { db.settingQueries.put(it.key, it.value) }
        }
    }
}
