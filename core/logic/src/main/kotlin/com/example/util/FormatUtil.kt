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
        val suffix = when (unit.lowercase()) {
            "kilogramme", "kg" -> " kg"
            "litre", "l" -> " L"
            "pièce", "piece", "pcs" -> " pcs"
            "paquet" -> " paq"
            "tasse", "kapoaka" -> " kap"
            else -> " $unit"
        }
        return numStr + suffix
    }
}
