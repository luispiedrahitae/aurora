package com.finanzen.data

import com.finanzen.domain.DateOrder
import com.finanzen.domain.SymbolPosition

/**
 * Convenciones CLDR (posicion del simbolo, orden de fecha, nombres de mes) por moneda ISO 4217.
 * Generado una vez con un script JVM temporal (java.text/java.time contra el catalogo de Locale
 * del propio JDK, que trae datos CLDR reales) -- igual patron que WorldCurrencies.kt: dato
 * estatico compilado, sin fetch en red ni columnas nuevas en la tabla Currency.
 *
 * ponytail: para monedas usadas por varios paises (EUR, USD, GBP...) se eligio un pais/idioma
 * representativo a mano (ver script de generacion); no hay una unica convencion "correcta" para
 * una moneda multi-pais. Ajustar la eleccion aqui si un caso concreto molesta en la practica.
 */
data class CurrencyLocaleInfo(
    val symbolPosition: SymbolPosition,
    val dateOrder: DateOrder,
    val shortMonths: List<String>,
    val longMonths: List<String>,
)

val CURRENCY_LOCALE_INFO: Map<String, CurrencyLocaleInfo> = mapOf(
    "AED" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"),
        longMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"),
    ), // ar-AE
    "AFN" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.YMD,
        shortMonths = listOf("جنو", "فبروری", "مارچ", "اپریل", "می", "جون", "جول", "اگست", "سپتمبر", "اکتوبر", "نومبر", "دسم"),
        longMonths = listOf("جنوری", "فبروری", "مارچ", "اپریل", "می", "جون", "جولای", "اگست", "سپتمبر", "اکتوبر", "نومبر", "دسمبر"),
    ), // fa-AF
    "ALL" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("jan", "shk", "mar", "pri", "maj", "qer", "korr", "gush", "sht", "tet", "nën", "dhj"),
        longMonths = listOf("janar", "shkurt", "mars", "prill", "maj", "qershor", "korrik", "gusht", "shtator", "tetor", "nëntor", "dhjetor"),
    ), // sq-AL
    "AMD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("հնվ", "փտվ", "մրտ", "ապր", "մյս", "հնս", "հլս", "օգս", "սեպ", "հոկ", "նոյ", "դեկ"),
        longMonths = listOf("հունվարի", "փետրվարի", "մարտի", "ապրիլի", "մայիսի", "հունիսի", "հուլիսի", "օգոստոսի", "սեպտեմբերի", "հոկտեմբերի", "նոյեմբերի", "դեկտեմբերի"),
    ), // hy-AM
    "AOA" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("jan.", "fev.", "mar.", "abr.", "mai.", "jun.", "jul.", "ago.", "set.", "out.", "nov.", "dez."),
        longMonths = listOf("janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"),
    ), // pt-AO
    "ARS" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ene.", "feb.", "mar.", "abr.", "may.", "jun.", "jul.", "ago.", "sep.", "oct.", "nov.", "dic."),
        longMonths = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"),
    ), // es-AR
    "AUD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "June", "July", "Aug", "Sept", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-AU
    "AWG" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("jan.", "feb.", "mrt.", "apr.", "mei", "jun.", "jul.", "aug.", "sep.", "okt.", "nov.", "dec."),
        longMonths = listOf("januari", "februari", "maart", "april", "mei", "juni", "juli", "augustus", "september", "oktober", "november", "december"),
    ), // nl-AW
    "AZN" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("yan", "fev", "mar", "apr", "may", "iyn", "iyl", "avq", "sen", "okt", "noy", "dek"),
        longMonths = listOf("yanvar", "fevral", "mart", "aprel", "may", "iyun", "iyul", "avqust", "sentyabr", "oktyabr", "noyabr", "dekabr"),
    ), // az-AZ
    "BAM" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("jan", "feb", "mar", "apr", "maj", "jun", "jul", "aug", "sep", "okt", "nov", "dec"),
        longMonths = listOf("januar", "februar", "mart", "april", "maj", "juni", "juli", "august", "septembar", "oktobar", "novembar", "decembar"),
    ), // bs-BA
    "BBD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-BB
    "BDT" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("জানু", "ফেব", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"),
        longMonths = listOf("জানুয়ারী", "ফেব্রুয়ারী", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"),
    ), // bn-BD
    "BGN" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("яну", "фев", "март", "апр", "май", "юни", "юли", "авг", "сеп", "окт", "ное", "дек"),
        longMonths = listOf("януари", "февруари", "март", "април", "май", "юни", "юли", "август", "септември", "октомври", "ноември", "декември"),
    ), // bg-BG
    "BHD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"),
        longMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"),
    ), // ar-BH
    "BIF" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc."),
        longMonths = listOf("janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre"),
    ), // fr-BI
    "BMD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-BM
    "BND" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mac", "Apr", "Mei", "Jun", "Jul", "Ogo", "Sep", "Okt", "Nov", "Dis"),
        longMonths = listOf("Januari", "Februari", "Mac", "April", "Mei", "Jun", "Julai", "Ogos", "September", "Oktober", "November", "Disember"),
    ), // ms-BN
    "BOB" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ene.", "feb.", "mar.", "abr.", "may.", "jun.", "jul.", "ago.", "sep.", "oct.", "nov.", "dic."),
        longMonths = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"),
    ), // es-BO
    "BRL" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("jan.", "fev.", "mar.", "abr.", "mai.", "jun.", "jul.", "ago.", "set.", "out.", "nov.", "dez."),
        longMonths = listOf("janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"),
    ), // pt-BR
    "BSD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-BS
    "BTN" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.YMD,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // dz-BT
    "BWP" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-BW
    "BYN" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("янв.", "февр.", "мар.", "апр.", "мая", "июн.", "июл.", "авг.", "сент.", "окт.", "нояб.", "дек."),
        longMonths = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"),
    ), // ru-BY
    "BZD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-BZ
    "CAD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.YMD,
        shortMonths = listOf("Jan.", "Feb.", "Mar.", "Apr.", "May", "Jun.", "Jul.", "Aug.", "Sep.", "Oct.", "Nov.", "Dec."),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-CA
    "CDF" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc."),
        longMonths = listOf("janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre"),
    ), // fr-CD
    "CHF" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan.", "Feb.", "März", "Apr.", "Mai", "Juni", "Juli", "Aug.", "Sept.", "Okt.", "Nov.", "Dez."),
        longMonths = listOf("Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember"),
    ), // de-CH
    "CLP" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ene.", "feb.", "mar.", "abr.", "may.", "jun.", "jul.", "ago.", "sep.", "oct.", "nov.", "dic."),
        longMonths = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"),
    ), // es-CL
    "CNY" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.YMD,
        shortMonths = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"),
        longMonths = listOf("一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"),
    ), // zh-CN
    "COP" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ene.", "feb.", "mar.", "abr.", "may.", "jun.", "jul.", "ago.", "sep.", "oct.", "nov.", "dic."),
        longMonths = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"),
    ), // es-CO
    "CRC" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ene.", "feb.", "mar.", "abr.", "may.", "jun.", "jul.", "ago.", "sep.", "oct.", "nov.", "dic."),
        longMonths = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"),
    ), // es-CR
    "CUP" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ene.", "feb.", "mar.", "abr.", "may.", "jun.", "jul.", "ago.", "sep.", "oct.", "nov.", "dic."),
        longMonths = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"),
    ), // es-CU
    "CVE" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("jan.", "fev.", "mar.", "abr.", "mai.", "jun.", "jul.", "ago.", "set.", "out.", "nov.", "dez."),
        longMonths = listOf("janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"),
    ), // pt-CV
    "CZK" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("led", "úno", "bře", "dub", "kvě", "čvn", "čvc", "srp", "zář", "říj", "lis", "pro"),
        longMonths = listOf("ledna", "února", "března", "dubna", "května", "června", "července", "srpna", "září", "října", "listopadu", "prosince"),
    ), // cs-CZ
    "DJF" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc."),
        longMonths = listOf("janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre"),
    ), // fr-DJ
    "DKK" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("jan.", "feb.", "mar.", "apr.", "maj", "jun.", "jul.", "aug.", "sep.", "okt.", "nov.", "dec."),
        longMonths = listOf("januar", "februar", "marts", "april", "maj", "juni", "juli", "august", "september", "oktober", "november", "december"),
    ), // da-DK
    "DOP" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ene.", "feb.", "mar.", "abr.", "may.", "jun.", "jul.", "ago.", "sep.", "oct.", "nov.", "dic."),
        longMonths = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"),
    ), // es-DO
    "DZD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("جانفي", "فيفري", "مارس", "أفريل", "ماي", "جوان", "جويلية", "أوت", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"),
        longMonths = listOf("جانفي", "فيفري", "مارس", "أفريل", "ماي", "جوان", "جويلية", "أوت", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"),
    ), // ar-DZ
    "EGP" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"),
        longMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"),
    ), // ar-EG
    "ERN" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ጥሪ", "ለካ", "መጋ", "ሚያ", "ግን", "ሰነ", "ሓም", "ነሓ", "መስ", "ጥቅ", "ሕዳ", "ታሕ"),
        longMonths = listOf("ጥሪ", "ለካቲት", "መጋቢት", "ሚያዝያ", "ግንቦት", "ሰነ", "ሓምለ", "ነሓሰ", "መስከረም", "ጥቅምቲ", "ሕዳር", "ታሕሳስ"),
    ), // ti-ER
    "ETB" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ጃንዩ", "ፌብሩ", "ማርች", "ኤፕሪ", "ሜይ", "ጁን", "ጁላይ", "ኦገስ", "ሴፕቴ", "ኦክቶ", "ኖቬም", "ዲሴም"),
        longMonths = listOf("ጃንዩወሪ", "ፌብሩወሪ", "ማርች", "ኤፕሪል", "ሜይ", "ጁን", "ጁላይ", "ኦገስት", "ሴፕቴምበር", "ኦክቶበር", "ኖቬምበር", "ዲሴምበር"),
    ), // am-ET
    "EUR" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan.", "Feb.", "März", "Apr.", "Mai", "Juni", "Juli", "Aug.", "Sept.", "Okt.", "Nov.", "Dez."),
        longMonths = listOf("Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember"),
    ), // de-DE
    "FJD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-FJ
    "FKP" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-FK
    "GBP" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-GB
    "GEL" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("იან", "თებ", "მარ", "აპრ", "მაი", "ივნ", "ივლ", "აგვ", "სექ", "ოქტ", "ნოე", "დეკ"),
        longMonths = listOf("იანვარი", "თებერვალი", "მარტი", "აპრილი", "მაისი", "ივნისი", "ივლისი", "აგვისტო", "სექტემბერი", "ოქტომბერი", "ნოემბერი", "დეკემბერი"),
    ), // ka-GE
    "GHS" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-GH
    "GIP" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-GI
    "GMD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-GM
    "GNF" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc."),
        longMonths = listOf("janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre"),
    ), // fr-GN
    "GTQ" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ene.", "feb.", "mar.", "abr.", "may.", "jun.", "jul.", "ago.", "sep.", "oct.", "nov.", "dic."),
        longMonths = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"),
    ), // es-GT
    "GYD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-GY
    "HKD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"),
        longMonths = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"),
    ), // zh-HK
    "HNL" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ene.", "feb.", "mar.", "abr.", "may.", "jun.", "jul.", "ago.", "sep.", "oct.", "nov.", "dic."),
        longMonths = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"),
    ), // es-HN
    "HTG" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc."),
        longMonths = listOf("janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre"),
    ), // fr-HT
    "HUF" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.YMD,
        shortMonths = listOf("jan.", "febr.", "márc.", "ápr.", "máj.", "jún.", "júl.", "aug.", "szept.", "okt.", "nov.", "dec."),
        longMonths = listOf("január", "február", "március", "április", "május", "június", "július", "augusztus", "szeptember", "október", "november", "december"),
    ), // hu-HU
    "IDR" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des"),
        longMonths = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember"),
    ), // id-ID
    "ILS" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ינו׳", "פבר׳", "מרץ", "אפר׳", "מאי", "יוני", "יולי", "אוג׳", "ספט׳", "אוק׳", "נוב׳", "דצמ׳"),
        longMonths = listOf("ינואר", "פברואר", "מרץ", "אפריל", "מאי", "יוני", "יולי", "אוגוסט", "ספטמבר", "אוקטובר", "נובמבר", "דצמבר"),
    ), // he-IL
    "INR" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("जन॰", "फ़र॰", "मार्च", "अप्रैल", "मई", "जून", "जुल॰", "अग॰", "सित॰", "अक्तू॰", "नव॰", "दिस॰"),
        longMonths = listOf("जनवरी", "फ़रवरी", "मार्च", "अप्रैल", "मई", "जून", "जुलाई", "अगस्त", "सितंबर", "अक्तूबर", "नवंबर", "दिसंबर"),
    ), // hi-IN
    "IQD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("كانون الثاني", "شباط", "آذار", "نيسان", "أيار", "حزيران", "تموز", "آب", "أيلول", "تشرين الأول", "تشرين الثاني", "كانون الأول"),
        longMonths = listOf("كانون الثاني", "شباط", "آذار", "نيسان", "أيار", "حزيران", "تموز", "آب", "أيلول", "تشرين الأول", "تشرين الثاني", "كانون الأول"),
    ), // ar-IQ
    "IRR" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.YMD,
        shortMonths = listOf("ژانویهٔ", "فوریهٔ", "مارس", "آوریل", "مهٔ", "ژوئن", "ژوئیهٔ", "اوت", "سپتامبر", "اکتبر", "نوامبر", "دسامبر"),
        longMonths = listOf("ژانویهٔ", "فوریهٔ", "مارس", "آوریل", "مهٔ", "ژوئن", "ژوئیهٔ", "اوت", "سپتامبر", "اکتبر", "نوامبر", "دسامبر"),
    ), // fa-IR
    "ISK" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("jan.", "feb.", "mar.", "apr.", "maí", "jún.", "júl.", "ágú.", "sep.", "okt.", "nóv.", "des."),
        longMonths = listOf("janúar", "febrúar", "mars", "apríl", "maí", "júní", "júlí", "ágúst", "september", "október", "nóvember", "desember"),
    ), // is-IS
    "JMD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-JM
    "JOD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("كانون الثاني", "شباط", "آذار", "نيسان", "أيار", "حزيران", "تموز", "آب", "أيلول", "تشرين الأول", "تشرين الثاني", "كانون الأول"),
        longMonths = listOf("كانون الثاني", "شباط", "آذار", "نيسان", "أيار", "حزيران", "تموز", "آب", "أيلول", "تشرين الأول", "تشرين الثاني", "كانون الأول"),
    ), // ar-JO
    "JPY" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.YMD,
        shortMonths = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"),
        longMonths = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"),
    ), // ja-JP
    "KES" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-KE
    "KGS" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("янв.", "фев.", "мар.", "апр.", "май", "июн.", "июл.", "авг.", "сен.", "окт.", "ноя.", "дек."),
        longMonths = listOf("январь", "февраль", "март", "апрель", "май", "июнь", "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь"),
    ), // ky-KG
    "KHR" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("មករា", "កុម្ភៈ", "មីនា", "មេសា", "ឧសភា", "មិថុនា", "កក្កដា", "សីហា", "កញ្ញា", "តុលា", "វិច្ឆិកា", "ធ្នូ"),
        longMonths = listOf("មករា", "កុម្ភៈ", "មីនា", "មេសា", "ឧសភា", "មិថុនា", "កក្កដា", "សីហា", "កញ្ញា", "តុលា", "វិច្ឆិកា", "ធ្នូ"),
    ), // km-KH
    "KMF" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc."),
        longMonths = listOf("janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre"),
    ), // fr-KM
    "KPW" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.YMD,
        shortMonths = listOf("1월", "2월", "3월", "4월", "5월", "6월", "7월", "8월", "9월", "10월", "11월", "12월"),
        longMonths = listOf("1월", "2월", "3월", "4월", "5월", "6월", "7월", "8월", "9월", "10월", "11월", "12월"),
    ), // ko-KP
    "KRW" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.YMD,
        shortMonths = listOf("1월", "2월", "3월", "4월", "5월", "6월", "7월", "8월", "9월", "10월", "11월", "12월"),
        longMonths = listOf("1월", "2월", "3월", "4월", "5월", "6월", "7월", "8월", "9월", "10월", "11월", "12월"),
    ), // ko-KR
    "KWD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"),
        longMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"),
    ), // ar-KW
    "KYD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-KY
    "KZT" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("қаң.", "ақп.", "нау.", "сәу.", "мам.", "мау.", "шіл.", "там.", "қыр.", "қаз.", "қар.", "жел."),
        longMonths = listOf("қаңтар", "ақпан", "наурыз", "сәуір", "мамыр", "маусым", "шілде", "тамыз", "қыркүйек", "қазан", "қараша", "желтоқсан"),
    ), // kk-KZ
    "LAK" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ມ.ກ.", "ກ.ພ.", "ມ.ນ.", "ມ.ສ.", "ພ.ພ.", "ມິ.ຖ.", "ກ.ລ.", "ສ.ຫ.", "ກ.ຍ.", "ຕ.ລ.", "ພ.ຈ.", "ທ.ວ."),
        longMonths = listOf("ມັງກອນ", "ກຸມພາ", "ມີນາ", "ເມສາ", "ພຶດສະພາ", "ມິຖຸນາ", "ກໍລະກົດ", "ສິງຫາ", "ກັນຍາ", "ຕຸລາ", "ພະຈິກ", "ທັນວາ"),
    ), // lo-LA
    "LBP" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("كانون الثاني", "شباط", "آذار", "نيسان", "أيار", "حزيران", "تموز", "آب", "أيلول", "تشرين الأول", "تشرين الثاني", "كانون الأول"),
        longMonths = listOf("كانون الثاني", "شباط", "آذار", "نيسان", "أيار", "حزيران", "تموز", "آب", "أيلول", "تشرين الأول", "تشرين الثاني", "كانون الأول"),
    ), // ar-LB
    "LKR" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.YMD,
        shortMonths = listOf("ජන", "පෙබ", "මාර්තු", "අප්‍රේල්", "මැයි", "ජූනි", "ජූලි", "අගෝ", "සැප්", "ඔක්", "නොවැ", "දෙසැ"),
        longMonths = listOf("ජනවාරි", "පෙබරවාරි", "මාර්තු", "අප්‍රේල්", "මැයි", "ජූනි", "ජූලි", "අගෝස්තු", "සැප්තැම්බර්", "ඔක්තෝබර්", "නොවැම්බර්", "දෙසැම්බර්"),
    ), // si-LK
    "LRD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-LR
    "LSL" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-LS
    "LYD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"),
        longMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"),
    ), // ar-LY
    "MAD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "ماي", "يونيو", "يوليوز", "غشت", "شتنبر", "أكتوبر", "نونبر", "دجنبر"),
        longMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "ماي", "يونيو", "يوليوز", "غشت", "شتنبر", "أكتوبر", "نونبر", "دجنبر"),
    ), // ar-MA
    "MDL" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ian.", "feb.", "mar.", "apr.", "mai", "iun.", "iul.", "aug.", "sept.", "oct.", "nov.", "dec."),
        longMonths = listOf("ianuarie", "februarie", "martie", "aprilie", "mai", "iunie", "iulie", "august", "septembrie", "octombrie", "noiembrie", "decembrie"),
    ), // ro-MD
    "MGA" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc."),
        longMonths = listOf("janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre"),
    ), // fr-MG
    "MKD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("јан.", "фев.", "мар.", "апр.", "мај", "јун.", "јул.", "авг.", "септ.", "окт.", "ноем.", "дек."),
        longMonths = listOf("јануари", "февруари", "март", "април", "мај", "јуни", "јули", "август", "септември", "октомври", "ноември", "декември"),
    ), // mk-MK
    "MMK" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ဇန်", "ဖေ", "မတ်", "ဧ", "မေ", "ဇွန်", "ဇူ", "ဩ", "စက်", "အောက်", "နို", "ဒီ"),
        longMonths = listOf("ဇန်နဝါရီ", "ဖေဖော်ဝါရီ", "မတ်", "ဧပြီ", "မေ", "ဇွန်", "ဇူလိုင်", "ဩဂုတ်", "စက်တင်ဘာ", "အောက်တိုဘာ", "နိုဝင်ဘာ", "ဒီဇင်ဘာ"),
    ), // my-MM
    "MNT" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.YMD,
        shortMonths = listOf("1-р сар", "2-р сар", "3-р сар", "4-р сар", "5-р сар", "6-р сар", "7-р сар", "8-р сар", "9-р сар", "10-р сар", "11-р сар", "12-р сар"),
        longMonths = listOf("нэгдүгээр сар", "хоёрдугаар сар", "гуравдугаар сар", "дөрөвдүгээр сар", "тавдугаар сар", "зургаадугаар сар", "долоодугаар сар", "наймдугаар сар", "есдүгээр сар", "аравдугаар сар", "арван нэгдүгээр сар", "арван хоёрдугаар сар"),
    ), // mn-MN
    "MOP" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"),
        longMonths = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"),
    ), // zh-MO
    "MRU" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("يناير", "فبراير", "مارس", "إبريل", "مايو", "يونيو", "يوليو", "أغشت", "شتمبر", "أكتوبر", "نوفمبر", "دجمبر"),
        longMonths = listOf("يناير", "فبراير", "مارس", "إبريل", "مايو", "يونيو", "يوليو", "أغشت", "شتمبر", "أكتوبر", "نوفمبر", "دجمبر"),
    ), // ar-MR
    "MUR" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-MU
    "MWK" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-MW
    "MXN" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ene.", "feb.", "mar.", "abr.", "may.", "jun.", "jul.", "ago.", "sep.", "oct.", "nov.", "dic."),
        longMonths = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"),
    ), // es-MX
    "MYR" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mac", "Apr", "Mei", "Jun", "Jul", "Ogo", "Sep", "Okt", "Nov", "Dis"),
        longMonths = listOf("Januari", "Februari", "Mac", "April", "Mei", "Jun", "Julai", "Ogos", "September", "Oktober", "November", "Disember"),
    ), // ms-MY
    "MZN" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("jan.", "fev.", "mar.", "abr.", "mai.", "jun.", "jul.", "ago.", "set.", "out.", "nov.", "dez."),
        longMonths = listOf("janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"),
    ), // pt-MZ
    "NAD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-NA
    "NGN" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-NG
    "NIO" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ene.", "feb.", "mar.", "abr.", "may.", "jun.", "jul.", "ago.", "sep.", "oct.", "nov.", "dic."),
        longMonths = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"),
    ), // es-NI
    "NOK" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("jan.", "feb.", "mar.", "apr.", "mai", "jun.", "jul.", "aug.", "sep.", "okt.", "nov.", "des."),
        longMonths = listOf("januar", "februar", "mars", "april", "mai", "juni", "juli", "august", "september", "oktober", "november", "desember"),
    ), // nb-NO
    "NPR" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.YMD,
        shortMonths = listOf("जनवरी", "फेब्रुअरी", "मार्च", "अप्रिल", "मे", "जुन", "जुलाई", "अगस्ट", "सेप्टेम्बर", "अक्टोबर", "नोभेम्बर", "डिसेम्बर"),
        longMonths = listOf("जनवरी", "फेब्रुअरी", "मार्च", "अप्रिल", "मे", "जुन", "जुलाई", "अगस्ट", "सेप्टेम्बर", "अक्टोबर", "नोभेम्बर", "डिसेम्बर"),
    ), // ne-NP
    "NZD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-NZ
    "OMR" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"),
        longMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"),
    ), // ar-OM
    "PAB" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.MDY,
        shortMonths = listOf("ene.", "feb.", "mar.", "abr.", "may.", "jun.", "jul.", "ago.", "sep.", "oct.", "nov.", "dic."),
        longMonths = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"),
    ), // es-PA
    "PEN" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ene.", "feb.", "mar.", "abr.", "may.", "jun.", "jul.", "ago.", "set.", "oct.", "nov.", "dic."),
        longMonths = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "setiembre", "octubre", "noviembre", "diciembre"),
    ), // es-PE
    "PGK" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-PG
    "PHP" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.MDY,
        shortMonths = listOf("Ene", "Peb", "Mar", "Abr", "May", "Hun", "Hul", "Ago", "Set", "Okt", "Nob", "Dis"),
        longMonths = listOf("Enero", "Pebrero", "Marso", "Abril", "Mayo", "Hunyo", "Hulyo", "Agosto", "Setyembre", "Oktubre", "Nobyembre", "Disyembre"),
    ), // fil-PH
    "PKR" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-PK
    "PLN" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("sty", "lut", "mar", "kwi", "maj", "cze", "lip", "sie", "wrz", "paź", "lis", "gru"),
        longMonths = listOf("stycznia", "lutego", "marca", "kwietnia", "maja", "czerwca", "lipca", "sierpnia", "września", "października", "listopada", "grudnia"),
    ), // pl-PL
    "PYG" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ene.", "feb.", "mar.", "abr.", "may.", "jun.", "jul.", "ago.", "sept.", "oct.", "nov.", "dic."),
        longMonths = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"),
    ), // es-PY
    "QAR" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"),
        longMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"),
    ), // ar-QA
    "RON" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ian.", "feb.", "mar.", "apr.", "mai", "iun.", "iul.", "aug.", "sept.", "oct.", "nov.", "dec."),
        longMonths = listOf("ianuarie", "februarie", "martie", "aprilie", "mai", "iunie", "iulie", "august", "septembrie", "octombrie", "noiembrie", "decembrie"),
    ), // ro-RO
    "RSD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("јан", "феб", "мар", "апр", "мај", "јун", "јул", "авг", "сеп", "окт", "нов", "дец"),
        longMonths = listOf("јануар", "фебруар", "март", "април", "мај", "јун", "јул", "август", "септембар", "октобар", "новембар", "децембар"),
    ), // sr-RS
    "RUB" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("янв.", "февр.", "мар.", "апр.", "мая", "июн.", "июл.", "авг.", "сент.", "окт.", "нояб.", "дек."),
        longMonths = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"),
    ), // ru-RU
    "RWF" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.YMD,
        shortMonths = listOf("mut.", "gas.", "wer.", "mat.", "gic.", "kam.", "nya.", "kan.", "nze.", "ukw.", "ugu.", "uku."),
        longMonths = listOf("Mutarama", "Gashyantare", "Werurwe", "Mata", "Gicuransi", "Kamena", "Nyakanga", "Kanama", "Nzeli", "Ukwakira", "Ugushyingo", "Ukuboza"),
    ), // rw-RW
    "SAR" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"),
        longMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"),
    ), // ar-SA
    "SBD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-SB
    "SCR" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-SC
    "SDG" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"),
        longMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"),
    ), // ar-SD
    "SEK" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.YMD,
        shortMonths = listOf("jan.", "feb.", "mars", "apr.", "maj", "juni", "juli", "aug.", "sep.", "okt.", "nov.", "dec."),
        longMonths = listOf("januari", "februari", "mars", "april", "maj", "juni", "juli", "augusti", "september", "oktober", "november", "december"),
    ), // sv-SE
    "SGD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-SG
    "SHP" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-SH
    "SLE" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-SL
    "SOS" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Abr", "May", "Jun", "Lul", "Ogs", "Seb", "Okt", "Nof", "Dis"),
        longMonths = listOf("Bisha Koobaad", "Bisha Labaad", "Bisha Saddexaad", "Bisha Afraad", "Bisha Shanaad", "Bisha Lixaad", "Bisha Todobaad", "Bisha Sideedaad", "Bisha Sagaalaad", "Bisha Tobnaad", "Bisha Kow iyo Tobnaad", "Bisha Laba iyo Tobnaad"),
    ), // so-SO
    "SRD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("jan.", "feb.", "mrt.", "apr.", "mei", "jun.", "jul.", "aug.", "sep.", "okt.", "nov.", "dec."),
        longMonths = listOf("januari", "februari", "maart", "april", "mei", "juni", "juli", "augustus", "september", "oktober", "november", "december"),
    ), // nl-SR
    "SSP" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-SS
    "STN" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("jan.", "fev.", "mar.", "abr.", "mai.", "jun.", "jul.", "ago.", "set.", "out.", "nov.", "dez."),
        longMonths = listOf("janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"),
    ), // pt-ST
    "SVC" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ene.", "feb.", "mar.", "abr.", "may.", "jun.", "jul.", "ago.", "sep.", "oct.", "nov.", "dic."),
        longMonths = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"),
    ), // es-SV
    "SYP" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("كانون الثاني", "شباط", "آذار", "نيسان", "أيار", "حزيران", "تموز", "آب", "أيلول", "تشرين الأول", "تشرين الثاني", "كانون الأول"),
        longMonths = listOf("كانون الثاني", "شباط", "آذار", "نيسان", "أيار", "حزيران", "تموز", "آب", "أيلول", "تشرين الأول", "تشرين الثاني", "كانون الأول"),
    ), // ar-SY
    "SZL" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-SZ
    "THB" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.", "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค."),
        longMonths = listOf("มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน", "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม"),
    ), // th-TH
    "TJS" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек"),
        longMonths = listOf("Январ", "Феврал", "Март", "Апрел", "Май", "Июн", "Июл", "Август", "Сентябр", "Октябр", "Ноябр", "Декабр"),
    ), // tg-TJ
    "TMT" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ýan", "few", "mart", "apr", "maý", "iýun", "iýul", "awg", "sen", "okt", "noý", "dek"),
        longMonths = listOf("ýanwar", "fewral", "mart", "aprel", "maý", "iýun", "iýul", "awgust", "sentýabr", "oktýabr", "noýabr", "dekabr"),
    ), // tk-TM
    "TND" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("جانفي", "فيفري", "مارس", "أفريل", "ماي", "جوان", "جويلية", "أوت", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"),
        longMonths = listOf("جانفي", "فيفري", "مارس", "أفريل", "ماي", "جوان", "جويلية", "أوت", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"),
    ), // ar-TN
    "TOP" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-TO
    "TRY" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Oca", "Şub", "Mar", "Nis", "May", "Haz", "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara"),
        longMonths = listOf("Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"),
    ), // tr-TR
    "TTD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-TT
    "TWD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.YMD,
        shortMonths = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"),
        longMonths = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"),
    ), // zh-TW
    "TZS" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mac", "Apr", "Mei", "Jun", "Jul", "Ago", "Sep", "Okt", "Nov", "Des"),
        longMonths = listOf("Januari", "Februari", "Machi", "Aprili", "Mei", "Juni", "Julai", "Agosti", "Septemba", "Oktoba", "Novemba", "Desemba"),
    ), // sw-TZ
    "UAH" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("січ.", "лют.", "бер.", "квіт.", "трав.", "черв.", "лип.", "серп.", "вер.", "жовт.", "лист.", "груд."),
        longMonths = listOf("січня", "лютого", "березня", "квітня", "травня", "червня", "липня", "серпня", "вересня", "жовтня", "листопада", "грудня"),
    ), // uk-UA
    "UGX" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-UG
    "USD" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.MDY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-US
    "UYU" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ene.", "feb.", "mar.", "abr.", "may.", "jun.", "jul.", "ago.", "set.", "oct.", "nov.", "dic."),
        longMonths = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "setiembre", "octubre", "noviembre", "diciembre"),
    ), // es-UY
    "UZS" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("yan", "fev", "mar", "apr", "may", "iyn", "iyl", "avg", "sen", "okt", "noy", "dek"),
        longMonths = listOf("yanvar", "fevral", "mart", "aprel", "may", "iyun", "iyul", "avgust", "sentabr", "oktabr", "noyabr", "dekabr"),
    ), // uz-UZ
    "VES" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("ene.", "feb.", "mar.", "abr.", "may.", "jun.", "jul.", "ago.", "sept.", "oct.", "nov.", "dic."),
        longMonths = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"),
    ), // es-VE
    "VND" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("thg 1", "thg 2", "thg 3", "thg 4", "thg 5", "thg 6", "thg 7", "thg 8", "thg 9", "thg 10", "thg 11", "thg 12"),
        longMonths = listOf("tháng 1", "tháng 2", "tháng 3", "tháng 4", "tháng 5", "tháng 6", "tháng 7", "tháng 8", "tháng 9", "tháng 10", "tháng 11", "tháng 12"),
    ), // vi-VN
    "VUV" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-VU
    "WST" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-WS
    "YER" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.SUFFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"),
        longMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"),
    ), // ar-YE
    "ZAR" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.YMD,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-ZA
    "ZMW" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // en-ZM
    "MVR" to CurrencyLocaleInfo(
        symbolPosition = SymbolPosition.PREFIX,
        dateOrder = DateOrder.DMY,
        shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        longMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    ), // ponytail: fallback ingles, sin locale dv-MV disponible en el JDK
)

