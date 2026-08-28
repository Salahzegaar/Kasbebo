package com.example.bookstoremanager.ui

import com.example.bookstoremanager.*
import com.example.bookstoremanager.data.*
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.coroutines.resume
import androidx.activity.compose.BackHandler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// مساعد فحص دور المستخدم الحالي
fun isCurrentUserAdmin(context: Context): Boolean {
    val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)
    return prefs.getString("user_role", "Admin") == "Admin"
}

@Composable
fun StoreManagerApp(inventoryManager: InventoryManager, isDarkMode: Boolean, onThemeChange: (Boolean) -> Unit, onDataChanged: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)

    val suppliersManager = remember { SuppliersManager(context) }
    val employeesManager = remember { EmployeesManager(context) }

    var isLocked by remember { mutableStateOf(prefs.getBoolean("is_pin_enabled", false)) }
    val savedPin = prefs.getString("pin_code", "") ?: ""

    if (isLocked && savedPin.isNotEmpty()) {
        PinLockScreen(savedPin = savedPin, isDark = isDarkMode) { isLocked = false }
        return
    }

    var refreshTrigger by remember { mutableStateOf(0) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentSubScreen by remember { mutableStateOf("none") }
    val currentBgColor = AppTheme.bg(isDarkMode)

    // =========================================================
    // 🌟 التحكم في زر الرجوع (Back Button) لمنع الخروج المفاجئ
    // =========================================================
    BackHandler(enabled = currentSubScreen != "none") {
        currentSubScreen = "none"
    }

    BackHandler(enabled = currentSubScreen == "none" && selectedTab != 0) {
        selectedTab = 0
    }
    // =========================================================

    Surface(modifier = Modifier.fillMaxSize(), color = currentBgColor) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (currentSubScreen == "none") {
                    MerchantSpotlightNavBar(selectedTab = selectedTab, isDark = isDarkMode) { newIndex ->
                        selectedTab = newIndex
                        currentSubScreen = "none"
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
                when (currentSubScreen) {
                    "pos" -> POSScreen(isDark = isDarkMode, inventoryManager = inventoryManager) { currentSubScreen = "none" }
                    "analytics" -> AnalyticsScreen(isDark = isDarkMode, inventoryManager = inventoryManager) { currentSubScreen = "none" }
                    "stock_alerts" -> StockAlertsScreen(isDark = isDarkMode, inventoryManager = inventoryManager) { currentSubScreen = "none" }

                    // 🚀 السطر الذي تم إصلاحه بدقة:
                    "inventory_audit" -> InventoryAuditScreen(isDark = isDarkMode, inventoryManager = inventoryManager) { currentSubScreen = "none" }

                    "settings" -> SettingsScreen(
                        inventoryManager = inventoryManager,
                        isDark = isDarkMode,
                        onThemeChange = onThemeChange,
                        onDataChanged = onDataChanged,
                        onBack = { currentSubScreen = "none" }
                    )

                    "invoices" -> InvoicesTab(inventoryManager, refreshTrigger, isDarkMode) { currentSubScreen = "none" }
                    "suppliers" -> SuppliersTab(suppliersManager, isDarkMode) { currentSubScreen = "none" }
                    "employees" -> EmployeesTab(employeesManager, isDarkMode) { currentSubScreen = "none" }
                    "reports" -> ReportsTab(inventoryManager, suppliersManager, employeesManager, isDarkMode) { currentSubScreen = "none" }

                    "merchant_orders" -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                IconButton(onClick = { currentSubScreen = "none" }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = AppTheme.text(isDarkMode))
                                }
                                Text("عودة للقائمة", color = AppTheme.text(isDarkMode), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                val storeId = prefs.getString("store_secret_id", "") ?: ""
                                MerchantOrdersView(storeId = storeId, isDark = isDarkMode)
                            }
                        }
                    }

                    "none" -> {
                        when (selectedTab) {
                            0 -> ERPDashboardTab(isDark = isDarkMode, inventoryManager = inventoryManager, suppliersManager = suppliersManager, refreshTrigger = refreshTrigger, onNavigateToSubScreen = { screenKey -> currentSubScreen = screenKey }, onNavigateToTab = { tabIndex -> selectedTab = tabIndex }, onDataChanged = onDataChanged)
                            1 -> InventoryTab(inventoryManager, refreshTrigger, isDarkMode) { refreshTrigger++; onDataChanged() }
                            2 -> CustomersTab(inventoryManager, refreshTrigger, isDarkMode) { refreshTrigger++; onDataChanged() }
                            3 -> MoreTab(
                                isDark = isDarkMode,
                                onNavigateToSettings = { currentSubScreen = "settings" },
                                onNavigateToInvoices = { currentSubScreen = "invoices" },
                                onNavigateToSuppliers = { currentSubScreen = "suppliers" },
                                onNavigateToEmployees = { currentSubScreen = "employees" },
                                onNavigateToReports = { currentSubScreen = "reports" },
                                onNavigateToOrders = { currentSubScreen = "merchant_orders" }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MerchantSpotlightNavBar(selectedTab: Int, isDark: Boolean = false, onTabSelected: (Int) -> Unit) {
    val navBarBg = if (isDark) Color(0xFF131B2E) else Color(0xFFFFFFFF)
    val navBarBorder = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val activeGlowColor = AppTheme.GoldPrimary

    Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        Surface(color = navBarBg, shape = RoundedCornerShape(32.dp), shadowElevation = 14.dp, border = BorderStroke(1.dp, navBarBorder), modifier = Modifier.fillMaxWidth().height(68.dp)) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                MerchantSpotlightNavItem(Icons.Default.Home, selectedTab == 0, activeGlowColor, isDark) { onTabSelected(0) }
                MerchantSpotlightNavItem(Icons.AutoMirrored.Filled.List, selectedTab == 1, activeGlowColor, isDark) { onTabSelected(1) }
                MerchantSpotlightNavItem(Icons.Default.Person, selectedTab == 2, activeGlowColor, isDark) { onTabSelected(2) }
                MerchantSpotlightNavItem(Icons.Default.Menu, selectedTab == 3, activeGlowColor, isDark) { onTabSelected(3) }
            }
        }
    }
}

@Composable
fun MerchantSpotlightNavItem(icon: ImageVector, isSelected: Boolean, activeColor: Color, isDark: Boolean, onClick: () -> Unit) {
    val alpha by animateFloatAsState(targetValue = if (isSelected) 1f else 0f, animationSpec = tween(280), label = "glowAlpha")
    val unselectedColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val iconColor by animateColorAsState(targetValue = if (isSelected) activeColor else unselectedColor, animationSpec = tween(280), label = "iconColor")

    Box(modifier = Modifier.fillMaxHeight().width(62.dp).clickable { onClick() }, contentAlignment = Alignment.TopCenter) {
        if (alpha > 0f) {
            Box(modifier = Modifier.width(34.dp).height(3.5.dp).clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)).background(activeColor))
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(activeColor.copy(alpha = 0.18f * alpha), activeColor.copy(alpha = 0.03f * alpha), Color.Transparent))))
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(25.dp)) }
    }
}

