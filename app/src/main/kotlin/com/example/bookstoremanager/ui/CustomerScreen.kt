package com.example.bookstoremanager.ui

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
import com.example.bookstoremanager.*
import com.example.bookstoremanager.data.*
import java.util.UUID

// ==========================================
// 👥 شاشة العملاء والديون
// ==========================================
@Composable
fun CustomersTab(
    inventoryManager: InventoryManager,
    refreshTrigger: Int,
    isDark: Boolean,
    onDataChanged: () -> Unit
) {
    val context = LocalContext.current

    // القراءة المباشرة من المحرك المركزي وتحويلها لقائمة لتحديث الواجهة فوراً
    val customers = remember(refreshTrigger, inventoryManager.getCustomerList().size) {
        inventoryManager.getCustomerList().toList()
    }

    var searchQuery by remember { mutableStateOf("") }
    var filterWithDebt by remember { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCustomerForRecords by remember { mutableStateOf<CustomerAccount?>(null) }
    var customerToEdit by remember { mutableStateOf<CustomerAccount?>(null) }

    val totalCustomers = customers.size
    val totalDebts = customers.sumOf { it.getTotalReceivable() }

    val filteredCustomers = customers.filter {
        (it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)) &&
        (!filterWithDebt || it.getTotalReceivable() > 0)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(AppTheme.GoldPrimary).clickable { showAddDialog = true },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(32.dp)) }

            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                placeholder = { Text("البحث عن عميل...", color = AppTheme.subText(isDark)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = AppTheme.subText(isDark)) },
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AppTheme.card(isDark),
                    unfocusedContainerColor = AppTheme.card(isDark),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = AppTheme.text(isDark),
                    unfocusedTextColor = AppTheme.text(isDark)
                )
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterToggleButton("الكل", !filterWithDebt, isDark, Modifier.weight(1f)) { filterWithDebt = false }
            FilterToggleButton("مع ديون", filterWithDebt, isDark, Modifier.weight(1f)) { filterWithDebt = true }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("إجمالي العملاء", "$totalCustomers", Icons.Default.Group, Color(0xFF3B82F6), Color(0xFFEFF6FF), isDark, Modifier.weight(1f))
            SummaryCard("مجموع الديون", totalDebts.toDzd(), Icons.Default.AttachMoney, Color(0xFFEF4444), Color(0xFFFEF2F2), isDark, Modifier.weight(1f))
        }

        Spacer(Modifier.height(8.dp))

        if (filteredCustomers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) { Text("لا يوجد عملاء", color = AppTheme.subText(isDark)) }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                items(filteredCustomers, key = { it.id }) { customer ->
                    CustomerCardItem(
                        customer = customer, isDark = isDark,
                        onOpenRecords = { selectedCustomerForRecords = customer },
                        onEdit = { customerToEdit = customer },
                        onDelete = {
                            inventoryManager.removeCustomer(customer.id)
                            onDataChanged()
                            Toast.makeText(context, "تم حذف العميل", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditCustomerDialog(null, isDark, onDismiss = { showAddDialog = false }) { newCust ->
            inventoryManager.addCustomer(newCust)
            onDataChanged()
            showAddDialog = false
            Toast.makeText(context, "تمت إضافة العميل ومزامنته", Toast.LENGTH_SHORT).show()
        }
    }

    if (customerToEdit != null) {
        AddEditCustomerDialog(customerToEdit, isDark, onDismiss = { customerToEdit = null }) { updated ->
            val index = inventoryManager.getCustomerList().indexOfFirst { it.id == updated.id }
            if (index >= 0) {
                inventoryManager.getCustomerList()[index] = updated
                inventoryManager.autoSyncToCloud()
                onDataChanged()
            }
            customerToEdit = null
        }
    }

    if (selectedCustomerForRecords != null) {
        CustomerCarnetDialog(
            customer = selectedCustomerForRecords!!,
            isDark = isDark,
            inventoryManager = inventoryManager, // تمرير المتغير
            onDismiss = { selectedCustomerForRecords = null },
            onUpdate = { onDataChanged() }
        )
    }
}

// ==========================================
// 🎨 مكونات الواجهة
// ==========================================
@Composable
fun FilterToggleButton(text: String, isSelected: Boolean, isDark: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val borderColor = if (isSelected) AppTheme.GoldPrimary else AppTheme.cardBorder(isDark)
    val textColor = if (isSelected) AppTheme.text(isDark) else AppTheme.subText(isDark)
    Surface(
        shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, borderColor), color = AppTheme.card(isDark),
        modifier = modifier.height(48.dp).clickable { onClick() }
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(text, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.width(8.dp))
            Icon(if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked, null, tint = borderColor, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SummaryCard(title: String, value: String, icon: ImageVector, color: Color, lightBg: Color, isDark: Boolean, modifier: Modifier) {
    val bgColor = if (isDark) color.copy(alpha = 0.15f) else lightBg
    Card(colors = CardDefaults.cardColors(containerColor = bgColor), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, color.copy(alpha = 0.3f)), modifier = modifier.height(80.dp)) {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.Center) {
                Text(value, fontWeight = FontWeight.Black, fontSize = 18.sp, color = color)
                Text(title, fontSize = 12.sp, color = color.copy(alpha = 0.8f))
            }
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun CustomerCardItem(customer: CustomerAccount, isDark: Boolean, onOpenRecords: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val initials = if (customer.name.length >= 2) customer.name.take(2).uppercase() else customer.name.uppercase()
    val balance = customer.getTotalReceivable()

    Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, AppTheme.cardBorder(isDark)), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFFEF4444), shape = CircleShape, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppTheme.text(isDark))
                    Text("الهاتف: ${customer.phone.ifBlank { "لا يوجد" }}", fontSize = 12.sp, color = AppTheme.subText(isDark))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("الديون المستحقة: ${balance.toDzd()}", fontWeight = FontWeight.Black, fontSize = 15.sp, color = if (balance > 0) Color(0xFFEF4444) else Color(0xFF10B981))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CustomerActionIcon(Icons.Default.TrendingUp, Color(0xFF10B981)) { onOpenRecords() }
                    CustomerActionIcon(Icons.Default.Edit, Color(0xFF3B82F6)) { onEdit() }
                    CustomerActionIcon(Icons.Default.Delete, Color(0xFFEF4444)) { onDelete() }
                }
            }
        }
    }
}

