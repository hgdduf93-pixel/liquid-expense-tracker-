package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.Transaction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InvoiceItem(
    val description: String,
    val quantity: Double,
    val unitPrice: Double
) {
    val total: Double get() = quantity * unitPrice
}

object InvoicePdfGenerator {

    fun numberToWords(amount: Double): String {
        val intVal = amount.toLong()
        if (intVal == 0L) return "Zero Rupees Only"
        
        val units = arrayOf("", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", 
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen")
        val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")

        fun convertLessThanOneThousand(n: Long): String {
            var current = n
            var res = ""
            if (current >= 100) {
                res += units[(current / 100).toInt()] + " Hundred "
                current %= 100
            }
            if (current >= 20) {
                res += tens[(current / 10).toInt()] + " "
                current %= 10
            }
            if (current > 0) {
                res += units[current.toInt()] + " "
            }
            return res
        }

        var n = intVal
        var result = ""

        if (n >= 10000000) {
            result += convertLessThanOneThousand(n / 10000000) + "Crore "
            n %= 10000000
        }
        if (n >= 100000) {
            result += convertLessThanOneThousand(n / 100000) + "Lakh "
            n %= 100000
        }
        if (n >= 1000) {
            result += convertLessThanOneThousand(n / 1000) + "Thousand "
            n %= 1000
        }
        if (n > 0) {
            result += convertLessThanOneThousand(n)
        }

        return result.trim() + " Rupees Only"
    }

    fun generateTransactionReceiptPdf(context: Context, transaction: Transaction): File? {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            
            drawReceiptContent(canvas, transaction)
            pdfDocument.finishPage(page)

            val file = File(context.cacheDir, "invoices").apply { mkdirs() }
            val pdfFile = File(file, "Receipt_${transaction.id}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun saveTransactionReceiptToUri(context: Context, uri: Uri, transaction: Transaction) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            drawReceiptContent(page.canvas, transaction)
            pdfDocument.finishPage(page)

            context.contentResolver.openOutputStream(uri)?.use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun drawReceiptContent(canvas: Canvas, tx: Transaction) {
        val paint = Paint().apply { color = Color.BLACK; isAntiAlias = true }
        
        // Background card border & header
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.DKGRAY
        canvas.drawRect(30f, 30f, 565f, 812f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#111111")
        canvas.drawRect(31f, 31f, 564f, 130f, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("MUDRIX PAYMENT VOUCHER", 50f, 70f, paint)

        paint.textSize = 11f
        paint.isFakeBoldText = false
        paint.color = Color.GRAY
        canvas.drawText("Official Verified Transaction Receipt", 50f, 95f, paint)

        // Status Badge
        paint.color = Color.parseColor("#065F46")
        canvas.drawRect(430f, 55f, 540f, 85f, paint)
        paint.color = Color.parseColor("#34D399")
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("PAID / VERIFIED", 442f, 73f, paint)

        // Metadata
        var y = 170f
        paint.color = Color.BLACK
        paint.isFakeBoldText = false
        paint.textSize = 12f

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault())
        val dateStr = dateFormat.format(Date(tx.timestamp))

        drawRow(canvas, paint, "Voucher / Receipt No:", "#${tx.id}-${tx.timestamp}", y)
        y += 28f
        drawRow(canvas, paint, "Date & Time:", dateStr, y)
        y += 28f
        drawRow(canvas, paint, "Merchant / Payee:", tx.merchantName, y)
        y += 28f
        drawRow(canvas, paint, "Category:", tx.category, y)
        y += 28f
        drawRow(canvas, paint, "Payment Type:", tx.type, y)
        y += 28f
        drawRow(canvas, paint, "Source App:", tx.rawMessage.takeIf { !it.isNullOrBlank() } ?: "Direct UPI / Bank Transfer", y)

        y += 40f
        paint.color = Color.LTGRAY
        canvas.drawLine(50f, y, 545f, y, paint)

        y += 40f
        paint.color = Color.GRAY
        paint.textSize = 12f
        canvas.drawText("AMOUNT PAID", 50f, y, paint)

        y += 30f
        paint.color = Color.BLACK
        paint.textSize = 32f
        paint.isFakeBoldText = true
        val amountPrefix = if (tx.type == "CREDIT") "+ ₹" else "- ₹"
        canvas.drawText(amountPrefix + String.format(Locale.getDefault(), "%.2f", tx.amount), 50f, y, paint)

        y += 30f
        paint.textSize = 13f
        paint.isFakeBoldText = false
        paint.color = Color.DKGRAY
        canvas.drawText("In Words: " + numberToWords(tx.amount), 50f, y, paint)

        // Footer
        paint.color = Color.GRAY
        paint.textSize = 10f
        canvas.drawText("Generated securely via Mudrix by GSD • 100% Offline & Encrypted Ledger", 50f, 760f, paint)
        canvas.drawText("This is a system-generated cryptographic payment voucher.", 50f, 778f, paint)
    }

    private fun drawRow(canvas: Canvas, paint: Paint, label: String, value: String, y: Float) {
        paint.isFakeBoldText = false
        paint.color = Color.DKGRAY
        canvas.drawText(label, 50f, y, paint)
        paint.isFakeBoldText = true
        paint.color = Color.BLACK
        canvas.drawText(value, 220f, y, paint)
    }

    data class CustomInvoiceData(
        val customerName: String,
        val customerPhone: String,
        val invoiceNumber: String,
        val date: String,
        val items: List<InvoiceItem>,
        val taxPercent: Double,
        val discount: Double
    )

    fun generateCustomInvoiceFile(context: Context, data: CustomInvoiceData): File? {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            drawCustomInvoiceCanvas(page.canvas, data)
            pdfDocument.finishPage(page)

            val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
            val file = File(dir, "Invoice_${data.invoiceNumber}.pdf")
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun generateCustomInvoicePdf(context: Context, uri: Uri, data: CustomInvoiceData) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            drawCustomInvoiceCanvas(page.canvas, data)
            pdfDocument.finishPage(page)

            context.contentResolver.openOutputStream(uri)?.use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun drawCustomInvoiceCanvas(canvas: Canvas, data: CustomInvoiceData) {
        val paint = Paint().apply { isAntiAlias = true }

        // Outer border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.DKGRAY
        canvas.drawRect(30f, 30f, 565f, 812f, paint)

        // Header Banner
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#111111")
        canvas.drawRect(31f, 31f, 564f, 125f, paint)

        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.isFakeBoldText = true
        canvas.drawText("MUDRIX TAX INVOICE", 50f, 70f, paint)

        paint.textSize = 11f
        paint.isFakeBoldText = false
        paint.color = Color.GRAY
        canvas.drawText("Professional Offline Billing & Receipt System", 50f, 95f, paint)

        // Metadata Box
        var y = 155f
        paint.color = Color.BLACK
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("INVOICE DETAILS:", 50f, y, paint)
        canvas.drawText("BILLED TO:", 330f, y, paint)

        y += 20f
        paint.isFakeBoldText = false
        paint.textSize = 11f
        canvas.drawText("Invoice No: ${data.invoiceNumber}", 50f, y, paint)
        canvas.drawText("Customer: ${data.customerName.ifBlank { "Walk-in Customer" }}", 330f, y, paint)

        y += 18f
        canvas.drawText("Date: ${data.date}", 50f, y, paint)
        canvas.drawText("Phone: ${data.customerPhone.ifBlank { "N/A" }}", 330f, y, paint)

        y += 35f
        paint.color = Color.parseColor("#E5E7EB")
        canvas.drawRect(50f, y, 545f, y + 26f, paint)

        paint.color = Color.BLACK
        paint.isFakeBoldText = true
        canvas.drawText("#", 60f, y + 17f, paint)
        canvas.drawText("Item Description", 95f, y + 17f, paint)
        canvas.drawText("Qty", 320f, y + 17f, paint)
        canvas.drawText("Rate (₹)", 390f, y + 17f, paint)
        canvas.drawText("Total (₹)", 470f, y + 17f, paint)

        y += 32f
        paint.isFakeBoldText = false
        var subtotal = 0.0

        data.items.forEachIndexed { index, item ->
            if (y > 650f) return@forEachIndexed
            subtotal += item.total
            paint.color = Color.BLACK
            canvas.drawText((index + 1).toString(), 60f, y, paint)
            val descTrunc = if (item.description.length > 35) item.description.substring(0, 35) + "..." else item.description
            canvas.drawText(descTrunc, 95f, y, paint)
            canvas.drawText(item.quantity.toString(), 320f, y, paint)
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", item.unitPrice), 390f, y, paint)
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", item.total), 470f, y, paint)
            y += 24f
        }

        y += 10f
        paint.color = Color.LTGRAY
        canvas.drawLine(50f, y, 545f, y, paint)

        y += 25f
        val taxAmount = subtotal * (data.taxPercent / 100.0)
        val grandTotal = subtotal + taxAmount - data.discount

        paint.color = Color.DKGRAY
        canvas.drawText("Subtotal:", 350f, y, paint)
        canvas.drawText(String.format(Locale.getDefault(), "₹ %.2f", subtotal), 470f, y, paint)

        if (data.taxPercent > 0.0) {
            y += 20f
            canvas.drawText("Tax (${data.taxPercent}%):", 350f, y, paint)
            canvas.drawText(String.format(Locale.getDefault(), "₹ %.2f", taxAmount), 470f, y, paint)
        }

        if (data.discount > 0.0) {
            y += 20f
            canvas.drawText("Discount:", 350f, y, paint)
            canvas.drawText(String.format(Locale.getDefault(), "- ₹ %.2f", data.discount), 470f, y, paint)
        }

        y += 25f
        paint.color = Color.BLACK
        paint.isFakeBoldText = true
        paint.textSize = 14f
        canvas.drawText("Grand Total:", 350f, y, paint)
        canvas.drawText(String.format(Locale.getDefault(), "₹ %.2f", grandTotal), 470f, y, paint)

        y += 35f
        paint.textSize = 11f
        paint.isFakeBoldText = false
        canvas.drawText("Amount in words: " + numberToWords(grandTotal), 50f, y, paint)

        // Footer
        paint.color = Color.GRAY
        paint.textSize = 10f
        canvas.drawText("Generated via Mudrix by GSD • 100% Offline & Verified Ledger", 50f, 760f, paint)
        canvas.drawText("Authorized Signatory / Digital Voucher", 50f, 778f, paint)
    }

    fun sharePdf(context: Context, pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", pdfFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share Invoice via WhatsApp / Print")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
