package com.example.util

import java.text.DecimalFormat

object FormatUtil {
    fun formatPrice(amount: Double): String {
        val df = DecimalFormat("#,##0")
        val symbols = df.decimalFormatSymbols
        symbols.groupingSeparator = ' '
        df.decimalFormatSymbols = symbols
        return df.format(amount)
    }

    fun formatQty(qty: Double, unit: String): String {
        val isWhole = qty % 1.0 == 0.0
        val numStr = if (isWhole) {
            qty.toInt().toString()
        } else {
            String.format(java.util.Locale.US, "%.2f", qty)
        }
        // La table d'abréviations vit dans `UniteAbrege` — c'était la TROISIÈME copie, et la seule
        // qui divergeait dans un sens visible par le gérant : elle ne connaissait pas carton, sac,
        // boîte ni bouteille, donc le même produit se lisait « 3 Carton » dans l'écran Stock et
        // « Total: 3 ctn » sur sa carte d'accueil. Une omission, pas une intention : cette table
        // abrège déjà « paquet » en « paq ».
        val suffix = if (unit.isEmpty()) "" else " " + UniteAbrege.abreviation(unit)
        return numStr + suffix
    }
}
