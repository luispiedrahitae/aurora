package com.finanzen.domain

/** Dónde va el símbolo de moneda respecto al número. AUTO deriva la posición real de cada moneda (CLDR). */
enum class SymbolPosition { AUTO, PREFIX, SUFFIX, NONE }

/** Orden día/mes/año usado al formatear una fecha, según la convención de la moneda seleccionada. */
enum class DateOrder { DMY, MDY, YMD }
