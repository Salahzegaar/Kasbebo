package com.example.bookstoremanager.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import com.example.bookstoremanager.*
import com.example.bookstoremanager.data.*
import org.json.JSONArray

// ==========================================
// 💾 محرك بيانات حركات المنتجات (حقيقي)
// ==========================================
data class ProductTransaction(
    val date: String,
    val partyName: String,
    val quantity: Double,
    val unitPrice: Double,
    val total: Double,
    val productId: String = "",
    val isSale: Boolean = true
)

fun getProductTransactions(context: Context, productId: String, isSale: Boolean): List<ProductTransaction> {
    val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)
    val jsonStr = prefs.getString("transactions_data", "[]") ?: "[]"
    val list = mutableListOf<ProductTransaction>()
    try {
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            if (obj.optString("productId") == productId && obj.optBoolean("isSale") == isSale) {
                list.add(
                    ProductTransaction(
                        date = obj.optString("date", "غير محدد"),
                        partyName = obj.optString("partyName", ""),
                        quantity = obj.optDouble("quantity", 0.0),
                        unitPrice = obj.optDouble("unitPrice", 0.0),
                        total = obj.optDouble("total", 0.0),
                        productId = obj.optString("productId", ""),
                        isSale = obj.optBoolean("isSale", true)
                    )
                )
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list.reversed()
}

// ==========================================
// 1️⃣ تقرير الجرد الفعلي التفاعلي
// ==========================================
@Composable
fun AuditReportScreen(inventoryManager: InventoryManager, onBack: () -> Unit) {
    val context = LocalContext.current
    var allProducts by remember { mutableStateOf(inventoryManager.getProductList()) }
    var searchQuery by remember { mutableStateOf("") }
    var countedQuantities by remember { mutableStateOf(mapOf<String, String>()) }

    val totalProducts = allProducts.size
    val countedProductsCount = countedQuantities.count { it.value.isNotBlank() }
    var differencesCount = 0
    var quantityDifference = 0.0
    var valueDifference = 0.0

    allProducts.forEach { product ->
        val countStr = countedQuantities[product.id]
        if (!countStr.isNullOrBlank()) {
            val countedQty = countStr.toDoubleOrNull() ?: product.stockQuantity
            val diff = countedQty - product.stockQuantity
            if (diff != 0.0) {
                differencesCount++
                quantityDifference += diff
                valueDifference += (diff * product.purchasePrice)
            }
        }
    }

    val filteredProducts = allProducts.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.barcode.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF1E293B)).padding(16.dp).statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الجرد", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Icon(Icons.Default.FactCheck, null, tint = Color(0xFF38BDF8))
            }
            Spacer(modifier = Modifier.size(48.dp))
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("بحث", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("الكود، الاسم، المرجع...", fontSize = 12.sp, color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = Color(0xFF38BDF8))
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { countedQuantities = mapOf() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("مسح الادخال", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                var updated = 0
                                allProducts.forEach { product ->
                                    val countStr = countedQuantities[product.id]
                                    if (!countStr.isNullOrBlank()) {
                                        val newQty = countStr.toDoubleOrNull()
                                        if (newQty != null && newQty != product.stockQuantity) {
                                            product.stockQuantity = newQty
                                            inventoryManager.updateProduct(product)
                                            updated++
                                        }
                                    }
                                }
                                if (updated > 0) {
                                    Toast.makeText(context, "تم تأكيد وتحديث $updated منتجات!", Toast.LENGTH_SHORT).show()
                                    countedQuantities = mapOf()
                                    allProducts = inventoryManager.getProductList()
                                } else {
                                    Toast.makeText(context, "لم تقم بإدخال أي فروقات.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("تأكيد الجرد", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AuditStatRow("المنتجات", "$totalProducts")
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    AuditStatRow("منتجات محسوبة", "$countedProductsCount")
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    AuditStatRow("فروقات", "$differencesCount")
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    AuditStatRow("فرق الكمية", quantityDifference.formatClean())
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    val diffColor = if (valueDifference < 0) Color(0xFFEF4444) else if (valueDifference > 0) Color(0xFF10B981) else Color(0xFF1E293B)
                    AuditStatRow("قيمة الفرق", valueDifference.toDzd(), isBold = true, textColor = diffColor)
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E293B)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المنتج", color = Color.White, modifier = Modifier.weight(2f), fontSize = 12.sp)
                        Text("النظام", color = Color.White, modifier = Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("الفعلي", color = Color.White, modifier = Modifier.weight(1.5f), fontSize = 12.sp, textAlign = TextAlign.Center)
                    }

                    filteredProducts.forEach { product ->
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(2f)) {
                                Text(product.name, color = Color(0xFF1E293B), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(product.barcode.ifBlank { "بدون كود" }, color = Color.Gray, fontSize = 10.sp)
                            }
                            Text(product.stockQuantity.formatClean(), color = Color.Gray, modifier = Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.Center)
                            OutlinedTextField(
                                value = countedQuantities[product.id] ?: "",
                                onValueChange = { newValue -> countedQuantities = countedQuantities.toMutableMap().apply { put(product.id, newValue) } },
                                modifier = Modifier.weight(1.5f).height(50.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFF0F9FF),
                                    unfocusedContainerColor = Color(0xFFF8FAFC),
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                placeholder = { Text("-", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color.LightGray) }
                            )
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }
    }
}

@Composable
fun AuditStatRow(label: String, value: String, isBold: Boolean = false, textColor: Color = Color(0xFF64748B)) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(value, color = textColor, fontSize = if (isBold) 18.sp else 16.sp, fontWeight = if (isBold) FontWeight.Black else FontWeight.Bold)
    }
}