@Composable
fun AnalyticsScreen(isDark: Boolean, inventoryManager: InventoryManager, onBack: () -> Unit) {
    val totalCash = inventoryManager.getTotalCashCollected()
    val netProfit = inventoryManager.getNetRealProfit()

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.bg(isDark))) {
        Row(modifier = Modifier.fillMaxWidth().background(AppTheme.card(isDark)).padding(16.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppTheme.text(isDark)) }
            Spacer(Modifier.width(8.dp))
            Text("التحليل المالي 📊", color = AppTheme.text(isDark), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnalyticsCard("إجمالي المبيعات", "${totalCash.formatClean()} د.ج", Color(0xFF10B981), Icons.Default.TrendingUp, isDark, Modifier.weight(1f))
                AnalyticsCard("الربح الصافي", "${netProfit.formatClean()} د.ج", AppTheme.GoldPrimary, Icons.Default.AccountBalanceWallet, isDark, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Text("تفاصيل المنتجات والمخزون", fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("إجمالي قيمة رأس المال (المخزون):", color = AppTheme.subText(isDark), fontSize = 12.sp)
                    Text("${inventoryManager.getTotalInventoryCost().formatClean()} د.ج", color = AppTheme.text(isDark), fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun AnalyticsCard(title: String, value: String, color: Color, icon: ImageVector, isDark: Boolean, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, AppTheme.inputBg(isDark)), modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(color = color.copy(alpha = 0.15f), shape = CircleShape, modifier = Modifier.size(36.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(18.dp)) } }
            Spacer(Modifier.height(12.dp))
            Text(title, fontSize = 12.sp, color = AppTheme.subText(isDark), fontWeight = FontWeight.Bold)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun StockAlertsScreen(isDark: Boolean, inventoryManager: InventoryManager, onBack: () -> Unit) {
    val lowStockProducts = inventoryManager.getProductList().filter { it.stockQuantity <= it.minStockAlert }
    Column(modifier = Modifier.fillMaxSize().background(AppTheme.bg(isDark))) {
        Row(modifier = Modifier.fillMaxWidth().background(AppTheme.card(isDark)).padding(16.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppTheme.text(isDark)) }; Spacer(Modifier.width(8.dp)); Text("تنبيهات المخزون ⚠️", color = AppTheme.text(isDark), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.padding(16.dp)) {
            if (lowStockProducts.isEmpty()) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("المخزون بحالة ممتازة!", color = AppTheme.subText(isDark)) } } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(lowStockProducts) { p -> Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), border = BorderStroke(1.dp, AppTheme.inputBg(isDark)), modifier = Modifier.fillMaxWidth()) { Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text(p.name, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark)); Text("حد التنبيه: ${p.minStockAlert.formatClean()}", fontSize = 11.sp, color = AppTheme.subText(isDark)) }; Text("المتبقي: ${p.stockQuantity.formatClean()}", fontWeight = FontWeight.Black, color = Color(0xFFEF4444)) } } }
                }
            }
        }
    }
}

