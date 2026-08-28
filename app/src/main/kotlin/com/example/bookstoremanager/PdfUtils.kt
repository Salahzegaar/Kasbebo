package com.example.bookstoremanager

import com.example.bookstoremanager.data.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

data class PdfInvoiceData(
    val storeName: String,
    val customerName: String,
    val date: String,
    val items: List<CartItem>,
    val discountAmt: Double,
    val totalAmt: Double
)

object PdfGenerator {
    fun generateInvoicePdf(context: Context, uri: Uri, data: PdfInvoiceData) {
        try {
            val document = android.graphics.pdf.PdfDocument()
            val pageWidth = 595f; val pageHeight = 842f
            val primaryColor = android.graphics.Color.parseColor("#283593")
            val secondaryColor = android.graphics.Color.parseColor("#E8EAF6")
            val textColor = android.graphics.Color.BLACK

            val titlePaint = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 28f; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true }
            val headerTextPaint = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 14f; textAlign = android.graphics.Paint.Align.RIGHT; isFakeBoldText = true }
            val normalTextPaint = android.graphics.Paint().apply { color = textColor; textSize = 12f; textAlign = android.graphics.Paint.Align.RIGHT }
            val normalTextCenterPaint = android.graphics.Paint().apply { color = textColor; textSize = 12f; textAlign = android.graphics.Paint.Align.CENTER }
            val linePaint = android.graphics.Paint().apply { color = android.graphics.Color.LTGRAY; strokeWidth = 1f; style = android.graphics.Paint.Style.STROKE }
            val borderPaint = android.graphics.Paint().apply { color = primaryColor; strokeWidth = 1.5f; style = android.graphics.Paint.Style.STROKE }
            val bgPaint = android.graphics.Paint().apply { color = primaryColor; style = android.graphics.Paint.Style.FILL }
            val fillSecondaryPaint = android.graphics.Paint().apply { color = secondaryColor; style = android.graphics.Paint.Style.FILL }

            var pageNumber = 1
            var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            fun drawHeader() {
                canvas.drawRoundRect(android.graphics.RectF(30f, 30f, pageWidth - 30f, 110f), 20f, 20f, bgPaint)
                canvas.drawText(data.storeName, pageWidth / 2, 75f, titlePaint)
                canvas.drawText("التاريخ: ${data.date}", pageWidth - 40f, 140f, normalTextPaint)
                canvas.drawText("مطلوب من العميل:", pageWidth - 40f, 165f, normalTextPaint)
                canvas.drawText(data.customerName, pageWidth - 160f, 165f, normalTextPaint.apply { isFakeBoldText = true })
                normalTextPaint.isFakeBoldText = false
                canvas.drawLine(30f, 180f, pageWidth - 30f, 180f, borderPaint)
                canvas.drawRect(30f, 190f, pageWidth - 30f, 220f, bgPaint)
                canvas.drawText("التفاصيل", pageWidth - 50f, 210f, headerTextPaint)
                canvas.drawText("العدد", 220f, 210f, headerTextPaint.apply { textAlign = android.graphics.Paint.Align.CENTER })
                canvas.drawText("سعر الوحدة", 140f, 210f, headerTextPaint)
                canvas.drawText("الإجمالي", 60f, 210f, headerTextPaint)
                headerTextPaint.textAlign = android.graphics.Paint.Align.RIGHT
                canvas.drawRect(30f, 190f, pageWidth - 30f, pageHeight - 120f, borderPaint)
            }

            drawHeader()
            var yPosition = 245f

            for (item in data.items) {
                if (yPosition > pageHeight - 150f) {
                    document.finishPage(page); pageNumber++; pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), pageNumber).create(); page = document.startPage(pageInfo); canvas = page.canvas; drawHeader(); yPosition = 245f
                }
                if (data.items.indexOf(item) % 2 != 0) { canvas.drawRect(31f, yPosition - 15f, pageWidth - 31f, yPosition + 10f, fillSecondaryPaint) }
                val pName = item.product.name.take(45)
                canvas.drawText(pName, pageWidth - 40f, yPosition, normalTextPaint)
                canvas.drawText(item.quantity.formatClean(), 220f, yPosition, normalTextCenterPaint)
                canvas.drawText(item.product.sellingPrice.toDzd(), 140f, yPosition, normalTextCenterPaint)
                canvas.drawText(item.getTotalPrice().toDzd(), 60f, yPosition, normalTextCenterPaint)
                canvas.drawLine(30f, yPosition + 10f, pageWidth - 30f, yPosition + 10f, linePaint)
                yPosition += 25f
            }

            val footerY = pageHeight - 120f
            if (data.discountAmt > 0) { canvas.drawText("الخصم: -${data.discountAmt.toDzd()}", 270f, footerY + 25f, normalTextPaint.apply { color = android.graphics.Color.RED }); normalTextPaint.color = textColor }
            canvas.drawRect(30f, footerY, 180f, footerY + 30f, bgPaint)
            canvas.drawText("الإجمالي الكلي", 170f, footerY + 20f, headerTextPaint)
            canvas.drawText(data.totalAmt.toDzd(), 60f, footerY + 20f, headerTextPaint.apply { textAlign = android.graphics.Paint.Align.CENTER })
            val sigY = pageHeight - 40f
            canvas.drawText("المبيعات: ........................", pageWidth - 40f, sigY, normalTextPaint)
            canvas.drawText("التوقيع: ........................", pageWidth / 2 + 50f, sigY, normalTextPaint)
            document.finishPage(page)
            context.contentResolver.openOutputStream(uri)?.use { document.writeTo(it) }
            document.close()
            Toast.makeText(context, "تم حفظ الفاتورة بنجاح! 📄", Toast.LENGTH_LONG).show()

            val openIntent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/pdf"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(Intent.createChooser(openIntent, "عرض الفاتورة باستخدام..."))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "فشل إنشاء الفاتورة!", Toast.LENGTH_SHORT).show()
        }
    }

    fun generateReportPdf(context: Context, uri: Uri, title: String, content: String) {
        try {
            val document = android.graphics.pdf.PdfDocument()
            val paint = android.graphics.Paint().apply { textSize = 14f; color = android.graphics.Color.BLACK; textAlign = android.graphics.Paint.Align.RIGHT }
            val titlePaint = android.graphics.Paint().apply { textSize = 22f; color = android.graphics.Color.parseColor("#283593"); textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true }

            var pageNumber = 1
            var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            canvas.drawText(title, 595f / 2f, 60f, titlePaint)
            canvas.drawLine(40f, 80f, 595f - 40f, 80f, paint)

            var yPosition = 120f
            val lines = content.split("\n")
            for (line in lines) {
                if (yPosition > 842f - 60f) { document.finishPage(page); pageNumber++; pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNumber).create(); page = document.startPage(pageInfo); canvas = page.canvas; yPosition = 60f }
                canvas.drawText(line, 595f - 40f, yPosition, paint)
                yPosition += 25f
            }

            document.finishPage(page)
            context.contentResolver.openOutputStream(uri)?.use { document.writeTo(it) }
            document.close()
            Toast.makeText(context, "تم حفظ التقرير بنجاح! 📄", Toast.LENGTH_LONG).show()

            val openIntent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/pdf"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(Intent.createChooser(openIntent, "فتح ملف PDF باستخدام..."))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "فشل إنشاء الـ PDF!", Toast.LENGTH_SHORT).show()
        }
    }
}
