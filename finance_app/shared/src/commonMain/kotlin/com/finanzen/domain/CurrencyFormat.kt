package com.finanzen.domain

/** Dónde va el símbolo de moneda respecto al número. */
enum class SymbolPosition { PREFIX, SUFFIX, NONE }

/** Orden día/mes/año usado al formatear una fecha, según la convención de la moneda seleccionada. */
enum class DateOrder { DMY, MDY, YMD }