@Composable
fun InventoryAuditScreen(isDark: Boolean, inventoryManager: InventoryManager, onBack: () -> Unit) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    // خريطة لحفظ الكميات التي قام المستخدم بجردها (المعرف -> الكمية الفعلية)
    var auditedQuantities by remember { mutableStateOf(mapOf<String, Double>()) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val allProducts = inventoryManager.getProductList()
    val filteredProducts = remember(searchQuery, allProducts.size) {
        if (searchQuery.isNotBlank()) {
            allProducts.filter { it.name.contains(searchQuery, ignoreCase = true) || it.barcode.contains(searchQuery) }
        } else {
            allProducts
        }
    }

    // 🧮 حسابات ملخص الجرد
    val totalProducts = allProducts.size
    val countedProducts = auditedQuantities.size
    var diffCount = 0
    var qtyDiff = 0.0
    var valueDiff = 0.0

    auditedQuantities.forEach { (id, actualQty) ->
        val product = allProducts.find { it.id == id }
        if (product != null) {
            val sysQty = product.stockQuantity
            if (actualQty != sysQty) {
                diffCount++
                val diff = actualQty - sysQty
                qtyDiff += diff
                valueDiff += diff * product.purchasePrice // قيمة العجز أو الزيادة بسعر الشراء
            }
        }
    }

    // ألوان التصميم المخصص من صورتك
    val blueButtonColor = Color(0xFF38BDF8)
    val orangeButtonColor = Color(0xFFF97316)
    val darkHeaderColor = Color(0xFF1E293B)

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.bg(isDark))) {
        // 1. الشريط العلوي
        Row(
            modifier = Modifier.fillMaxWidth().background(darkHeaderColor).padding(16.dp).statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Inventory, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("الجرد", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
            }
        }

        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            // 2. بطاقة البحث والأزرار
            Card(
                colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AppTheme.cardBorder(isDark)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("الكود، الاسم، المرجع...", color = AppTheme.subText(isDark)) },
                        label = { Text("بحث") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = blueButtonColor,
                            unfocusedBorderColor = AppTheme.cardBorder(isDark),
                            focusedTextColor = AppTheme.text(isDark),
                            unfocusedTextColor = AppTheme.text(isDark)
                        )
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                if (auditedQuantities.isNotEmpty()) showConfirmDialog = true
                                else Toast.makeText(context, "لم تقم بجرد أي منتج بعد!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = blueButtonColor),
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("تأكيد الجرد", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Button(
                            onClick = { auditedQuantities = emptyMap() },
                            colors = ButtonDefaults.buttonColors(containerColor = orangeButtonColor),
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("مسح الادخال", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }

            // 3. بطاقة ملخص الجرد
            Card(
                colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AppTheme.cardBorder(isDark)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AuditSummaryRow("المنتجات", totalProducts.toString(), isDark)
                    HorizontalDivider(color = AppTheme.inputBg(isDark), modifier = Modifier.padding(vertical = 8.dp))
                    AuditSummaryRow("منتجات محسوبة", countedProducts.toString(), isDark)
                    HorizontalDivider(color = AppTheme.inputBg(isDark), modifier = Modifier.padding(vertical = 8.dp))
                    AuditSummaryRow("فروقات", diffCount.toString(), isDark)
                    HorizontalDivider(color = AppTheme.inputBg(isDark), modifier = Modifier.padding(vertical = 8.dp))
                    AuditSummaryRow("فرق الكمية", qtyDiff.formatClean(), isDark)
                    HorizontalDivider(color = AppTheme.inputBg(isDark), modifier = Modifier.padding(vertical = 8.dp))
                    AuditSummaryRow("قيمة الفرق", "${valueDiff.formatClean()} دج", isDark, isBold = true)
                }
            }

            // 4. جدول المنتجات
            Card(
                colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AppTheme.cardBorder(isDark)),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Column {
                    // رأس الجدول
                    Row(
                        modifier = Modifier.fillMaxWidth().background(darkHeaderColor).padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("المنتج", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                        Text("النظام", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("الفعلي", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                    }

                    // قائمة المنتجات
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredProducts, key = { it.id }) { product ->
                            AuditProductItem(
                                product = product,
                                auditedQty = auditedQuantities[product.id],
                                isDark = isDark,
                                onQtyChange = { newQty ->
                                    val currentMap = auditedQuantities.toMutableMap()
                                    if (newQty != null) {
                                        currentMap[product.id] = newQty
                                    } else {
                                        currentMap.remove(product.id)
                                    }
                                    auditedQuantities = currentMap
                                }
                            )
                            HorizontalDivider(color = AppTheme.inputBg(isDark))
                        }
                    }
                }
            }
        }
    }

    // نافذة تأكيد تسوية المخزون
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = AppTheme.card(isDark),
            title = { Text("تأكيد تسوية المخزون", fontWeight = FontWeight.Bold, color = AppTheme.text(isDark)) },
            text = { Text("سيتم تحديث كميات $countedProducts منتج في النظام بناءً على الجرد الفعلي. هل أنت متأكد؟", color = AppTheme.subText(isDark)) },
            confirmButton = {
                Button(
                    onClick = {
                        // تحديث المنتجات في النظام
                        auditedQuantities.forEach { (id, actualQty) ->
                            val p = inventoryManager.getProductList().find { it.id == id }
                            if (p != null && p.stockQuantity != actualQty) {
                                p.stockQuantity = actualQty
                                inventoryManager.updateProduct(p)
                            }
                        }
                        Toast.makeText(context, "تم تسوية المخزون بنجاح! ✅", Toast.LENGTH_LONG).show()
                        auditedQuantities = emptyMap()
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = blueButtonColor)
                ) { Text("نعم، حدّث المخزون", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("إلغاء", color = AppTheme.subText(isDark)) }
            }
        )
    }
}

