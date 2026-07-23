package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.print.PrintAttributes
import android.print.PrintManager
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

object BarcodeUtil {

    // Code 39 character mapping
    private val CODE39_MAP = mapOf(
        '0' to "NNNWWNWNN",
        '1' to "WNNWNNNNW",
        '2' to "NNWWNNNNW",
        '3' to "WNWWNNNNN",
        '4' to "NNNWWNNNW",
        '5' to "WNNWWNNNN",
        '6' to "NNWWWNNNN",
        '7' to "NNNWNNWNW",
        '8' to "WNNWNNWNN",
        '9' to "NNWWNNWNN",
        ' ' to "NWNNNWNWN",
        '*' to "NWNNWNNNW" // Start/Stop
    )

    /**
     * Our Code 39 table only encodes digits, space and the start/stop character. Any other
     * character (letters, symbols) cannot be represented and must never be silently dropped,
     * since that would print a barcode label that doesn't match the product's real code.
     */
    fun isFullyEncodableAsCode39(text: String): Boolean {
        val cleaned = text.trim().uppercase()
        return cleaned.isNotEmpty() && cleaned.all { CODE39_MAP.containsKey(it) }
    }

    /**
     * Translates a string into a list of booleans representing narrow (false) and wide (true) alternating elements of Code 39.
     * true = black bar, false = white space
     */
    fun generateCode39Pattern(text: String): List<Boolean> {
        // Reject rather than silently drop unsupported characters: a partial pattern would look
        // like a valid barcode but would not scan back to the actual stored value.
        if (!isFullyEncodableAsCode39(text)) return emptyList()

        val uppercaseText = text.trim().uppercase()

        val fullText = "*$uppercaseText*"
        val booleanList = mutableListOf<Boolean>()

        for (charIndex in fullText.indices) {
            val char = fullText[charIndex]
            val pattern = CODE39_MAP[char] ?: continue

            // Each character pattern has 9 elements: alternating bar/space, starting with a bar (index 0 is bar, 1 is space, etc.)
            for (i in 0 until 9) {
                val isBar = (i % 2 == 0)
                val isWide = (pattern[i] == 'W')
                val elementWidth = if (isWide) 3 else 1

                for (w in 0 until elementWidth) {
                    booleanList.add(isBar)
                }
            }

            // Append inter-character gap (narrow space) except after the last character
            if (charIndex < fullText.length - 1) {
                booleanList.add(false) // narrow space
            }
        }

        return booleanList
    }

    /**
     * Translates a 13-digit EAN-13 barcode string into a list of booleans representing the 95 binary modules.
     * true = black bar, false = white space
     */
    fun generateEan13Pattern(barcode: String): List<Boolean> {
        if (barcode.length != 13 || !barcode.all { it.isDigit() }) return emptyList()

        val booleanList = mutableListOf<Boolean>()

        val lCode = arrayOf(
            "0001101", "0011001", "0010011", "0111101", "0100011",
            "0110001", "0101111", "0111011", "0110111", "0001011"
        )
        val gCode = arrayOf(
            "0100111", "0110011", "0011011", "0100001", "0011101",
            "0111001", "0000101", "0010001", "0001001", "0010111"
        )
        val rCode = arrayOf(
            "1110010", "1100110", "1101100", "1000010", "1011100",
            "1001110", "1010000", "1000100", "1001000", "1110100"
        )

        val parityPatterns = arrayOf(
            "LLLLLL", // 0
            "LLGLGG", // 1
            "LLGGLG", // 2
            "LLGGGL", // 3
            "LGLLGG", // 4
            "LGGLLG", // 5
            "LGGGLL", // 6
            "LGLGLG", // 7
            "LGLGGL", // 8
            "LGGLGL"  // 9
        )

        val firstDigit = barcode[0].toString().toInt()
        val parity = parityPatterns.getOrNull(firstDigit) ?: "LLLLLL"

        // Left Guard: 101
        booleanList.add(true)
        booleanList.add(false)
        booleanList.add(true)

        // Left 6 digits (indices 1 to 6)
        for (i in 1..6) {
            val digit = barcode[i].toString().toInt()
            val codeType = parity[i - 1]
            val patternStr = if (codeType == 'L') lCode[digit] else gCode[digit]
            for (char in patternStr) {
                booleanList.add(char == '1')
            }
        }

        // Center Guard: 01010
        booleanList.add(false)
        booleanList.add(true)
        booleanList.add(false)
        booleanList.add(true)
        booleanList.add(false)

        // Right 6 digits (indices 7 to 12)
        for (i in 7..12) {
            val digit = barcode[i].toString().toInt()
            val patternStr = rCode[digit]
            for (char in patternStr) {
                booleanList.add(char == '1')
            }
        }

        // Right Guard: 101
        booleanList.add(true)
        booleanList.add(false)
        booleanList.add(true)

        return booleanList
    }

