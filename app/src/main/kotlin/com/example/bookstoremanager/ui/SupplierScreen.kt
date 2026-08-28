package com.example.bookstoremanager.ui

import android.content.Context
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bookstoremanager.AppTheme
import com.example.bookstoremanager.data.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// ==========================================
// 📦 نماذج ودوال تخزين بيانات الموردين محلياً
// ==========================================
data class SupplierInvoiceData(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val isPayment: Boolean
)

data class SupplierData(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val records: MutableList<SupplierInvoiceData> = mutableListOf()
) {
    fun getNetDebt(): Double {
        return records.sumOf { if (it.isPayment) -it.amount else it.amount }
    }
}

fun loadSuppliersData(context: Context): List<SupplierData> {
    val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)
    val jsonStr = prefs.getString("suppliers_data", "[]") ?: "[]"
    val list = mutableListOf<SupplierData>()
    try {
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val supplier = SupplierData(
                id = obj.optString("id", UUID.randomUUID().toString()),
                name = obj.optString("name", ""),
                phone = obj.optString("phone", "")
            )
            val recordsArr = obj.optJSONArray("records") ?: JSONArray()
            for (j in 0 until recordsArr.length()) {
                val recObj = recordsArr.getJSONObject(j)
                supplier.records.add(
                    SupplierInvoiceData(
                        id = recObj.optString("id", UUID.randomUUID().toString()),
                        title = recObj.optString("title", ""),
                        amount = recObj.optDouble("amount", 0.0),
                        isPayment = recObj.optBoolean("isPayment", false)
                    )
                )
            }
            list.add(supplier)
        }
    } catch (e: Exception) { e.printStackTrace() }
    return list
}

fun saveSuppliersData(context: Context, suppliers: List<SupplierData>) {
    val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)
    val array = JSONArray()
    suppliers.forEach { s ->
        val obj = JSONObject()
        obj.put("id", s.id)
        obj.put("name", s.name)
        obj.put("phone", s.phone)
        obj.put("totalDebt", s.getNetDebt())
        val recordsArr = JSONArray()
        s.records.forEach { r ->
            val rObj = JSONObject()
            rObj.put("id", r.id)
            rObj.put("title", r.title)
            rObj.put("amount", r.amount)
            rObj.put("isPayment", r.isPayment)
            recordsArr.put(rObj)
        }
        obj.put("records", recordsArr)
        array.put(obj)
    }
    prefs.edit().putString("suppliers_data", array.toString()).apply()
}

// ==========================================
// 🚚 شاشة الموردين الرئيسية
// ==========================================
@Composable
fun SuppliersTab(suppliersManager: SuppliersManager, isDark: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    var suppliers by remember { mutableStateOf(loadSuppliersData(context)) }

    var searchQuery by remember { mutableStateOf("") }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedSupplierForRecords by remember { mutableStateOf<SupplierData?>(null) }
    var supplierToEdit by remember { mutableStateOf<SupplierData?>(null) }

    val totalSuppliers = suppliers.size
    val totalDebtToPay = suppliers.sumOf { it.getNetDebt() }

    val filteredSuppliers = suppliers.filter { it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery) }

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.bg(isDark)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "عودة", tint = AppTheme.text(isDark)) }
            Text("إدارة الموردين 🚚", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF0EA5E9)).clickable { showAddDialog = true },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(32.dp)) }

            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                placeholder = { Text("البحث عن مورد/شركة...", color = AppTheme.subText(isDark)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = AppTheme.subText(isDark)) },
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = AppTheme.card(isDark), unfocusedContainerColor = AppTheme.card(isDark), focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedTextColor = AppTheme.text(isDark), unfocusedTextColor = AppTheme.text(isDark))
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SupplierSummaryCard("الشركات والموردين", "$totalSuppliers", Icons.Default.LocalShipping, Color(0xFF0EA5E9), Color(0xFFF0F9FF), isDark, Modifier.weight(1f))
            SupplierSummaryCard("إجمالي المستحقات علينا", "${totalDebtToPay} د.ج", Icons.Default.AccountBalanceWallet, Color(0xFFF97316), Color(0xFFFFF7ED), isDark, Modifier.weight(1f))
        }

        Spacer(Modifier.height(8.dp))

        if (filteredSuppliers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) { Text("لا يوجد موردين مسجلين", color = AppTheme.subText(isDark)) }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredSuppliers) { supplier ->
                    SupplierCardItem(
                        supplier = supplier, isDark = isDark,
                        onOpenRecords = { selectedSupplierForRecords = supplier },
                        onEdit = { supplierToEdit = supplier },
                        onDelete = {
                            suppliers = suppliers.filter { it.id != supplier.id }
                            saveSuppliersData(context, suppliers)
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditSupplierDialog(null, isDark, onDismiss = { showAddDialog = false }) { newSupp ->
            suppliers = suppliers + newSupp
            saveSuppliersData(context, suppliers); showAddDialog = false
        }
    }

    if (supplierToEdit != null) {
        AddEditSupplierDialog(supplierToEdit, isDark, onDismiss = { supplierToEdit = null }) { updated ->
            suppliers = suppliers.map { if (it.id == updated.id) updated else it }
            saveSuppliersData(context, suppliers); supplierToEdit = null
        }
    }

    if (selectedSupplierForRecords != null) {
        SupplierLedgerDialog(
            supplier = selectedSupplierForRecords!!, isDark = isDark,
            onDismiss = { selectedSupplierForRecords = null },
            onUpdate = { updatedSupp ->
                suppliers = suppliers.map { if (it.id == updatedSupp.id) updatedSupp else it }
                saveSuppliersData(context, suppliers)
            }
        )
    }
}

// ==========================================
// 🎨 مكونات الواجهة للموردين
// ==========================================
@Composable
fun SupplierSummaryCard(title: String, value: String, icon: ImageVector, color: Color, lightBg: Color, isDark: Boolean, modifier: Modifier) {
    val bgColor = if (isDark) color.copy(alpha = 0.15f) else lightBg
    Card(colors = CardDefaults.cardColors(containerColor = bgColor), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, color.copy(alpha = 0.3f)), modifier = modifier.height(80.dp)) {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.Center) {
                Text(value, fontWeight = FontWeight.Black, fontSize = 16.sp, color = color)
                Text(title, fontSize = 11.sp, color = color.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
            }
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun SupplierCardItem(supplier: SupplierData, isDark: Boolean, onOpenRecords: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val initials = if (supplier.name.length >= 2) supplier.name.take(2).uppercase() else supplier.name.uppercase()
    val debt = supplier.getNetDebt()

    Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, AppTheme.cardBorder(isDark)), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFF0EA5E9), shape = CircleShape, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(supplier.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppTheme.text(isDark))
                    Text("الهاتف: ${supplier.phone.ifBlank { "غير مسجل" }}", fontSize = 12.sp, color = AppTheme.subText(isDark))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("المستحقات: ${debt} د.ج", fontWeight = FontWeight.Black, fontSize = 15.sp, color = if (debt > 0) Color(0xFFF97316) else AppTheme.text(isDark))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SupplierActionIcon(Icons.Default.ReceiptLong, Color(0xFF0EA5E9)) { onOpenRecords() }
                    SupplierActionIcon(Icons.Default.Edit, Color(0xFF3B82F6)) { onEdit() }
                    SupplierActionIcon(Icons.Default.Delete, Color(0xFFEF4444)) { onDelete() }
                }
            }
        }
    }
}

