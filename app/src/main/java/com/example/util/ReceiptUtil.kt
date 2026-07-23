package com.example.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.util.Log
import com.example.ui.viewmodel.CartItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates a printable cash register receipt (ticket de caisse) as a narrow-width PDF, sized like
 * a thermal receipt roll, using the same vector Canvas/PdfDocument approach as BarcodeUtil so it
 * can be sent straight to BarcodeUtil.printBarcode() (a generic PDF print helper despite its name).
 */
object ReceiptUtil {

    private const val PAGE_WIDTH = 226 // ~80mm thermal receipt width at 72dpi

    fun generateReceiptPdf(
        context: Context,
        groceryName: String,
        items: List<CartItem>,
        totalAmount: Double,
        amountReceived: Double,
        change: Double,
        timestamp: Long = System.currentTimeMillis()
    ): File? {
        if (items.isEmpty()) return null

        val lineHeight = 16f
        val headerHeight = 90f
        val footerHeight = 90f
        val pageHeight = (headerHeight + items.size * lineHeight + footerHeight).toInt().coerceAtLeast(250)

        val pdfDocument = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val center = PAGE_WIDTH / 2f
            var y = 22f

            val titlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 13f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            canvas.drawText(groceryName.ifBlank { "Varotra" }, center, y, titlePaint)
            y += 16f

            val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
            val subPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 8f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(dateFmt.format(Date(timestamp)), center, y, subPaint)
            y += 16f

            val dashPaint = Paint().apply {
                color = Color.BLACK
                strokeWidth = 1f
                pathEffect = android.graphics.DashPathEffect(floatArrayOf(3f, 3f), 0f)
            }
            canvas.drawLine(10f, y, PAGE_WIDTH - 10f, y, dashPaint)
            y += 14f

            val itemNamePaint = Paint().apply {
                color = Color.BLACK
                textSize = 8.5f
                isAntiAlias = true
            }
            val itemAmountPaint = Paint().apply {
                color = Color.BLACK
                textSize = 8.5f
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }
            for (item in items) {
                val qtyStr = FormatUtil.formatQty(item.quantity, "").trim()
                val label = "$qtyStr x ${item.name}"
                val truncated = if (itemNamePaint.measureText(label) > 140f) {
                    var s = label
                    while (s.isNotEmpty() && itemNamePaint.measureText("$s…") > 140f) s = s.dropLast(1)
                    "$s…"
                } else label
                canvas.drawText(truncated, 10f, y, itemNamePaint)
                canvas.drawText(FormatUtil.formatPrice(item.totalPrice), PAGE_WIDTH - 10f, y, itemAmountPaint)
                y += lineHeight
            }

            canvas.drawLine(10f, y, PAGE_WIDTH - 10f, y, dashPaint)
            y += 16f

            val totalLabelPaint = Paint().apply {
                color = Color.BLACK
                textSize = 11f
                isAntiAlias = true
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            val totalAmountPaint = Paint(totalLabelPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText("TOTAL", 10f, y, totalLabelPaint)
            canvas.drawText("${FormatUtil.formatPrice(totalAmount)} Ar", PAGE_WIDTH - 10f, y, totalAmountPaint)
            y += 16f

            if (amountReceived > 0.0) {
                val smallLabelPaint = Paint().apply { color = Color.DKGRAY; textSize = 8.5f; isAntiAlias = true }
                val smallAmountPaint = Paint(smallLabelPaint).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText("Espèces reçues", 10f, y, smallLabelPaint)
                canvas.drawText("${FormatUtil.formatPrice(amountReceived)} Ar", PAGE_WIDTH - 10f, y, smallAmountPaint)
                y += 14f
                canvas.drawText("Monnaie rendue", 10f, y, smallLabelPaint)
                canvas.drawText("${FormatUtil.formatPrice(change)} Ar", PAGE_WIDTH - 10f, y, smallAmountPaint)
                y += 20f
            }

            val thanksPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 8f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Misaotra ! Merci ! Thank you!", center, y, thanksPaint)

            pdfDocument.finishPage(page)

            val externalDir = context.getExternalFilesDir("Receipts") ?: context.filesDir
            if (!externalDir.exists()) externalDir.mkdirs()
            val file = File(externalDir, "recu_${timestamp}.pdf")
            pdfDocument.writeTo(FileOutputStream(file))
            return file
        } catch (e: Exception) {
            Log.e("ReceiptUtil", "Failed to generate receipt PDF", e)
            return null
        } finally {
            pdfDocument.close()
        }
    }
}