@Composable
fun AuditSummaryRow(label: String, value: String, isDark: Boolean, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = AppTheme.subText(isDark), fontSize = 14.sp)
        Text(value, color = AppTheme.text(isDark), fontWeight = if (isBold) FontWeight.Black else FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun AuditProductItem(product: Product, auditedQty: Double?, isDark: Boolean, onQtyChange: (Double?) -> Unit) {
    // حالة محلية للحقل النصي لضمان سلاسة الكتابة
    var textValue by remember(auditedQty) {
        mutableStateOf(auditedQty?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(2f)) {
            Text(product.name, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark), fontSize = 14.sp)
            Text(product.barcode.ifBlank { "بدون كود" }, color = AppTheme.subText(isDark), fontSize = 11.sp)
        }

        Text(
            text = product.stockQuantity.formatClean(),
            color = AppTheme.subText(isDark),
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )

        OutlinedTextField(
            value = textValue,
            onValueChange = {
                textValue = it
                onQtyChange(it.replace(",", ".").toDoubleOrNull())
            },
            placeholder = { Text("-", color = AppTheme.subText(isDark)) },
            modifier = Modifier.weight(1.5f).height(50.dp),
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = AppTheme.cardBorder(isDark),
                focusedTextColor = AppTheme.text(isDark),
                unfocusedTextColor = AppTheme.text(isDark)
            )
        )
    }
}