@Composable
private fun SupplierActionIcon(icon: ImageVector, color: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.15f)).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
    }
}

// ==========================================
// 📝 نوافذ إضافة مورد وسجل الفواتير
// ==========================================
@Composable
fun AddEditSupplierDialog(supplier: SupplierData?, isDark: Boolean, onDismiss: () -> Unit, onSave: (SupplierData) -> Unit) {
    var name by remember { mutableStateOf(supplier?.name ?: "") }
    var phone by remember { mutableStateOf(supplier?.phone ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = AppTheme.card(isDark), shape = RoundedCornerShape(20.dp),
        title = { Text(if (supplier == null) "إضافة شركة/مورد جديد" else "تعديل بيانات المورد", fontWeight = FontWeight.Bold, color = AppTheme.text(isDark)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الشركة أو المورد") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onSave(SupplierData(id = supplier?.id ?: UUID.randomUUID().toString(), name = name, phone = phone, records = supplier?.records ?: mutableListOf())) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))) { Text("حفظ", color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = AppTheme.subText(isDark)) } }
    )
}

@Composable
fun SupplierLedgerDialog(supplier: SupplierData, isDark: Boolean, onDismiss: () -> Unit, onUpdate: (SupplierData) -> Unit) {
    var showAddRecordDialog by remember { mutableStateOf(false) }
    var isPaymentType by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = AppTheme.bg(isDark), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxHeight(0.9f).fillMaxWidth(0.95f),
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("حساب المورد: ${supplier.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppTheme.text(isDark))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = AppTheme.subText(isDark)) }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), border = BorderStroke(1.dp, AppTheme.cardBorder(isDark)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("الديون المتراكمة (يجب دفعها)", fontSize = 12.sp, color = AppTheme.subText(isDark))
                        Text("${supplier.getNetDebt()} د.ج", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFFF97316))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { isPaymentType = false; showAddRecordDialog = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316))) { Text("📦 فاتورة شراء", color = Color.White, fontSize = 12.sp) }
                    Button(onClick = { isPaymentType = true; showAddRecordDialog = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))) { Text("💸 تسديد دفعة", color = Color.White, fontSize = 12.sp) }
                }
                Spacer(Modifier.height(16.dp))
                Text("السجل وحركة الحساب:", fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(supplier.records.reversed()) { record ->
                        Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(record.title.ifBlank { if (record.isPayment) "تسديد نقدي" else "سلع واردة" }, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark), fontSize = 13.sp)
                                    Text(if (record.isPayment) "تم الدفع للمورد" else "شراء بالدين", fontSize = 11.sp, color = if (record.isPayment) Color(0xFF10B981) else Color(0xFFF97316))
                                }
                                Text("${record.amount} د.ج", fontWeight = FontWeight.Bold, color = if (record.isPayment) Color(0xFF10B981) else Color(0xFFF97316))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )

    if (showAddRecordDialog) {
        var amountInput by remember { mutableStateOf("") }
        var titleInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddRecordDialog = false }, containerColor = AppTheme.card(isDark),
            title = { Text(if (isPaymentType) "تسديد دفعة للمورد" else "إدخال فاتورة شراء", fontWeight = FontWeight.Bold, color = AppTheme.text(isDark)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = amountInput, onValueChange = { amountInput = it }, label = { Text("المبلغ (دج)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = titleInput, onValueChange = { titleInput = it }, label = { Text(if (isPaymentType) "طريقة الدفع (مثال: نقداً / شيك)" else "رقم الفاتورة أو البيان") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountInput.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            supplier.records.add(SupplierInvoiceData(title = titleInput, amount = amount, isPayment = isPaymentType))
                            onUpdate(supplier); showAddRecordDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isPaymentType) Color(0xFF10B981) else Color(0xFFF97316))
                ) { Text("تأكيد العملية", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showAddRecordDialog = false }) { Text("إلغاء", color = AppTheme.subText(isDark)) } }
        )
    }
}
