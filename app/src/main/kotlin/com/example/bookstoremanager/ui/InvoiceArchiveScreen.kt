package com.example.bookstoremanager.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bookstoremanager.BluetoothPrinterManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Locale

// 🌟 دوال مساعدة محلية لتجنب أخطاء الاستيراد 🌟
private fun Double.formatClean(): String {
    return if (this % 1.0 == 0.0) this.toLong().toString() else String.format(Locale.US, "%.2f", this)
}

private fun Double.toDzd(): String {
    return "${this.formatClean()} دج"
}

// 🌟 كلاس محلي للبيانات 🌟
data class ArchiveTransaction(
    val date: String,
    val partyName: String,
    val quantity: Double,
    val unitPrice: Double,
    val total: Double
)

// 🌟 دالة لتنسيق شكل الفاتورة للطباعة 🌟
private fun generateReceiptText(date: String, customerName: String, items: List<ArchiveTransaction>, total: Double): String {
    val sb = StringBuilder()
    sb.append("==============================\n")
    sb.append("         فاتورة مبيعات        \n")
    sb.append("==============================\n")
    sb.append("التاريخ: $date\n")
    sb.append("العميل: $customerName\n")
    sb.append("------------------------------\n")
    sb.append("الكمية   السعر      المجموع\n")
    sb.append("------------------------------\n")

    items.forEach { item ->
        val qty = item.quantity.formatClean()
        val price = item.unitPrice.formatClean()
        val itemTotal = item.total.formatClean()
        sb.append("عنصر x$qty  | $price | $itemTotal\n")
    }

    sb.append("------------------------------\n")
    sb.append("الاجمالي: ${total.toDzd()}\n")
    sb.append("==============================\n")
    sb.append("        شكرا لزيارتكم!        \n")
    sb.append("\n\n\n") // مسافة لقطع الورقة
    return sb.toString()
}

@Composable
fun InvoiceArchiveScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)
    val coroutineScope = rememberCoroutineScope()

    var showPrintDialog by remember { mutableStateOf(false) }
    var selectedInvoiceForPrint by remember { mutableStateOf<Pair<String, List<ArchiveTransaction>>?>(null) }
    var printerName by remember { mutableStateOf(prefs.getString("printer_name", "MTP-II") ?: "MTP-II") }
    var isPrinting by remember { mutableStateOf(false) }

    var invoices by remember { mutableStateOf<Map<String, List<ArchiveTransaction>>>(emptyMap()) }

    // 🌟 جلب البيانات فور الدخول للشاشة
    LaunchedEffect(Unit) {
        val jsonStr = prefs.getString("transactions_data", "[]") ?: "[]"
        val list = mutableListOf<ArchiveTransaction>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optBoolean("isSale", true)) {
                    list.add(
                        ArchiveTransaction(
                            date = obj.optString("date", "غير محدد"),
                            partyName = obj.optString("partyName", "زبون نقدي"),
                            quantity = obj.optDouble("quantity", 0.0),
                            unitPrice = obj.optDouble("unitPrice", 0.0),
                            total = obj.optDouble("total", 0.0)
                        )
                    )
                }
            }
            // تجميع المنتجات في فاتورة واحدة بناءً على التاريخ
            invoices = list.groupBy { it.date }.toSortedMap(compareByDescending {
                    try { SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).parse(it)?.time ?: 0L } catch (e: Exception) { 0L }
            })
        } catch (e: Exception) { e.printStackTrace() }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp).statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "عودة", tint = Color(0xFFF59E0B)) }
            Text("أرشيف الفواتير", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.size(48.dp))
        }

        if (invoices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("أرشيف الفواتير فارغ", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(invoices.entries.toList()) { (date, items) ->
                    val invoiceTotal = items.sumOf { it.total }
                    val customerName = items.firstOrNull()?.partyName ?: "زبون نقدي"

                    Card(
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF0F9FF)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFF3B82F6))
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(customerName, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 16.sp)
                                        Text(date, color = Color.Gray, fontSize = 12.sp)
                                    }
                                }

                                // 🖨️ زر الطباعة
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(invoiceTotal.toDzd(), fontWeight = FontWeight.Black, color = Color(0xFF10B981), fontSize = 16.sp)
                                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF97316)).clickable {
                                            selectedInvoiceForPrint = Pair(date, items)
                                            showPrintDialog = true
                                        }, contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Print, contentDescription = "طباعة", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
                            Text("عناصر الفاتورة (${items.size}):", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))

                            items.forEach { item ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("• منتج (الكمية: ${item.quantity.formatClean()})", color = Color(0xFF64748B), fontSize = 12.sp)
                                    Text(item.total.toDzd(), color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 🖨️ نافذة إعداد الطباعة
        if (showPrintDialog && selectedInvoiceForPrint != null) {
            val (invoiceDate, invoiceItems) = selectedInvoiceForPrint!!
            val customerName = invoiceItems.firstOrNull()?.partyName ?: "زبون نقدي"
            val total = invoiceItems.sumOf { it.total }

            AlertDialog(
                onDismissRequest = { if (!isPrinting) showPrintDialog = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                title = { Text("طباعة الفاتورة", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("يرجى التأكد من تشغيل طابعة البلوتوث واقترانها بالهاتف.", fontSize = 12.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = printerName,
                            onValueChange = { printerName = it },
                            label = { Text("اسم الطابعة (مثال: MTP-II)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isPrinting = true
                            prefs.edit().putString("printer_name", printerName).apply()

                            coroutineScope.launch(Dispatchers.IO) {
                                val printerManager = BluetoothPrinterManager()
                                val isConnected = printerManager.connectToPrinter(printerName)

                                withContext(Dispatchers.Main) {
                                    if (isConnected) {
                                        val receiptText = generateReceiptText(invoiceDate, customerName, invoiceItems, total)

                                        coroutineScope.launch(Dispatchers.IO) {
                                            printerManager.printText(receiptText)
                                            printerManager.disconnect()
                                            withContext(Dispatchers.Main) {
                                                isPrinting = false
                                                showPrintDialog = false
                                                Toast.makeText(context, "تمت الطباعة بنجاح! 🧾", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        isPrinting = false
                                        Toast.makeText(context, "فشل الاتصال بالطابعة! تأكد من الاسم وتشغيل البلوتوث.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                        enabled = !isPrinting
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("طباعة الآن 🖨️", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPrintDialog = false }, enabled = !isPrinting) {
                        Text("إلغاء", color = Color.Gray)
                    }
                }
            )
        }
    }
}