// ==========================================
// 📱 شاشة المزيد (More Tab) محصنة بالصلاحيات
// ==========================================
@Composable
fun MoreTab(
    isDark: Boolean,
    onNavigateToSettings: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onNavigateToSuppliers: () -> Unit,
    onNavigateToEmployees: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToOrders: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)
    val storeId = prefs.getString("store_secret_id", "") ?: ""
    val isAdmin = remember { isCurrentUserAdmin(context) }

    var showPrinterSettings by remember { mutableStateOf(false) }
    var showAdminPinPrompt by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("المزيد من الخيارات", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                GridMenuItem("طلبات الزبائن", Icons.Default.ShoppingCart, AppTheme.GoldPrimary, isDark) {
                    if (storeId.isNotBlank()) onNavigateToOrders()
                    else Toast.makeText(context, "الرجاء ضبط كود المتجر السري في الإعدادات أولاً!", Toast.LENGTH_SHORT).show()
                }
            }
            Box(modifier = Modifier.weight(1f)) { GridMenuItem("التقارير", Icons.Default.DateRange, AppTheme.ColorSuccess, isDark) { onNavigateToReports() } }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) { GridMenuItem("المبيعات والفواتير", Icons.Default.ReceiptLong, Color(0xFF10B981), isDark) { onNavigateToInvoices() } }
            Box(modifier = Modifier.weight(1f)) { GridMenuItem("الموردون", Icons.Default.ShoppingCart, Color(0xFF3B82F6), isDark) { onNavigateToSuppliers() } }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) { GridMenuItem("الموظفين", Icons.Default.Person, Color(0xFF8B5CF6), isDark) { onNavigateToEmployees() } }

            Box(modifier = Modifier.weight(1f)) {
                GridMenuItem("الإعدادات", Icons.Default.Settings, Color.Gray, isDark) {
                    if (isAdmin) { onNavigateToSettings() } else { showAdminPinPrompt = true }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) { GridMenuItem("طابعات (Bluetooth)", Icons.Default.Build, AppTheme.ColorDanger, isDark) { showPrinterSettings = true } }
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(80.dp))
    }

    if (showPrinterSettings) { PrinterSettingsDialog(isDark = isDark) { showPrinterSettings = false } }

    if (showAdminPinPrompt) {
        AlertDialog(
            onDismissRequest = { showAdminPinPrompt = false },
            containerColor = AppTheme.card(isDark),
            title = { Text("🔒 صلاحيات المدير مطلوبة", fontWeight = FontWeight.Bold, color = AppTheme.text(isDark)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل كلمة مرور المدير للوصول إلى الإعدادات:", color = AppTheme.subText(isDark), fontSize = 12.sp)
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { enteredPin = it },
                        label = { Text("كلمة المرور") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTheme.text(isDark), unfocusedTextColor = AppTheme.text(isDark),
                            focusedContainerColor = AppTheme.inputBg(isDark), unfocusedContainerColor = AppTheme.inputBg(isDark),
                            focusedBorderColor = AppTheme.GoldPrimary, unfocusedBorderColor = AppTheme.cardBorder(isDark)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val savedUsersJson = prefs.getString("app_users", "{}") ?: "{}"
                        var adminPwd = "admin"
                        try {
                            val usersObj = org.json.JSONObject(savedUsersJson)
                            if (usersObj.has("admin")) { adminPwd = usersObj.getString("admin") }
                        } catch (e: Exception) { e.printStackTrace() }

                        if (enteredPin == adminPwd) {
                            showAdminPinPrompt = false
                            enteredPin = ""
                            onNavigateToSettings()
                        } else {
                            Toast.makeText(context, "كلمة المرور غير صحيحة!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.GoldPrimary)
                ) { Text("دخول", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showAdminPinPrompt = false }) { Text("إلغاء", color = AppTheme.subText(isDark)) } }
        )
    }
}

@Composable
fun GridMenuItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, isDark: Boolean, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, AppTheme.cardBorder(isDark)), modifier = Modifier.fillMaxWidth().height(110.dp).clickable { onClick() }) {
        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) { Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(48.dp)) { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(28.dp)) } }; Spacer(Modifier.height(10.dp)); Text(title, color = AppTheme.text(isDark), fontSize = 13.sp, fontWeight = FontWeight.Bold) }
    }
}