    /**
     * Generates a vector PDF containing the barcode and product name.
     * Saves the PDF to public Downloads directory and app's external files directory.
     * Returns the File in the app's files directory.
     */
    fun generateBarcodePdf(context: Context, productName: String, barcodeValue: String): File? {
        if (barcodeValue.isBlank()) return null

        val pdfDocument = PdfDocument()
        
        // standard label dimensions: 300 x 180 points (approx 4.16" x 2.5" at 72dpi)
        val pageWidth = 300
        val pageHeight = 180
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        try {
            // Draw Product Name (Centered)
            val namePaint = Paint().apply {
                color = Color.BLACK
                textSize = 14f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                style = Paint.Style.FILL
            }
            // Smart truncate for very long names to fit nicely on the label
            val displayProductName = if (productName.length > 28) {
                productName.take(25) + "..."
            } else if (productName.isBlank()) {
                "Produit Sans Nom"
            } else {
                productName
            }
            canvas.drawText(displayProductName, pageWidth / 2f, 35f, namePaint)

            // Generate Barcode Bit List
            val isEan = (barcodeValue.length == 13 && barcodeValue.all { it.isDigit() })
            val pattern = if (isEan) generateEan13Pattern(barcodeValue) else generateCode39Pattern(barcodeValue)
            if (pattern.isNotEmpty()) {
                val maxBarcodeWidth = 260f
                val unitWidth = if (isEan) minOf(2.5f, maxBarcodeWidth / pattern.size) else minOf(1.3f, maxBarcodeWidth / pattern.size)
                val totalBarcodeWidth = pattern.size * unitWidth
                val startX = (pageWidth - totalBarcodeWidth) / 2f
                val barTop = 50f
                val barBottom = 120f

                val barPaint = Paint().apply {
                    color = Color.BLACK
                    style = Paint.Style.FILL
                    isAntiAlias = false // crisp vector edges
                }

                for (i in pattern.indices) {
                    if (pattern[i]) { // black bar
                        val left = startX + (i * unitWidth)
                        val right = left + unitWidth
                        val isGuard = isEan && (i in 0..2 || i in 45..49 || i in 92..94)
                        val bottom = if (isGuard) barBottom + 8f else barBottom
                        canvas.drawRect(left, barTop, right, bottom, barPaint)
                    }
                }

                // Draw Readable Text below barcode
                if (isEan) {
                    val textPaint = Paint().apply {
                        color = Color.BLACK
                        textSize = 12f
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL)
                    }
                    val firstDigitPaint = Paint(textPaint).apply { textAlign = Paint.Align.RIGHT }
                    canvas.drawText(barcodeValue.take(1), startX - 6f, barBottom + 12f, firstDigitPaint)
                    canvas.drawText(barcodeValue.substring(1, 7), startX + 24 * unitWidth, barBottom + 12f, textPaint)
                    canvas.drawText(barcodeValue.substring(7, 13), startX + 71 * unitWidth, barBottom + 12f, textPaint)
                } else {
                    val textPaint = Paint().apply {
                        color = Color.BLACK
                        textSize = 10f
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                        letterSpacing = 0.15f
                    }
                    canvas.drawText(barcodeValue, pageWidth / 2f, 145f, textPaint)
                }
            } else {
                // If pattern is empty, draw an error message
                val errorPaint = Paint().apply {
                    color = Color.RED
                    textSize = 11f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("Erreur: Code-barres non conforme (Chiffres/Majuscules uniquement)", pageWidth / 2f, 90f, errorPaint)
            }

            // Draw clean subtle border for alignment/cutting
            val borderPaint = Paint().apply {
                color = Color.LTGRAY
                style = Paint.Style.STROKE
                strokeWidth = 1f
                pathEffect = android.graphics.DashPathEffect(floatArrayOf(5f, 5f), 0f)
            }
            canvas.drawRect(8f, 8f, (pageWidth - 8).toFloat(), (pageHeight - 8).toFloat(), borderPaint)

            pdfDocument.finishPage(page)

            // Step 1: Save to internal external files directory (always safe)
            val externalDir = context.getExternalFilesDir("Barcodes") ?: context.filesDir
            if (!externalDir.exists()) {
                externalDir.mkdirs()
            }
            val appFile = File(externalDir, "Barcode_${barcodeValue}.pdf")
            pdfDocument.writeTo(FileOutputStream(appFile))
            Log.d("BarcodeUtil", "Saved PDF locally to: ${appFile.absolutePath}")

            // Step 2: Save to public Downloads directory (so user can find/print easily from any file explorer)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "Barcode_${barcodeValue}.pdf")
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/EpicerieBarcodes")
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            pdfDocument.writeTo(outputStream)
                        }
                        Log.d("BarcodeUtil", "Saved PDF to public Downloads via MediaStore")
                    }
                } else {
                    val publicDownloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val epicerieDir = File(publicDownloadsDir, "EpicerieBarcodes")
                    if (!epicerieDir.exists()) {
                        epicerieDir.mkdirs()
                    }
                    val publicFile = File(epicerieDir, "Barcode_${barcodeValue}.pdf")
                    pdfDocument.writeTo(FileOutputStream(publicFile))
                    Log.d("BarcodeUtil", "Saved PDF to public Downloads directly: ${publicFile.absolutePath}")
                }
            } catch (e: Exception) {
                Log.e("BarcodeUtil", "Failed to save PDF to public Downloads folder", e)
            }

            return appFile

        } catch (e: Exception) {
            Log.e("BarcodeUtil", "Error generating PDF", e)
            return null
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Calculates the mathematically correct EAN-13 checksum for a given 12-digit base barcode.
     */
    fun calculateEan13Checksum(barcode12: String): Int {
        if (barcode12.length != 12 || !barcode12.all { it.isDigit() }) return 0
        var sum = 0
        for (i in 0 until 12) {
            val digit = barcode12[i].toString().toInt()
            sum += if (i % 2 == 1) digit * 3 else digit
        }
        val mod = sum % 10
        return if (mod == 0) 0 else 10 - mod
    }

    /**
     * Generates a standard-compliant 13-digit EAN-13 barcode using Madagascar/European prefixes
     * with a mathematically correct checksum.
     */
    fun generateStandardBarcode(): String {
        val randomPrefix = listOf("611", "301", "325", "400").random()
        val randomDigits = (100000000..999999999).random().toString().padStart(9, '0')
        val barcode12 = randomPrefix + randomDigits
        val checksum = calculateEan13Checksum(barcode12)
        return barcode12 + checksum
    }

    /**
     * Helper to verify if a barcode was generated by our application.
     * Checks if length is 13, strictly numeric, and starts with one of the prefixes we use.
     */
    fun isGeneratedBarcode(barcode: String?): Boolean {
        if (barcode == null || barcode.length != 13) return false
        val prefixes = listOf("611", "301", "325", "400")
        return prefixes.any { barcode.startsWith(it) } && barcode.all { it.isDigit() }
    }

    /**
     * Generates a single consolidated PDF document (varotra_code_barre.pdf) containing
     * the barcode labels in a spacious 2-column grid layout (2 columns, 5 rows per page).
     * Includes ONLY barcodes generated by the app (filtering out pre-existing scanned codes).
     * Automatically handles pagination so that no label is cut off.
     */
    fun generateConsolidatedBarcodePdf(context: Context, products: List<com.example.data.model.Product>): File? {
        val validProducts = products.filter { !it.barcode.isNullOrBlank() && isGeneratedBarcode(it.barcode) }.sortedWith(compareBy({ it.category.lowercase() }, { it.name.lowercase() }))
        if (validProducts.isEmpty()) return null

        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val topMargin = 50f
        val leftMargin = 35f
        val colWidth = 245f
        val colGap = 35f
        val rowHeight = 130f
        val rowGap = 22f

        var currentPage: PdfDocument.Page? = null
        var canvas: android.graphics.Canvas? = null

        try {
            for (idx in validProducts.indices) {
                val p = validProducts[idx]
                val itemOnPage = idx % 10
                val pageIndex = idx / 10

                if (itemOnPage == 0) {
                    if (currentPage != null) {
                        pdfDocument.finishPage(currentPage)
                    }
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                    currentPage = pdfDocument.startPage(pageInfo)
                    canvas = currentPage.canvas
                }

                val row = itemOnPage / 2
                val col = itemOnPage % 2
                val x = leftMargin + col * (colWidth + colGap)
                val y = topMargin + row * (rowHeight + rowGap)

                canvas?.let { cv ->
                    // 1. Draw cut outline (light dashed)
                    val borderPaint = Paint().apply {
                        color = Color.LTGRAY
                        style = Paint.Style.STROKE
                        strokeWidth = 0.5f
                        pathEffect = android.graphics.DashPathEffect(floatArrayOf(4f, 4f), 0f)
                    }
                    cv.drawRect(x, y, x + colWidth, y + rowHeight, borderPaint)

                    // 2. Draw Product Name (Centered horizontally at top of label)
                    val namePaint = Paint().apply {
                        color = Color.BLACK
                        textSize = 11f
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                        style = Paint.Style.FILL
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    }
                    val displayName = if (p.name.length > 30) {
                        p.name.take(27) + "..."
                    } else if (p.name.isBlank()) {
                        "Produit sans nom"
                    } else {
                        p.name
                    }
                    cv.drawText(displayName, x + colWidth / 2f, y + 25f, namePaint)

                    // 3. Draw Barcode Pattern (centered)
                    val isEan = (p.barcode?.length == 13 && p.barcode.all { it.isDigit() })
                    val pattern = if (isEan) generateEan13Pattern(p.barcode ?: "") else generateCode39Pattern(p.barcode ?: "")
                    if (pattern.isNotEmpty()) {
                        val maxBarcodeWidth = 220f
                        val unitWidth = if (isEan) minOf(2.2f, maxBarcodeWidth / pattern.size) else minOf(1.8f, maxBarcodeWidth / pattern.size)
                        val totalBarcodeWidth = pattern.size * unitWidth
                        val startX = x + (colWidth - totalBarcodeWidth) / 2f
                        val barTop = y + 36f
                        val barBottom = y + 94f

                        val barPaint = Paint().apply {
                            color = Color.BLACK
                            style = Paint.Style.FILL
                            isAntiAlias = false
                        }

                        for (i in pattern.indices) {
                            if (pattern[i]) {
                                val left = startX + (i * unitWidth)
                                val right = left + unitWidth
                                val isGuard = isEan && (i in 0..2 || i in 45..49 || i in 92..94)
                                val bottom = if (isGuard) barBottom + 6f else barBottom
                                cv.drawRect(left, barTop, right, bottom, barPaint)
                            }
                        }

                        // 4. Draw readable barcode value
                        if (isEan) {
                            val textPaint = Paint().apply {
                                color = Color.BLACK
                                textSize = 9f
                                isAntiAlias = true
                                textAlign = Paint.Align.CENTER
                                typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL)
                            }
                            val firstDigitPaint = Paint(textPaint).apply { textAlign = Paint.Align.RIGHT }
                            cv.drawText(p.barcode.take(1), startX - 5f, barBottom + 10f, firstDigitPaint)
                            cv.drawText(p.barcode.substring(1, 7), startX + 24 * unitWidth, barBottom + 10f, textPaint)
                            cv.drawText(p.barcode.substring(7, 13), startX + 71 * unitWidth, barBottom + 10f, textPaint)
                        } else {
                            val textPaint = Paint().apply {
                                color = Color.BLACK
                                textSize = 10f
                                isAntiAlias = true
                                textAlign = Paint.Align.CENTER
                                letterSpacing = 0.12f
                            }
                            cv.drawText(p.barcode ?: "", x + colWidth / 2f, y + 110f, textPaint)
                        }
                    } else {
                        // Error fallback
                        val errorPaint = Paint().apply {
                            color = Color.RED
                            textSize = 9f
                            isAntiAlias = true
                            textAlign = Paint.Align.CENTER
                        }
                        cv.drawText("Format Invalide", x + colWidth / 2f, y + 65f, errorPaint)
                    }

                    // 5. Draw Price (bottom right corner) & Category/Unit info (bottom left corner)
                    val pricePaint = Paint().apply {
                        color = Color.BLACK
                        textSize = 10f
                        isAntiAlias = true
                        textAlign = Paint.Align.RIGHT
                        style = Paint.Style.FILL
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    }
                    val priceStr = String.format("%,.0f Ar", p.price)
                    cv.drawText(priceStr, x + colWidth - 12f, y + 110f, pricePaint)

                    val unitPaint = Paint().apply {
                        color = Color.GRAY
                        textSize = 8f
                        isAntiAlias = true
                        textAlign = Paint.Align.LEFT
                        style = Paint.Style.FILL
                    }
                    val unitStr = p.unit.ifEmpty { "Pcs" }
                    val catStr = p.category.ifBlank { "Misc" }
                    val categoryAndUnit = "${catStr.uppercase()} • ${unitStr.uppercase()}"
                    cv.drawText(categoryAndUnit, x + 12f, y + 110f, unitPaint)
                }
            }

            if (currentPage != null) {
                pdfDocument.finishPage(currentPage)
            }

            // Step 1: Save internally
            val externalDir = context.getExternalFilesDir("Barcodes") ?: context.filesDir
            if (!externalDir.exists()) {
                externalDir.mkdirs()
            }
            val appFile = File(externalDir, "varotra_code_barre.pdf")
            pdfDocument.writeTo(FileOutputStream(appFile))
            Log.d("BarcodeUtil", "Saved consolidated PDF locally to: ${appFile.absolutePath}")

            // Step 2: Save to public Downloads directory as requested
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val projection = arrayOf(MediaStore.MediaColumns._ID)
                    val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
                    val selectionArgs = arrayOf("varotra_code_barre.pdf", Environment.DIRECTORY_DOWNLOADS + "/EpicerieBarcodes/")
                    resolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                            val uri = Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
                            resolver.delete(uri, null, null)
                        }
                    }

                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "varotra_code_barre.pdf")
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/EpicerieBarcodes")
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            pdfDocument.writeTo(outputStream)
                        }
                        Log.d("BarcodeUtil", "Saved consolidated PDF to public Downloads via MediaStore")
                    }
                } else {
                    val publicDownloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val epicerieDir = File(publicDownloadsDir, "EpicerieBarcodes")
                    if (!epicerieDir.exists()) {
                        epicerieDir.mkdirs()
                    }
                    val publicFile = File(epicerieDir, "varotra_code_barre.pdf")
                    pdfDocument.writeTo(FileOutputStream(publicFile))
                    Log.d("BarcodeUtil", "Saved consolidated PDF to public Downloads directly: ${publicFile.absolutePath}")
                }
            } catch (e: Exception) {
                Log.e("BarcodeUtil", "Failed to save consolidated PDF to public Downloads folder", e)
            }

            return appFile

        } catch (e: Exception) {
            Log.e("BarcodeUtil", "Error generating consolidated PDF", e)
            return null
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Launches the standard Android Print service interface for the generated PDF.
     */
    fun printBarcode(context: Context, file: File) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val jobName = "Epicerie Barcode Print - ${file.name}"
            
            val printAdapter = object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }

                    val info = PrintDocumentInfo.Builder(file.name)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                        .build()

                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out android.print.PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    var input: FileInputStream? = null
                    var output: FileOutputStream? = null

                    try {
                        input = FileInputStream(file)
                        output = FileOutputStream(destination?.fileDescriptor)

                        val buf = ByteArray(1024)
                        var bytesRead: Int
                        while (input.read(buf).also { bytesRead = it } >= 0) {
                            if (cancellationSignal?.isCanceled == true) {
                                callback?.onWriteCancelled()
                                return
                            }
                            output.write(buf, 0, bytesRead)
                        }

                        callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))

                    } catch (e: Exception) {
                        Log.e("BarcodeUtil", "Error writing print spool", e)
                        callback?.onWriteFailed(e.message)
                    } finally {
                        input?.close()
                        output?.close()
                    }
                }
            }

            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
        } catch (e: Exception) {
            Log.e("BarcodeUtil", "Failed to invoke Android print manager", e)
            Toast.makeText(context, "Erreur d'impression: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