val DEFAULT_LOCALE_INFO: CurrencyLocaleInfo = CURRENCY_LOCALE_INFO.getValue("USD")

/** Pais ISO-3166 (2 letras) -> moneda representativa. Usado para proponer la moneda inicial segun el locale del sistema. */
val COUNTRY_TO_CURRENCY: Map<String, String> = mapOf(
    "AE" to "AED",
    "AF" to "AFN",
    "AL" to "ALL",
    "AM" to "AMD",
    "AO" to "AOA",
    "AR" to "ARS",
    "AU" to "AUD",
    "AW" to "AWG",
    "AZ" to "AZN",
    "BA" to "BAM",
    "BB" to "BBD",
    "BD" to "BDT",
    "BG" to "BGN",
    "BH" to "BHD",
    "BI" to "BIF",
    "BM" to "BMD",
    "BN" to "BND",
    "BO" to "BOB",
    "BR" to "BRL",
    "BS" to "BSD",
    "BT" to "BTN",
    "BW" to "BWP",
    "BY" to "BYN",
    "BZ" to "BZD",
    "CA" to "CAD",
    "CD" to "CDF",
    "CH" to "CHF",
    "CL" to "CLP",
    "CN" to "CNY",
    "CO" to "COP",
    "CR" to "CRC",
    "CU" to "CUP",
    "CV" to "CVE",
    "CZ" to "CZK",
    "DJ" to "DJF",
    "DK" to "DKK",
    "DO" to "DOP",
    "DZ" to "DZD",
    "EG" to "EGP",
    "ER" to "ERN",
    "ET" to "ETB",
    "DE" to "EUR",
    "FJ" to "FJD",
    "FK" to "FKP",
    "GB" to "GBP",
    "GE" to "GEL",
    "GH" to "GHS",
    "GI" to "GIP",
    "GM" to "GMD",
    "GN" to "GNF",
    "GT" to "GTQ",
    "GY" to "GYD",
    "HK" to "HKD",
    "HN" to "HNL",
    "HT" to "HTG",
    "HU" to "HUF",
    "ID" to "IDR",
    "IL" to "ILS",
    "IN" to "INR",
    "IQ" to "IQD",
    "IR" to "IRR",
    "IS" to "ISK",
    "JM" to "JMD",
    "JO" to "JOD",
    "JP" to "JPY",
    "KE" to "KES",
    "KG" to "KGS",
    "KH" to "KHR",
    "KM" to "KMF",
    "KP" to "KPW",
    "KR" to "KRW",
    "KW" to "KWD",
    "KY" to "KYD",
    "KZ" to "KZT",
    "LA" to "LAK",
    "LB" to "LBP",
    "LK" to "LKR",
    "LR" to "LRD",
    "LS" to "LSL",
    "LY" to "LYD",
    "MA" to "MAD",
    "MD" to "MDL",
    "MG" to "MGA",
    "MK" to "MKD",
    "MM" to "MMK",
    "MN" to "MNT",
    "MO" to "MOP",
    "MR" to "MRU",
    "MU" to "MUR",
    "MW" to "MWK",
    "MX" to "MXN",
    "MY" to "MYR",
    "MZ" to "MZN",
    "NA" to "NAD",
    "NG" to "NGN",
    "NI" to "NIO",
    "NO" to "NOK",
    "NP" to "NPR",
    "NZ" to "NZD",
    "OM" to "OMR",
    "PA" to "PAB",
    "PE" to "PEN",
    "PG" to "PGK",
    "PH" to "PHP",
    "PK" to "PKR",
    "PL" to "PLN",
    "PY" to "PYG",
    "QA" to "QAR",
    "RO" to "RON",
    "RS" to "RSD",
    "RU" to "RUB",
    "RW" to "RWF",
    "SA" to "SAR",
    "SB" to "SBD",
    "SC" to "SCR",
    "SD" to "SDG",
    "SE" to "SEK",
    "SG" to "SGD",
    "SH" to "SHP",
    "SL" to "SLE",
    "SO" to "SOS",
    "SR" to "SRD",
    "SS" to "SSP",
    "ST" to "STN",
    "SV" to "SVC",
    "SY" to "SYP",
    "SZ" to "SZL",
    "TH" to "THB",
    "TJ" to "TJS",
    "TM" to "TMT",
    "TN" to "TND",
    "TO" to "TOP",
    "TR" to "TRY",
    "TT" to "TTD",
    "TW" to "TWD",
    "TZ" to "TZS",
    "UA" to "UAH",
    "UG" to "UGX",
    "US" to "USD",
    "UY" to "UYU",
    "UZ" to "UZS",
    "VE" to "VES",
    "VN" to "VND",
    "VU" to "VUV",
    "WS" to "WST",
    "YE" to "YER",
    "ZA" to "ZAR",
    "ZM" to "ZMW",
    "MV" to "MVR",
)
