package com.finanzen.platform

/**
 * País ISO-3166 (2 letras, ej. "CO", "US") del locale configurado en el sistema. No requiere
 * ningún permiso — el locale del SO es información pública. Se usa una sola vez, en el primer
 * arranque, para proponer una moneda inicial en vez de forzar USD (ver [com.finanzen.data.seedIfEmpty]).
 */
expect fun systemCountryCode(): String