// ==========================================
// 2️⃣ حركة المخزون الشاملة + النوافذ المنبثقة
// ==========================================
@Composable
fun StockMovementReportScreen(inventoryManager: InventoryManager, onBack: () -> Unit) {
    val context = LocalContext.current
    val products = inventoryManager.getProductList()
    var searchQuery by remember { mutableStateOf("") }
    val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)

    var selectedProductForSales by remember { mutableStateOf<Product?>(null) }
    var selectedProductForPurchases by remember { mutableStateOf<Product?>(null) }
    var allTransactions by remember { mutableStateOf<List<ProductTransaction>>(emptyList()) }

    LaunchedEffect(Unit) {
        val jsonStr = prefs.getString("transactions_data", "[]") ?: "[]"
        val list = mutableListOf<ProductTransaction>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ProductTransaction(
                        date = obj.optString("date", ""),
                        partyName = obj.optString("partyName", ""),
                        quantity = obj.optDouble("quantity", 0.0),
                        unitPrice = obj.optDouble("unitPrice", 0.0),
                        total = obj.optDouble("total", 0.0),
                        productId = obj.optString("productId", ""),
                        isSale = obj.optBoolean("isSale", true)
                    )
                )
            }
        } catch (e: Exception) {}
        allTransactions = list
    }

    val salesTransactions = allTransactions.filter { it.isSale }
    val purchaseTransactions = allTransactions.filter { !it.isSale }

    val globalSoldQty = salesTransactions.sumOf { it.quantity }
    val globalSoldValue = salesTransactions.sumOf { it.total }
    val globalPurchasedQty = products.sumOf { it.stockQuantity } + globalSoldQty
    val globalPurchasedValue = products.sumOf { it.purchasePrice * it.stockQuantity } + purchaseTransactions.sumOf { it.total }

    val filteredProducts = products.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.barcode.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF1E293B)).padding(16.dp).statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEF4444)).clickable { Toast.makeText(context, "الطباعة قيد التحضير", Toast.LENGTH_SHORT).show() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = Color.White)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("حركة المخزون", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Icon(Icons.Default.SyncAlt, null, tint = Color(0xFF38BDF8))
            }
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White)
            }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MovementStatCard("الكمية المباعة", globalSoldQty.formatClean(), Icons.Default.Inventory, Color(0xFF10B981), Color(0xFFECFDF5), Modifier.weight(1f))
                MovementStatCard("إجمالي المشتريات", globalPurchasedValue.formatClean() + " دج", Icons.Default.AccountBalanceWallet, Color(0xFF3B82F6), Color(0xFFEFF6FF), Modifier.weight(1.2f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MovementStatCard("الكمية المشتراة", globalPurchasedQty.formatClean(), Icons.Default.ShoppingCart, Color(0xFF3B82F6), Color(0xFFEFF6FF), Modifier.weight(1f))
                MovementStatCard("إجمالي المبيعات", globalSoldValue.formatClean() + " دج", Icons.Default.TrendingUp, Color(0xFF10B981), Color(0xFFECFDF5), Modifier.weight(1f))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF8B5CF6)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.FilterList, null, tint = Color.White)
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث بالاسم / المرجع", fontSize = 12.sp, color = Color.Gray) },
                    trailingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, unfocusedBorderColor = Color(0xFFE2E8F0))
                )
            }

            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    Row(modifier = Modifier.background(Color(0xFF1E293B)).padding(12.dp).width(1200.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("المنتج", color = Color.White, modifier = Modifier.width(150.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("المرجع", color = Color.White, modifier = Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("الفئة", color = Color.White, modifier = Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("الوحدة", color = Color.White, modifier = Modifier.width(60.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("المخزون", color = Color.White, modifier = Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text("كمية مشتراة", color = Color.White, modifier = Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text("إجمالي الشراء (DA)", color = Color.White, modifier = Modifier.width(120.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("كمية مباعة", color = Color.White, modifier = Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text("إجمالي البيع (DA)", color = Color.White, modifier = Modifier.width(120.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("السجلات", color = Color.White, modifier = Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }

                    if (filteredProducts.isEmpty()) {
                        Box(modifier = Modifier.width(1200.dp).padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("لا توجد بيانات للعرض", color = Color.Gray)
                        }
                    } else {
                        filteredProducts.forEach { product ->
                            val pSales = salesTransactions.filter { it.productId == product.id }
                            val pSoldQty = pSales.sumOf { it.quantity }
                            val pSoldValue = pSales.sumOf { it.total }

                            val pPurchasedQty = product.stockQuantity + pSoldQty
                            val pPurchasedValue = pPurchasedQty * product.purchasePrice

                            Row(modifier = Modifier.width(1200.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(product.name, color = Color(0xFF1E293B), modifier = Modifier.width(150.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                                Text(product.barcode.ifBlank { "-" }, color = Color.Gray, modifier = Modifier.width(100.dp), fontSize = 11.sp)
                                Text(product.category, color = Color.Gray, modifier = Modifier.width(100.dp), fontSize = 11.sp)
                                Text(product.unitType, color = Color.Gray, modifier = Modifier.width(60.dp), fontSize = 11.sp)

                                val stockColor = if (product.stockQuantity < 0) Color(0xFFEF4444) else Color(0xFF10B981)
                                Text(product.stockQuantity.formatClean(), color = stockColor, modifier = Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)

                                Text(pPurchasedQty.formatClean(), color = Color(0xFF3B82F6), modifier = Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                Text(pPurchasedValue.formatClean(), color = Color(0xFF3B82F6), modifier = Modifier.width(120.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                Text(pSoldQty.formatClean(), color = Color(0xFF10B981), modifier = Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                Text(pSoldValue.formatClean(), color = Color(0xFF10B981), modifier = Modifier.width(120.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                Row(modifier = Modifier.width(100.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFF97316)).clickable { selectedProductForSales = product }, contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.ShoppingCart, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF3B82F6)).clickable { selectedProductForPurchases = product }, contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Receipt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (selectedProductForSales != null) {
        SalesHistoryDialog(product = selectedProductForSales!!, onDismiss = { selectedProductForSales = null })
    }
    if (selectedProductForPurchases != null) {
        PurchaseHistoryDialog(product = selectedProductForPurchases!!, onDismiss = { selectedProductForPurchases = null })
    }
}

@Composable
fun SalesHistoryDialog(product: Product, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var transactions by remember { mutableStateOf<List<ProductTransaction>>(emptyList()) }

    LaunchedEffect(product.id) {
        transactions = getProductTransactions(context, product.id, isSale = true)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF1E3A8A)).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("سجل البيع", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(product.name, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEF4444)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("التاريخ", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFF64748B))
                    Text("العميل", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFF64748B))
                    Text("الكمية", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFF64748B))
                    Text("السعر", modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFF64748B))
                    Text("المجموع", modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFF64748B))
                }
                HorizontalDivider(color = Color(0xFFE2E8F0))

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    if (transactions.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("لا توجد مبيعات مسجلة لهذا المنتج", color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        items(transactions) { t ->
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(t.date, modifier = Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.Center, color = Color(0xFF1E293B))
                                Text(t.partyName.ifBlank { "نقدي" }, modifier = Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.Center, color = Color(0xFF1E293B))
                                Text(t.quantity.formatClean(), modifier = Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.Center, color = Color(0xFF1E293B))
                                Text(t.unitPrice.toDzd(), modifier = Modifier.weight(1.2f), fontSize = 11.sp, textAlign = TextAlign.Center, color = Color(0xFF1E293B))
                                Text(t.total.toDzd(), modifier = Modifier.weight(1.2f), fontSize = 11.sp, textAlign = TextAlign.Center, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PurchaseHistoryDialog(product: Product, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var transactions by remember { mutableStateOf<List<ProductTransaction>>(emptyList()) }

    LaunchedEffect(product.id) {
        transactions = getProductTransactions(context, product.id, isSale = false)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF1E3A8A)).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("سجل الشراء", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(product.name, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEF4444)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("التاريخ", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFF64748B))
                    Text("المورد", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFF64748B))
                    Text("الكمية", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFF64748B))
                    Text("السعر", modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFF64748B))
                    Text("المجموع", modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFF64748B))
                }
                HorizontalDivider(color = Color(0xFFE2E8F0))

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    if (transactions.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("لا توجد مشتريات مسجلة لهذا المنتج", color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        items(transactions) { t ->
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(t.date, modifier = Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.Center, color = Color(0xFF1E293B))
                                Text(t.partyName.ifBlank { "غير معروف" }, modifier = Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.Center, color = Color(0xFF1E293B))
                                Text(t.quantity.formatClean(), modifier = Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.Center, color = Color(0xFF1E293B))
                                Text(t.unitPrice.toDzd(), modifier = Modifier.weight(1.2f), fontSize = 11.sp, textAlign = TextAlign.Center, color = Color(0xFF1E293B))
                                Text(t.total.toDzd(), modifier = Modifier.weight(1.2f), fontSize = 11.sp, textAlign = TextAlign.Center, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MovementStatCard(title: String, value: String, icon: ImageVector, color: Color, bgColor: Color, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = bgColor), shape = RoundedCornerShape(12.dp), modifier = modifier.height(80.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp), modifier = Modifier.size(28.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(16.dp)) }
                }
                Text(value, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF1E293B))
            }
            Text(title, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

// ==========================================
// 3️⃣ المنتجات الراكدة، التنبيهات وقائمة المنتجات
// ==========================================
@Composable
fun StagnantProductsReportScreen(inventoryManager: InventoryManager, isDark: Boolean, onBack: () -> Unit) {
    val stagnantProducts = inventoryManager.getProductList().filter { it.stockQuantity > 20 }.sortedByDescending { it.stockQuantity }
    val totalFrozenCapital = stagnantProducts.sumOf { it.purchasePrice * it.stockQuantity }

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.bg(isDark)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "عودة", tint = AppTheme.text(isDark)) }
            Text("المنتجات الراكدة 📦", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF97316).copy(alpha = 0.1f)), border = BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("إجمالي رأس المال المجمد", fontSize = 13.sp, color = AppTheme.text(isDark))
                Text(totalFrozenCapital.toDzd(), fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFFF97316))
                Spacer(Modifier.height(8.dp))
                Text("هذه المنتجات متوفرة بكميات كبيرة جداً، يُنصح بعمل تخفيضات عليها.", fontSize = 11.sp, color = AppTheme.subText(isDark), textAlign = TextAlign.Center)
            }
        }

        Spacer(Modifier.height(16.dp))

        if (stagnantProducts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("لا توجد منتجات راكدة.", color = AppTheme.subText(isDark)) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(stagnantProducts) { p ->
                    Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), border = BorderStroke(1.dp, AppTheme.inputBg(isDark)), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark), fontSize = 14.sp)
                                Text("المخزون: ${p.stockQuantity.formatClean()}", color = Color(0xFFF97316), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("التكلفة الإجمالية", fontSize = 10.sp, color = AppTheme.subText(isDark))
                                Text((p.purchasePrice * p.stockQuantity).toDzd(), fontWeight = FontWeight.Bold, color = AppTheme.text(isDark), fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StockAlertsReportScreen(inventoryManager: InventoryManager, isDark: Boolean, onBack: () -> Unit) {
    val lowStockProducts = inventoryManager.getProductList().filter { it.stockQuantity <= it.minStockAlert }

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.bg(isDark)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "عودة", tint = AppTheme.text(isDark)) }
            Text("تنبيه المخزون ⚠️", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
        }

        if (lowStockProducts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("المخزون بحالة جيدة ولا توجد نواقص.", color = AppTheme.subText(isDark)) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(lowStockProducts) { p ->
                    Card(colors = CardDefaults.cardColors(containerColor = AppTheme.ColorDanger.copy(alpha = 0.1f)), border = BorderStroke(1.dp, AppTheme.ColorDanger.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(p.name, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark), fontSize = 16.sp)
                                Text("حد التنبيه: ${p.minStockAlert.formatClean()}", fontSize = 12.sp, color = AppTheme.subText(isDark))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("الكمية", fontSize = 10.sp, color = AppTheme.subText(isDark))
                                Text(p.stockQuantity.formatClean(), fontWeight = FontWeight.Black, fontSize = 20.sp, color = AppTheme.ColorDanger)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductsListReportScreen(inventoryManager: InventoryManager, isDark: Boolean, onBack: () -> Unit) {
    val products = inventoryManager.getProductList()
    val totalCost = products.sumOf { it.purchasePrice * it.stockQuantity }
    val totalRetail = products.sumOf { it.sellingPrice * it.stockQuantity }

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.bg(isDark)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "عودة", tint = AppTheme.text(isDark)) }
            Text("قائمة المنتجات 📦", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("إجمالي التكلفة", fontSize = 11.sp, color = AppTheme.subText(isDark))
                    Text(totalCost.toDzd(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("القيمة البيعية", fontSize = 11.sp, color = AppTheme.subText(isDark))
                    Text(totalRetail.toDzd(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (products.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("لا توجد منتجات.", color = AppTheme.subText(isDark)) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(products) { product ->
                    Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), border = BorderStroke(1.dp, AppTheme.inputBg(isDark)), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark), fontSize = 14.sp)
                                Text("المخزون: ${product.stockQuantity.formatClean()} ${product.unitType}", color = AppTheme.subText(isDark), fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(product.sellingPrice.toDzd(), fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontSize = 14.sp)
                                Text("شراء: ${product.purchasePrice.toDzd()}", color = AppTheme.subText(isDark), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
