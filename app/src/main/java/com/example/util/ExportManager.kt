package com.example.util

import android.content.Context
import android.net.Uri
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Canvas
import com.example.data.Transaction
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportManager {

    fun exportCsv(context: Context, uri: Uri, transactions: List<Transaction>) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val sb = StringBuilder()
                sb.append("ID,Date,Time,Type,Merchant,Category,Amount,RawMessage\n")
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

                for (tx in transactions) {
                    val date = dateFormat.format(Date(tx.timestamp))
                    val time = timeFormat.format(Date(tx.timestamp))
                    val merchant = "\"${tx.merchantName.replace("\"", "\"\"")}\""
                    val category = "\"${tx.category.replace("\"", "\"\"")}\""
                    val raw = "\"${tx.rawMessage.replace("\"", "\"\"")}\""
                    sb.append("${tx.id},$date,$time,${tx.type},$merchant,$category,${tx.amount},$raw\n")
                }
                outputStream.write(sb.toString().toByteArray())
                outputStream.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportPdf(context: Context, uri: Uri, transactions: List<Transaction>, title: String) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            paint.color = android.graphics.Color.BLACK
            paint.textSize = 18f
            paint.isFakeBoldText = true
            canvas.drawText("Mudrix Vault - Monthly Statement", 40f, 50f, paint)

            paint.textSize = 12f
            paint.isFakeBoldText = false
            canvas.drawText(title, 40f, 75f, paint)

            paint.color = android.graphics.Color.GRAY
            canvas.drawLine(40f, 90f, 555f, 90f, paint)

            var y = 120f
            paint.color = android.graphics.Color.BLACK
            paint.textSize = 10f

            canvas.drawText("Date & Time", 40f, y, paint)
            canvas.drawText("Merchant / Source", 160f, y, paint)
            canvas.drawText("Category", 340f, y, paint)
            canvas.drawText("Amount", 480f, y, paint)
            y += 20f

            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

            for (tx in transactions) {
                if (y > 800f) break
                val dateStr = dateFormat.format(Date(tx.timestamp))
                val typePrefix = if (tx.type == "CREDIT") "+₹" else "-₹"

                canvas.drawText(dateStr, 40f, y, paint)
                val merchantTrunc = if (tx.merchantName.length > 22) tx.merchantName.substring(0, 22) + "..." else tx.merchantName
                canvas.drawText(merchantTrunc, 160f, y, paint)
                canvas.drawText(tx.category, 340f, y, paint)
                canvas.drawText("$typePrefix${tx.amount}", 480f, y, paint)

                y += 20f
            }

            pdfDocument.finishPage(page)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