class BluetoothPrinterManager(private val context: Context) {
    private val bluetoothAdapter: android.bluetooth.BluetoothAdapter? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) { (context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter } else { @Suppress("DEPRECATION") android.bluetooth.BluetoothAdapter.getDefaultAdapter() }
    private val SPP_UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @android.annotation.SuppressLint("MissingPermission")
    fun getPairedDevices(): List<android.bluetooth.BluetoothDevice> {
        if (bluetoothAdapter == null) return emptyList()
        return bluetoothAdapter.bondedDevices?.toList() ?: emptyList()
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun printReceipt(deviceAddress: String, receiptText: String, onResult: (Boolean, String) -> Unit) {
        if (bluetoothAdapter == null) { onResult(false, "البلوتوث غير متوفر"); return }
        Thread { try { val device = bluetoothAdapter.getRemoteDevice(deviceAddress); bluetoothAdapter.cancelDiscovery(); val socket: android.bluetooth.BluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID); socket.connect(); val outputStream = socket.outputStream; outputStream.write(byteArrayOf(0x1B.toByte(), 0x40.toByte())); outputStream.write(receiptText.toByteArray(charset("CP1256"))); outputStream.write(byteArrayOf(0x0A.toByte(), 0x0A.toByte(), 0x0A.toByte())); outputStream.flush(); outputStream.close(); socket.close(); onResult(true, "تمت الطباعة بنجاح! 🖨️") } catch (e: Exception) { onResult(false, "فشل الاتصال بالطابعة") } }.start()
    }
}

@Composable
fun PrinterSettingsDialog(isDark: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val printerManager = remember { BluetoothPrinterManager(context) }
    val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)
    var pairedDevices by remember { mutableStateOf(emptyList<android.bluetooth.BluetoothDevice>()) }
    var selectedDeviceAddress by remember { mutableStateOf(prefs.getString("saved_printer_mac", "") ?: "") }
    var statusMessage by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { pairedDevices = printerManager.getPairedDevices() }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = AppTheme.card(isDark), shape = RoundedCornerShape(20.dp),
        title = { Text("🖨️ إعدادات الطابعات", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(300.dp)) { Text("الأجهزة المقترنة:", color = AppTheme.subText(isDark), fontSize = 12.sp); LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) { itemsIndexed(pairedDevices, key = { index, device -> "${device.address}_$index" }) { _, device -> @android.annotation.SuppressLint("MissingPermission") val name = device.name ?: "طابعة"; val isSelected = device.address == selectedDeviceAddress; Card(colors = CardDefaults.cardColors(containerColor = if (isSelected) AppTheme.GoldPrimary.copy(alpha = 0.2f) else AppTheme.inputBg(isDark)), border = BorderStroke(1.dp, if (isSelected) AppTheme.GoldPrimary else AppTheme.cardBorder(isDark)), modifier = Modifier.fillMaxWidth().clickable { selectedDeviceAddress = device.address; prefs.edit().putString("saved_printer_mac", device.address).apply() }) { Column(modifier = Modifier.padding(10.dp)) { Text(name, color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(device.address, color = AppTheme.subText(isDark), fontSize = 11.sp) } } } }; if (statusMessage.isNotBlank()) { Text(statusMessage, color = AppTheme.GoldPrimary, fontSize = 12.sp) } } },
        confirmButton = { Button(onClick = { if (selectedDeviceAddress.isNotBlank()) { statusMessage = "جاري الإرسال..."; printerManager.printReceipt(selectedDeviceAddress, "تجربة طباعة كاسبو\n") { _, msg -> statusMessage = msg } } }, modifier = Modifier.fillMaxWidth()) { Text("طباعة تجريبية") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق", color = AppTheme.subText(isDark)) } }
    )
}

