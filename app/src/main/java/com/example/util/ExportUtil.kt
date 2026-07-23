package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.Debt
import com.example.data.model.MouvementCaisse
import com.example.data.model.Product
import com.example.data.model.Sale
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportFormat { CSV, PDF }

/**
 * Generic CSV (opens directly in Excel/Sheets) and PDF table export helpers, reused by every
 * screen that needs a detailed export (ventes, stock, dettes, mouvements de caisse). Follows the
 * exact same "save to app external dir + copy to public Downloads" pattern already used by
 * BarcodeUtil, so exported files are easy to find from any file explorer.
 */
object ExportUtil {

    private const val EXPORT_SUBDIR = "Exports"
    private const val PUBLIC_EXPORT_DIR = "EpicerieExports"

    /**
     * Writes a UTF-8 CSV file (with BOM so Excel renders accented FR/MG characters correctly).
     */
    fun exportToCsv(context: Context, fileBaseName: String, headers: List<String>, rows: List<List<String>>): File? {
        return try {
            val builder = StringBuilder()
            builder.append("﻿") // UTF-8 BOM so Excel renders accented FR/MG characters correctly
            builder.append(headers.joinToString(",") { csvEscape(it) }).append("\r\n")
            for (row in rows) {
                builder.append(row.joinToString(",") { csvEscape(it) }).append("\r\n")
            }
            saveExportFile(context, "$fileBaseName.csv", "text/csv") { outputStream ->
                outputStream.write(builder.toString().toByteArray(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            Log.e("ExportUtil", "Failed to export CSV", e)
            null
        }
    }

    private fun csvEscape(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    /**
     * Generates a paginated PDF table (A4 portrait) with a title and repeated column headers on
     * every page, following the same vector Canvas drawing approach already used for barcode PDFs.
     */
    fun exportToPdf(
        context: Context,
        fileBaseName: String,
        title: String,
        headers: List<String>,
        rows: List<List<String>>,
        columnWeights: List<Float>? = null
    ): File? {
        if (headers.isEmpty()) return null
        val weights = columnWeights ?: List(headers.size) { 1f }
        require(weights.size == headers.size) { "columnWeights must match headers size" }

        val pageWidth = 842 // A4 landscape at 72dpi, roomier for wide detailed tables
        val pageHeight = 595
        val leftMargin = 30f
        val rightMargin = 30f
        val topMargin = 70f
        val bottomMargin = 30f
        val rowHeight = 20f
        val headerRowHeight = 24f

        val tableWidth = pageWidth - leftMargin - rightMargin
        val totalWeight = weights.sum().takeIf { it > 0f } ?: 1f
        val columnWidths = weights.map { (it / totalWeight) * tableWidth }

        val rowsPerPage = ((pageHeight - topMargin - bottomMargin - headerRowHeight) / rowHeight).toInt().coerceAtLeast(1)
        val pageCount = if (rows.isEmpty()) 1 else Math.ceil(rows.size.toDouble() / rowsPerPage).toInt()

        val pdfDocument = PdfDocument()

        try {
            for (pageIndex in 0 until pageCount) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Title + page indicator
                val titlePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 16f
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                }
                canvas.drawText(title, leftMargin, 35f, titlePaint)

                val pagePaint = Paint().apply {
                    color = Color.DKGRAY
                    textSize = 10f
                    isAntiAlias = true
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText("${pageIndex + 1} / $pageCount", pageWidth - rightMargin, 35f, pagePaint)

                val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.FRANCE)
                val subtitlePaint = Paint().apply {
                    color = Color.GRAY
                    textSize = 9f
                    isAntiAlias = true
                }
                canvas.drawText("Exporté le ${dateFormat.format(java.util.Date())}", leftMargin, 50f, subtitlePaint)

                // Header row background
                var y = topMargin
                val headerBgPaint = Paint().apply { color = Color.rgb(230, 230, 230); style = Paint.Style.FILL }
                canvas.drawRect(leftMargin, y, pageWidth - rightMargin, y + headerRowHeight, headerBgPaint)

                val headerTextPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 10f
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                }
                var x = leftMargin
                for (colIndex in headers.indices) {
                    drawCellText(canvas, headers[colIndex], x + 4f, y + 16f, columnWidths[colIndex] - 8f, headerTextPaint)
                    x += columnWidths[colIndex]
                }
                y += headerRowHeight

                val borderPaint = Paint().apply { color = Color.LTGRAY; style = Paint.Style.STROKE; strokeWidth = 0.5f }
                canvas.drawLine(leftMargin, y, pageWidth - rightMargin, y, borderPaint)

                // Data rows for this page
                val startRow = pageIndex * rowsPerPage
                val endRow = minOf(startRow + rowsPerPage, rows.size)
                val cellPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 9.5f
                    isAntiAlias = true
                }
                for (rowIdx in startRow until endRow) {
                    val row = rows[rowIdx]
                    x = leftMargin
                    for (colIndex in headers.indices) {
                        val cellValue = row.getOrElse(colIndex) { "" }
                        drawCellText(canvas, cellValue, x + 4f, y + 15f, columnWidths[colIndex] - 8f, cellPaint)
                        x += columnWidths[colIndex]
                    }
                    y += rowHeight
                    canvas.drawLine(leftMargin, y, pageWidth - rightMargin, y, borderPaint)
                }

                if (rows.isEmpty()) {
                    val emptyPaint = Paint().apply {
                        color = Color.GRAY
                        textSize = 11f
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText("Aucune donnée à exporter", pageWidth / 2f, y + 30f, emptyPaint)
                }

                pdfDocument.finishPage(page)
            }

            return saveExportFile(context, "$fileBaseName.pdf", "application/pdf") { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
        } catch (e: Exception) {
            Log.e("ExportUtil", "Failed to export PDF", e)
            return null
        } finally {
            pdfDocument.close()
        }
    }

    /** Draws text truncated with an ellipsis if it doesn't fit within [maxWidth]. */
    private fun drawCellText(canvas: android.graphics.Canvas, text: String, x: Float, y: Float, maxWidth: Float, paint: Paint) {
        var display = text
        if (paint.measureText(display) > maxWidth) {
            while (display.isNotEmpty() && paint.measureText("$display…") > maxWidth) {
                display = display.dropLast(1)
            }
            display = "$display…"
        }
        canvas.drawText(display, x, y, paint)
    }

    /**
     * Saves the export both to the app's own external files directory (always accessible, used
     * for the share sheet via FileProvider) and to the public Downloads folder for easy discovery
     * from any file manager. Returns the app-local File used for sharing.
     */
    private fun saveExportFile(context: Context, fileName: String, mimeType: String, writer: (java.io.OutputStream) -> Unit): File? {
        val externalDir = context.getExternalFilesDir(EXPORT_SUBDIR) ?: context.filesDir
        if (!externalDir.exists()) externalDir.mkdirs()
        val appFile = File(externalDir, fileName)
        FileOutputStream(appFile).use { writer(it) }
        Log.d("ExportUtil", "Saved export locally to: ${appFile.absolutePath}")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + PUBLIC_EXPORT_DIR)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { writer(it) }
                }
            } else {
                val publicDownloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val exportDir = File(publicDownloadsDir, PUBLIC_EXPORT_DIR)
                if (!exportDir.exists()) exportDir.mkdirs()
                val publicFile = File(exportDir, fileName)
                FileOutputStream(publicFile).use { writer(it) }
            }
        } catch (e: Exception) {
            Log.e("ExportUtil", "Failed to save export to public Downloads folder", e)
        }

        return appFile
    }

    /**
     * Opens the standard Android share sheet for the given exported file, so the user can send it
     * via email, WhatsApp, Drive, or save it anywhere else, without needing storage permissions.
     */
    fun shareFile(context: Context, file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooserIntent = Intent.createChooser(sendIntent, null)
            if (context !is android.app.Activity) {
                // Starting an activity from a non-Activity context (e.g. Application) requires this flag.
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            Log.e("ExportUtil", "Failed to launch share sheet", e)
            android.widget.Toast.makeText(context, "Erreur de partage: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun mimeTypeFor(format: ExportFormat) = if (format == ExportFormat.CSV) "text/csv" else "application/pdf"

    private fun generateAndShare(
        context: Context,
        format: ExportFormat,
        fileBaseName: String,
        title: String,
        headers: List<String>,
        rows: List<List<String>>,
        columnWeights: List<Float>? = null
    ) {
        val file = when (format) {
            ExportFormat.CSV -> exportToCsv(context, fileBaseName, headers, rows)
            ExportFormat.PDF -> exportToPdf(context, fileBaseName, title, headers, rows, columnWeights)
        }
        if (file != null) {
            shareFile(context, file, mimeTypeFor(format))
        } else {
            android.widget.Toast.makeText(context, "Échec de l'export", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    /** Detailed export: one row per sold line item (not per sale), so every article is traceable. */
    fun exportSales(context: Context, sales: List<Sale>, format: ExportFormat) {
        val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
        val timeFmt = SimpleDateFormat("HH:mm", Locale.FRANCE)
        val headers = listOf("Date", "Heure", "Produit", "Quantité", "Prix unitaire (Ar)", "Total ligne (Ar)", "Total vente (Ar)")
        val rows = mutableListOf<List<String>>()
        for (sale in sales.sortedByDescending { it.timestamp }) {
            val date = Date(sale.timestamp)
            for (item in sale.items) {
                rows.add(
                    listOf(
                        dateFmt.format(date),
                        timeFmt.format(date),
                        item.name,
                        FormatUtil.formatQty(item.quantity, "").trim(),
                        FormatUtil.formatPrice(item.price),
                        FormatUtil.formatPrice(item.price * item.quantity),
                        FormatUtil.formatPrice(sale.totalAmount)
                    )
                )
            }
        }
        generateAndShare(
            context, format, "historique_ventes_${System.currentTimeMillis()}", "Historique des ventes",
            headers, rows, listOf(1.2f, 0.8f, 2.2f, 1f, 1.3f, 1.3f, 1.3f)
        )
    }

    fun exportStock(context: Context, products: List<Product>, format: ExportFormat) {
        val headers = listOf("Nom", "Catégorie", "SKU", "Code-barres", "Unité", "Stock actuel", "Seuil alerte", "Prix vente (Ar)", "Prix achat (Ar)")
        val rows = products.sortedBy { it.name.lowercase() }.map { p ->
            listOf(
                p.name,
                p.category,
                p.sku,
                p.barcode,
                p.unit,
                FormatUtil.formatQty(p.stock, "").trim(),
                FormatUtil.formatQty(p.lowStockThreshold, "").trim(),
                FormatUtil.formatPrice(p.price),
                FormatUtil.formatPrice(p.prixAchatUniteBase)
            )
        }
        generateAndShare(
            context, format, "inventaire_stock_${System.currentTimeMillis()}", "Inventaire / État des stocks",
            headers, rows, listOf(2f, 1.3f, 1.2f, 1.4f, 1f, 1f, 1f, 1.2f, 1.2f)
        )
    }

    fun exportDebts(context: Context, debts: List<Debt>, format: ExportFormat) {
        val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
        val headers = listOf("Débiteur", "Date", "Montant initial (Ar)", "Solde restant (Ar)", "Statut", "Note")
        val rows = debts.sortedByDescending { it.date }.map { d ->
            listOf(
                d.debtorName,
                dateFmt.format(Date(d.date)),
                FormatUtil.formatPrice(d.amount),
                FormatUtil.formatPrice(d.balance),
                if (d.isPaid) "Payée" else "En cours",
                d.note
            )
        }
        generateAndShare(
            context, format, "dettes_${System.currentTimeMillis()}", "Dettes clients",
            headers, rows, listOf(1.6f, 1f, 1.3f, 1.3f, 1f, 2f)
        )
    }

    fun exportCaisseMouvements(context: Context, mouvements: List<MouvementCaisse>, format: ExportFormat) {
        val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
        val headers = listOf("Date", "Type", "Motif", "Montant (Ar)", "Note")
        val rows = mouvements.sortedByDescending { it.date }.map { m ->
            listOf(
                dateFmt.format(Date(m.date)),
                if (m.type == "ENTREE") "Entrée" else "Sortie",
                m.motif,
                FormatUtil.formatPrice(m.montant),
                m.note
            )
        }
        generateAndShare(
            context, format, "mouvements_caisse_${System.currentTimeMillis()}", "Mouvements de caisse",
            headers, rows, listOf(1.4f, 1f, 1.6f, 1.2f, 2f)
        )
    }
}
