package com.example.bookstoremanager

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.bookstoremanager.data.*

object PdfInvoiceGenerator {
    fun generateAndSavePdf(
        context: Context,
        cartItems: List<CartItem>,
        totalAmount: Double,
        discountAmount: Double = 0.0,
        customerInfo: String = ""
    ) {
        // سحب إعدادات الشركة والتذكرة التي سجلناها سابقاً
        val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)
        val companyManager = CompanyInfoManager(context)
        val companyInfo = companyManager.getCompanyInfo()

        val welcomeMsg = prefs.getString("ticket_welcome", "مرحباً بكم في متجرنا") ?: ""
        val footerMsg = prefs.getString("ticket_footer", "شكراً لزيارتكم") ?: ""

        // تجهيز صفحة الـ PDF
        val pdfDocument = PdfDocument()
        val pageHeight = 600 + (cartItems.size * 35) // طول الصفحة يتمدد حسب عدد المنتجات
        val pageInfo = PdfDocument.PageInfo.Builder(400, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        var yPosition = 50f

        // 🏢 طباعة معلومات المؤسسة في أعلى الفاتورة
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 22f
        canvas.drawText(companyInfo.name.ifBlank { "فاتورة مبيعات" }, 200f, yPosition, paint)

        yPosition += 25f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 12f
        if (companyInfo.activity.isNotBlank()) { canvas.drawText(companyInfo.activity, 200f, yPosition, paint); yPosition += 15f }
        if (companyInfo.phoneFax.isNotBlank()) { canvas.drawText("الهاتف: ${companyInfo.phoneFax}", 200f, yPosition, paint); yPosition += 15f }
        if (companyInfo.rcNumber.isNotBlank()) { canvas.drawText("RC: ${companyInfo.rcNumber} | NIF: ${companyInfo.nifNumber}", 200f, yPosition, paint); yPosition += 15f }
        if (companyInfo.address.isNotBlank()) { canvas.drawText(companyInfo.address, 200f, yPosition, paint); yPosition += 15f }

        // رسالة الترحيب
        yPosition += 10f
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(welcomeMsg, 200f, yPosition, paint)
        yPosition += 30f

        // خط فاصل
        paint.strokeWidth = 1f
        canvas.drawLine(20f, yPosition, 380f, yPosition, paint)
        yPosition += 25f

        // 📅 تفاصيل الفاتورة والزبون
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("التاريخ والوقت: $dateStr", 380f, yPosition, paint)

        if (customerInfo.isNotBlank()) {
            yPosition += 18f
            canvas.drawText("الزبون: $customerInfo", 380f, yPosition, paint)
        }

        yPosition += 20f
        canvas.drawLine(20f, yPosition, 380f, yPosition, paint)
        yPosition += 25f

        // 🛒 رأس جدول المشتريات
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("المنتج", 380f, yPosition, paint)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("الكمية", 180f, yPosition, paint)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("المجموع", 20f, yPosition, paint)

        yPosition += 15f
        canvas.drawLine(20f, yPosition, 380f, yPosition, paint)
        yPosition += 25f

        // 📦 سرد المنتجات
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        cartItems.forEach { item ->
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(item.product.name, 380f, yPosition, paint)

            paint.textAlign = Paint.Align.CENTER
            val qtyStr = "${item.quantity.toString().replace(".0", "")} ${item.product.unitType}"
            canvas.drawText(qtyStr, 180f, yPosition, paint)

            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("${item.getTotalPrice()} د.ج", 20f, yPosition, paint)
            yPosition += 25f
        }

        canvas.drawLine(20f, yPosition, 380f, yPosition, paint)
        yPosition += 35f

        // 💰 الإجمالي والخصم
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        if (discountAmount > 0) {
            paint.color = Color.RED
            canvas.drawText("قيمة الخصم: -$discountAmount د.ج", 20f, yPosition, paint)
            yPosition += 25f
        }
        paint.color = Color.BLACK
        paint.textSize = 18f
        canvas.drawText("الإجمالي الصافي: $totalAmount د.ج", 20f, yPosition, paint)

        // تذييل التذكرة
        yPosition += 50f
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(footerMsg, 200f, yPosition, paint)

        pdfDocument.finishPage(page)

        // 💾 حفظ الملف في مجلد التنزيلات (Downloads) ليسهل على المستخدم إيجاده
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val appFolder = File(downloadsDir, "Kasebo_Invoices")
            if (!appFolder.exists()) appFolder.mkdirs()

            val fileName = "Invoice_${System.currentTimeMillis()}.pdf"
            val file = File(appFolder, fileName)
            pdfDocument.writeTo(FileOutputStream(file))

            Toast.makeText(context, "✅ تم الحفظ في التنزيلات: Kasebo_Invoices", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "❌ فشل حفظ الفاتورة!", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }
}
