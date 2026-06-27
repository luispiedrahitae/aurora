package com.finanzen.data

import kotlinx.serialization.Serializable

/** Snapshot completo de la DB para backup. Versión 1: schema actual. */
@Serializable
data class BackupSnapshot(
    val version: Int = 1,
    val createdAt: Long,
    val currencies: List<CurrencyDto>,
    val accounts: List<AccountDto>,
    val cards: List<CardDto>,
    val categories: List<CategoryDto>,
    val transactions: List<TransactionDto>,
    val installmentPlans: List<InstallmentPlanDto>,
    val subscriptions: List<SubscriptionDto>,
    val recurringExpenses: List<RecurringExpenseDto>,
    val budgets: List<BudgetDto>,
    val settings: List<SettingDto>,
)

@Serializable data class CurrencyDto(val code: String, val symbol: String, val decimals: Long, val rateToBase: Double)

@Serializable data class AccountDto(
    val id: Long,
    val name: String,
    val type: String,
    val currency: String,
    val openingBalanceMinor: Long,
    val color: Long,
    val archived: Long,
)

@Serializable data class CardDto(
    val id: Long,
    val accountId: Long,
    val last4: String,
    val network: String,
    val creditLimitMinor: Long?,
    val cutoffDay: Long?,
    val dueDay: Long?,
    val interestRate: Double? = null,
)

@Serializable data class CategoryDto(
    val id: Long,
    val parentId: Long?,
    val name: String,
    val icon: String,
    val color: Long,
    val kind: String,
)

@Serializable data class TransactionDto(
    val id: Long,
    val accountId: Long,
    val categoryId: Long?,
    val amountMinor: Long,
    val currency: String,
    val date: Long,
    val note: String,
    val kind: String,
    val transferAccountId: Long?,
    val installmentPlanId: Long?,
)

@Serializable data class InstallmentPlanDto(
    val id: Long,
    val cardId: Long,
    val totalAmountMinor: Long,
    val installments: Long,
    val interestRate: Double,
    val startDate: Long,
    val description: String,
)

@Serializable data class SubscriptionDto(
    val id: Long,
    val name: String,
    val amountMinor: Long,
    val currency: String,
    val categoryId: Long?,
    val accountId: Long?,
    val frequency: String,
    val intervalCount: Long,
    val nextChargeDate: Long,
    val remindDaysBefore: Long,
    val active: Long,
)

@Serializable data class RecurringExpenseDto(
    val id: Long,
    val name: String,
    val amountMinor: Long,
    val currency: String,
    val categoryId: Long?,
    val accountId: Long?,
    val frequency: String,
    val intervalCount: Long,
    val nextChargeDate: Long,
    val remindDaysBefore: Long,
    val active: Long,
)

@Serializable data class BudgetDto(val id: Long, val categoryId: Long, val periodMonth: Long, val limitMinor: Long)

@Serializable data class SettingDto(val key: String, val value: String)

/** Envelope cifrado escrito al disco. Self-describing; cualquier actual de BackupCrypto puede leerlo. */
@Serializable
data class EncryptedEnvelope(
    val v: Int = 1,
    val s: String, // salt hex
    val i: String, // iv hex
    val c: String, // ciphertext base64
)
