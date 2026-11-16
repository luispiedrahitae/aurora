package com.finanzen.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** Número de días del mes de esta fecha (28–31) */
fun LocalDate.daysInMonth(): Int {
    val first = LocalDate(year, month, 1)
    return (first.plus(DatePeriod(months = 1)).toEpochDays() - first.toEpochDays()).toInt()
}