@Composable
private fun CustomerActionIcon(icon: ImageVector, color: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.15f)).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
    }
}

// ==========================================
// 📝 نوافذ الإضافة والسجل
// ==========================================
@Composable
fun AddEditCustomerDialog(customer: CustomerAccount?, isDark: Boolean, onDismiss: () -> Unit, onSave: (CustomerAccount) -> Unit) {
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var lastName by remember { mutableStateOf(customer?.lastName ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = AppTheme.card(isDark), shape = RoundedCornerShape(20.dp),
        title = { Text(if (customer == null) "إضافة عميل جديد" else "تعديل بيانات العميل", fontWeight = FontWeight.Bold, color = AppTheme.text(isDark)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("اللقب (اختياري)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val cust = CustomerAccount(
                            id = customer?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            lastName = lastName.trim(),
                            phone = phone.trim(),
                            socialMedia = customer?.socialMedia ?: ""
                        )
                        customer?.records?.let { cust.records.addAll(it) }
                        onSave(cust)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.GoldPrimary)
            ) { Text("حفظ", color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = AppTheme.subText(isDark)) } }
    )
}

// 🌟 هنا تم التعديل لتستقبل inventoryManager بشكل صحيح
@Composable
fun CustomerCarnetDialog(
    customer: CustomerAccount,
    isDark: Boolean,
    inventoryManager: InventoryManager,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    var showAddRecordDialog by remember { mutableStateOf(false) }
    var isDebtType by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.bg(isDark),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxHeight(0.9f).fillMaxWidth(0.95f),
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("سجل الديون: ${customer.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppTheme.text(isDark))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = AppTheme.subText(isDark)) }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), border = BorderStroke(1.dp, AppTheme.cardBorder(isDark)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("الرصيد المتبقي (الكريدي)", fontSize = 12.sp, color = AppTheme.subText(isDark))
                        Text(customer.getTotalReceivable().toDzd(), fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { isDebtType = true; showAddRecordDialog = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) { Text("➕ إضافة دين", color = Color.White) }
                    Button(onClick = { isDebtType = false; showAddRecordDialog = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))) { Text("💸 تسديد", color = Color.White) }
                }
                Spacer(Modifier.height(16.dp))
                Text("العمليات السابقة:", fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(customer.records.toList()) { record ->
                        Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(record.title.ifBlank { if (record.isReceivable) "شراء سلع (دين)" else "دفعة تسديد" }, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark), fontSize = 13.sp)
                                    Text(if (record.isReceivable) "إضافة للدين" else "تسديد", fontSize = 11.sp, color = if (record.isReceivable) Color(0xFFEF4444) else Color(0xFF10B981))
                                }
                                Text(record.amount.toDzd(), fontWeight = FontWeight.Bold, color = if (record.isReceivable) Color(0xFFEF4444) else Color(0xFF10B981))
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
            title = { Text(if (isDebtType) "إضافة دين جديد" else "تسديد دفعة", fontWeight = FontWeight.Bold, color = AppTheme.text(isDark)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = amountInput, onValueChange = { amountInput = it }, label = { Text("المبلغ (دج)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = titleInput, onValueChange = { titleInput = it }, label = { Text("البيان (اختياري)") }, placeholder = { Text(if (isDebtType) "مثال: شراء مواد غذائية" else "مثال: تسديد نقداً") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountInput.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            val record = DebtRecord(
                                id = UUID.randomUUID().toString(),
                                title = titleInput.trim(),
                                amount = amount,
                                isReceivable = isDebtType
                            )
                            inventoryManager.addDebtRecordToCustomer(customer.id, record)
                            onUpdate()
                            showAddRecordDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDebtType) Color(0xFFEF4444) else Color(0xFF10B981))
                ) { Text("تأكيد", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showAddRecordDialog = false }) { Text("إلغاء", color = AppTheme.subText(isDark)) } }
        )
    }
}