@Composable
fun InvoicesTab(
    inventoryManager: InventoryManager,
    refreshTrigger: Int,
    isDark: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)
    val printerManager = remember { BluetoothPrinterManager(context) }

    // 🧮 تجميع المبيعات التي تمت في نفس الوقت لتكوين "فاتورة واحدة"
    val groupedInvoices = remember(refreshTrigger, inventoryManager.getSalesHistory().size) {
        inventoryManager.getSalesHistory()
        .groupBy { it.timestamp }
        .toList()
        .sortedByDescending { it.first } // الأحدث أولاً
    }

    val totalInvoicesCount = groupedInvoices.size

    // متغير للتحكم في ظهور نافذة المعاينة
    var selectedInvoiceText by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.bg(isDark))) {
        // 💎 الهيدر
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "عودة", tint = AppTheme.text(isDark))
                }
                Text("سجل المبيعات والفواتير", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
            }
            Icon(Icons.Default.ReceiptLong, null, tint = AppTheme.GoldPrimary, modifier = Modifier.size(28.dp))
        }

        if (groupedInvoices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Receipt, null, tint = AppTheme.subText(isDark), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("لا توجد فواتير حتى الآن", color = AppTheme.subText(isDark), fontSize = 16.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(groupedInvoices) { index, (timestamp, itemsList) ->
                    val sequentialNum = totalInvoicesCount - index
                    val yearStr = java.text.SimpleDateFormat("yy", java.util.Locale.US).format(java.util.Date(timestamp))
                    val formattedInvoiceId = String.format(java.util.Locale.US, "%03d/%s", sequentialNum, yearStr)
                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
                    val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timestamp))

                    val invoiceTotal = itemsList.sumOf { it.revenue }
                    val itemsCount = itemsList.size

                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AppTheme.cardBorder(isDark)),
                        // 🚀 جعل الفاتورة قابلة للضغط لفتح المعاينة
                        modifier = Modifier.fillMaxWidth().clickable {
                            // بناء نص الفاتورة للطباعة
                            var text = "🧾 فاتورة مبيعات - كاسبو Kasebo\n"
                            text += "رقم الفاتورة: $formattedInvoiceId\n"
                            text += "التاريخ: $dateStr | الوقت: $timeStr\n"
                            text += "--------------------------------\n"
                            itemsList.forEach { sale ->
                                val price = if(sale.quantity > 0) sale.revenue / sale.quantity else 0.0
                                text += "▪ ${sale.productName}\n  ${sale.quantity.formatClean()} × ${price.formatClean()} = ${sale.revenue.formatClean()} دج\n"
                            }
                            text += "--------------------------------\n"
                            text += "💰 المجموع الإجمالي: ${invoiceTotal.formatClean()} دج\n"
                            text += "      شكراً لزيارتكم!\n"
                            text += "\n\n"

                            selectedInvoiceText = text
                        }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Tag, null, tint = AppTheme.GoldPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("فاتورة: $formattedInvoiceId", color = AppTheme.GoldPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                }
                                Text("+ ${invoiceTotal.formatClean()} د.ج", color = Color(0xFF10B981), fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }

                            HorizontalDivider(color = AppTheme.inputBg(isDark), modifier = Modifier.padding(vertical = 10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if(itemsCount == 1) itemsList.first().productName else "فاتورة متعددة المنتجات ($itemsCount منتجات)", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Icon(Icons.Default.Print, null, tint = AppTheme.subText(isDark), modifier = Modifier.size(18.dp))
                            }

                            Spacer(Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DateRange, null, tint = AppTheme.subText(isDark), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(dateStr, color = AppTheme.subText(isDark), fontSize = 12.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccessTime, null, tint = AppTheme.subText(isDark), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(timeStr, color = AppTheme.subText(isDark), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 🖨️ نافذة معاينة الفاتورة والطباعة
    selectedInvoiceText?.let { invoiceText ->
        InvoicePreviewDialog(
            invoiceText = invoiceText,
            isDark = isDark,
            onDismiss = { selectedInvoiceText = null },
            onPrint = {
                val mac = prefs.getString("saved_printer_mac", "") ?: ""
                if (mac.isNotBlank()) {
                    Toast.makeText(context, "جاري إرسال الفاتورة للطابعة...", Toast.LENGTH_SHORT).show()
                    printerManager.printReceipt(mac, invoiceText) { success, msg ->
                        // سيتم إظهار رسالة النجاح من داخل الـ Thread
                    }
                } else {
                    Toast.makeText(context, "لم يتم تحديد طابعة! يرجى ربط طابعة من الإعدادات.", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}
@Composable
fun InvoicePreviewDialog(
    invoiceText: String,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onPrint: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.card(isDark),
        shape = RoundedCornerShape(16.dp),
        title = {
            Text("معاينة الفاتورة 🧾", fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
        },
        text = {
            // محاكاة شكل الورقة الحرارية (خلفية بيضاء دائماً لتوضيح الطباعة)
            Surface(
                color = Color(0xFFFDFDFD),
                shape = RoundedCornerShape(4.dp),
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 250.dp, max = 400.dp)
            ) {
                Column(
                    modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                ) {
                    Text(
                        text = invoiceText,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = Color.Black,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onPrint,
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.GoldPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Print, contentDescription = "طباعة", tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("طباعة الفاتورة", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إغلاق", color = AppTheme.subText(isDark))
            }
        }
    )
}
