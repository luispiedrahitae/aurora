package com.finanzen.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.ui.graphics.vector.ImageVector

/** Icono por tipo de cuenta (efectivo/débito/ahorros/crédito). */
fun accountTypeIcon(type: String): ImageVector = when (type) {
    "CASH" -> Icons.Outlined.Payments
    "SAVINGS" -> Icons.Outlined.Savings
    "DEBIT" -> Icons.Outlined.AccountBalance
    "CREDIT" -> Icons.Outlined.CreditCard
    else -> Icons.Outlined.AccountBalanceWallet
}

/** Etiqueta en español por tipo de cuenta (efectivo/débito/ahorros/crédito). */
fun accountTypeLabel(type: String): String = when (type) {
    "CASH" -> "Efectivo"
    "DEBIT" -> "Débito"
    "SAVINGS" -> "Ahorros"
    "CREDIT" -> "Crédito"
    else -> type
}

/** Etiqueta en español por tipo de movimiento (ingreso/gasto/transferencia). */
fun kindLabel(kind: String): String = when (kind) {
    "INCOME" -> "Ingreso"
    "TRANSFER" -> "Transferencia"
    "ADJUSTMENT" -> "Ajuste de saldo"
    else -> "Gasto"
}
