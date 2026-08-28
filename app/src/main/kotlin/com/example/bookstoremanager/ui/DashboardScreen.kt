package com.example.bookstoremanager.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bookstoremanager.AppTheme
import com.example.bookstoremanager.PdfGenerator
import com.example.bookstoremanager.data.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ERPDashboardTab(
    isDark: Boolean,
    inventoryManager: InventoryManager,
    suppliersManager: SuppliersManager? = null,
    refreshTrigger: Int,
    onNavigateToSubScreen: (String) -> Unit,
    onNavigateToTab: (Int) -> Unit,
    onDataChanged: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)
    val isAdmin = prefs.getString("user_role", "Admin") == "Admin"
    val storeName = prefs.getString("store_name", "كاسبو - Kasebo POS") ?: "كاسبو - Kasebo POS"

    val lowStockCount = inventoryManager.getLowStockCount()
    val totalCash = remember(refreshTrigger) { inventoryManager.getTotalCashCollected() }
    val totalExp = remember(refreshTrigger) { inventoryManager.getTotalExpenses() }
    val netProfit = remember(refreshTrigger) { inventoryManager.getNetRealProfit() }
    val receivable = remember(refreshTrigger) { inventoryManager.getTotalStoreReceivable() }
    val payable = 0.0
    val inventoryCost = remember(refreshTrigger) { inventoryManager.getTotalInventoryCost() }
    val salesHistory = remember(refreshTrigger) { inventoryManager.getSalesHistory() }

    var showExpenseDialog by remember { mutableStateOf(false) }
    var showSalesHistoryDialog by remember { mutableStateOf(false) }
    var showFinancialReportDialog by remember { mutableStateOf(false) }
    var showDailyInsightsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // الهيدر
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(16.dp)).background(AppTheme.PremiumGold.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Storefront, null, tint = AppTheme.PremiumGold, modifier = Modifier.size(28.dp)) }
                Column {
                    Text(storeName, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = AppTheme.text(isDark))
                    Text(if (isAdmin) "نظرة عامة (المدير)" else "نقطة البيع (الكاشير)", fontSize = 12.sp, color = AppTheme.subText(isDark))
                }
            }
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(AppTheme.inputBg(isDark)).clickable { showDailyInsightsDialog = true },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Lightbulb, contentDescription = "الملخص", tint = AppTheme.PremiumGold, modifier = Modifier.size(24.dp)) }
        }

        // البطاقة المالية
        PremiumCard(isDark = isDark) {
            Text("إجمالي المبيعات المحصلة", color = AppTheme.subText(isDark), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text(totalCash.toDzd(), color = AppTheme.PremiumGold, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)

            if (isAdmin) {
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("صافي الربح", color = AppTheme.subText(isDark), fontSize = 11.sp)
                        Text(netProfit.toDzd(), color = AppTheme.ColorSuccess, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("المصروفات", color = AppTheme.subText(isDark), fontSize = 11.sp)
                        Text(totalExp.toDzd(), color = AppTheme.ColorDanger, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // بطاقة المخزون
        if (isAdmin) {
            PremiumCard(isDark = isDark) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("قيمة البضاعة في المخزون", color = AppTheme.subText(isDark), fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(inventoryCost.toDzd(), color = AppTheme.text(isDark), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.size(46.dp).clip(CircleShape).background(AppTheme.inputBg(isDark)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Inventory2, null, tint = AppTheme.subText(isDark))
                    }
                }
            }
            PremiumButton(text = "عرض التقرير المالي الشامل", icon = Icons.Default.Analytics, modifier = Modifier.fillMaxWidth()) { showFinancialReportDialog = true }
        }

        // المهام السريعة
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PremiumCard(isDark = isDark, modifier = Modifier.weight(1f).height(64.dp), onClick = { showSalesHistoryDialog = true }) {
                Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.ReceiptLong, null, tint = AppTheme.PremiumGold, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("سجل المبيعات", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            if (isAdmin) {
                PremiumCard(isDark = isDark, modifier = Modifier.weight(1f).height(64.dp), onClick = { showExpenseDialog = true }) {
                    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.MoneyOff, null, tint = AppTheme.ColorDanger, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("المصروفات", color = AppTheme.ColorDanger, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // بطاقة الديون
        if (isAdmin && (receivable > 0.0 || payable > 0.0)) {
            PremiumCard(isDark = isDark) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("ديون لك (الزبائن)", color = AppTheme.subText(isDark), fontSize = 11.sp)
                        Text(receivable.toDzd(), color = AppTheme.ColorSuccess, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("ديون عليك (الموردين)", color = AppTheme.subText(isDark), fontSize = 11.sp)
                        Text(payable.toDzd(), color = AppTheme.ColorDanger, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Text("الوصول السريع", fontWeight = FontWeight.Bold, color = AppTheme.text(isDark), fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))

        val menuItems = listOf(
            MenuGridItem("بيع جديد (POS)", Icons.Default.PointOfSale, "pos", true),
            MenuGridItem("التحليل والأرباح", Icons.Default.PieChart, "analytics", true),
            MenuGridItem("إدارة المنتجات", Icons.Default.Category, "1", false),
            MenuGridItem("العملاء والديون", Icons.Default.People, "2", false),
            MenuGridItem("تدقيق الجرد", Icons.Default.FactCheck, "inventory_audit", true),
            MenuGridItem("الإعدادات", Icons.Default.Settings, "settings", true)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(350.dp)
        ) {
            items(menuItems) { item ->
                PremiumCard(isDark = isDark, modifier = Modifier.height(100.dp), onClick = {
                        if (item.isSubScreen) onNavigateToSubScreen(item.dest) else onNavigateToTab(item.dest.toInt())
                }) {
                    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(item.icon, null, tint = AppTheme.PremiumGold, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(10.dp))
                        Text(item.title, color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(90.dp))
    }

    // استدعاء النوافذ المنبثقة
    if (showExpenseDialog) ExpenseDialog(inventoryManager, isDark, { showExpenseDialog = false }, onDataChanged)
    if (showSalesHistoryDialog) StoreSalesHistoryDialog(inventoryManager, refreshTrigger, isDark) { showSalesHistoryDialog = false }
    if (showFinancialReportDialog) FinancialReportDialog(inventoryManager, refreshTrigger, isDark) { showFinancialReportDialog = false }
    if (showDailyInsightsDialog) DailySmartInsightsDialog(inventoryManager, isDark) { showDailyInsightsDialog = false }
}

data class MenuGridItem(val title: String, val icon: ImageVector, val dest: String, val isSubScreen: Boolean)

@Composable
fun TopProductsChart(salesHistory: List<SaleRecord>, isDark: Boolean) {
    val topProducts = salesHistory.groupBy { it.productName }.mapValues { entry -> entry.value.sumOf { it.revenue } }.toList().sortedByDescending { it.second }.take(5)
    val maxRevenue = topProducts.maxOfOrNull { it.second }?.toFloat() ?: 1f
    val barColor = AppTheme.PremiumGold
    val textColor = android.graphics.Color.parseColor(if (isDark) "#94A3B8" else "#64748B")

    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        val width = size.width
        val height = size.height
        val calculatedWidth = width / (topProducts.size * 2f)
        val barWidth = calculatedWidth.coerceIn(30f, 70f)
        val space = barWidth
        val paint = android.graphics.Paint().apply { color = textColor; textSize = 28f; textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true }

        topProducts.forEachIndexed { index, pair ->
            val productName = pair.first.take(8) + if (pair.first.length > 8) ".." else ""
            val revenue = pair.second.toFloat()
            val maxHeight = height - 50f
            val barHeight = ((revenue / maxRevenue) * maxHeight).coerceIn(20f, maxHeight)
            val totalGroupWidth = (topProducts.size * barWidth) + ((topProducts.size - 1) * space)
            val startX = (width - totalGroupWidth) / 2f
            val x = startX + (index * (barWidth + space))
            val y = height - barHeight - 35f

            drawRoundRect(color = barColor, topLeft = Offset(x, y), size = Size(barWidth, barHeight), cornerRadius = CornerRadius(16f, 16f))
            drawContext.canvas.nativeCanvas.drawText(productName, x + (barWidth / 2), height - 10f, paint)
        }
    }
}

// ==========================================
// 🧩 النوافذ المنبثقة (المصروفات، المبيعات، التقارير) التي كانت مفقودة
// ==========================================

@Composable
fun ExpenseDialog(inventoryManager: InventoryManager, isDark: Boolean, onDismiss: () -> Unit, onDataChanged: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = AppTheme.card(isDark), shape = RoundedCornerShape(20.dp),
        title = { Text("تسجيل مصروف 💸", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumTextField(value = title, onValueChange = { title = it }, label = "البيان (إيجار، كهرباء..)", isDark = isDark)
                PremiumTextField(value = amount, onValueChange = { amount = it }, label = "المبلغ (دج)", isDark = isDark, isNumber = true)
            }
        },
        confirmButton = {
            PremiumButton(text = "حفظ المصروف", isDanger = true, modifier = Modifier.fillMaxWidth()) {
                val amt = amount.replace(",", ".").toDoubleOrNull()
                if (title.isNotBlank() && amt != null && amt > 0) {
                    inventoryManager.addExpense(ExpenseRecord(System.currentTimeMillis().toString(), title, amt))
                    onDataChanged()
                    onDismiss()
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = AppTheme.subText(isDark)) } }
    )
}

@Composable
fun StoreSalesHistoryDialog(inventoryManager: InventoryManager, refreshTrigger: Int, isDark: Boolean, onDismiss: () -> Unit) {
    val salesHistory = remember(refreshTrigger) { inventoryManager.getSalesHistory() }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = AppTheme.card(isDark), shape = RoundedCornerShape(20.dp),
        title = { Text("سجل المبيعات 📅", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold) },
        text = {
            Box(modifier = Modifier.fillMaxWidth().height(350.dp)) {
                if (salesHistory.isEmpty()) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("لا توجد مبيعات.", color = AppTheme.subText(isDark)) } } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(salesHistory.reversed()) { _, record ->
                            Surface(color = AppTheme.inputBg(isDark), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, AppTheme.cardBorder(isDark))) {
                                Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(record.productName, color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("الكمية: ${record.quantity}", color = AppTheme.subText(isDark), fontSize = 11.sp)
                                    }
                                    Text(record.revenue.toDzd(), color = AppTheme.PremiumGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { PremiumButton(text = "إغلاق", modifier = Modifier.fillMaxWidth(), onClick = onDismiss) }
    )
}

@Composable
fun DailySmartInsightsDialog(inventoryManager: InventoryManager, isDark: Boolean, onDismiss: () -> Unit) {
    val sales = inventoryManager.getSalesHistory()
    val todaySales = sales.filter { (System.currentTimeMillis() - it.timestamp) <= 24 * 60 * 60 * 1000L }
    val todayRevenue = todaySales.sumOf { it.revenue }
    val todayProfit = todaySales.sumOf { it.profit }
    val lowStockCount = inventoryManager.getLowStockCount()

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = AppTheme.card(isDark), shape = RoundedCornerShape(20.dp),
        title = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Default.Lightbulb, null, tint = AppTheme.PremiumGold); Text("💡 الملخص اليومي", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 16.sp) } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ReportItemCard("مبيعات اليوم", todayRevenue.toDzd(), AppTheme.text(isDark), isDark)
                ReportItemCard("أرباح اليوم التقديرية", todayProfit.toDzd(), AppTheme.ColorSuccess, isDark)
                ReportItemCard("نواقص المخزون", "$lowStockCount منتجات", if(lowStockCount > 0) AppTheme.ColorDanger else AppTheme.ColorSuccess, isDark)
            }
        },
        confirmButton = { PremiumButton(text = "حسناً", modifier = Modifier.fillMaxWidth(), onClick = onDismiss) }
    )
}

@Composable
fun FinancialReportDialog(inventoryManager: InventoryManager, refreshTrigger: Int, isDark: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var selectedPeriod by remember { mutableStateOf("اليوم") }
    val currentTime = System.currentTimeMillis()
    val periodStartTime = remember(selectedPeriod) { when (selectedPeriod) { "اليوم" -> currentTime - (24L * 60 * 60 * 1000); "الأسبوع" -> currentTime - (7L * 24 * 60 * 60 * 1000); "الشهر" -> currentTime - (30L * 24 * 60 * 60 * 1000); else -> 0L } }
    val salesHistory = remember(refreshTrigger, selectedPeriod) { inventoryManager.getSalesHistory().filter { it.timestamp >= periodStartTime } }
    val expenseList = remember(refreshTrigger, selectedPeriod) { inventoryManager.getExpenseList().filter { it.timestamp >= periodStartTime } }
    val totalCash = salesHistory.sumOf { it.revenue }
    val netProfit = salesHistory.sumOf { it.profit } - expenseList.sumOf { it.amount }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let {
            val reportContent = """
                تاريخ التقرير: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}
                الفترة المحددة: $selectedPeriod
                
                إجمالي المبيعات المحصلة: ${totalCash.toDzd()}
                صافي الربح الفعلي: ${netProfit.toDzd()}
                
                -----------------------------------
                تم الإنشاء تلقائياً بواسطة تطبيق كاسبو - Kasebo POS
            """.trimIndent()
            PdfGenerator.generateReportPdf(context, it, "التقرير المالي", reportContent)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = AppTheme.card(isDark), shape = RoundedCornerShape(20.dp),
        title = { Text("التقرير المالي الشامل 📊", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("اليوم", "الأسبوع", "الشهر", "الكل").forEach { period -> DashboardFilterChip(period, selectedPeriod == period, isDark) { selectedPeriod = period } } }
                HorizontalDivider(color = AppTheme.inputBg(isDark))
                ReportItemCard("المبيعات المحصلة", totalCash.toDzd(), AppTheme.text(isDark), isDark)
                ReportItemCard("الربح الصافي", netProfit.toDzd(), if (netProfit >= 0) AppTheme.ColorSuccess else AppTheme.ColorDanger, isDark)
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PremiumButton(text = "PDF 📄", modifier = Modifier.weight(1f)) { pdfLauncher.launch("Kasebo_Report.pdf") }
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("إغلاق", color = AppTheme.subText(isDark)) }
            }
        }
    )
}

@Composable
fun ReportItemCard(title: String, value: String, color: Color, isDark: Boolean) {
    Surface(color = AppTheme.inputBg(isDark), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, AppTheme.cardBorder(isDark)), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(title, color = AppTheme.subText(isDark), fontSize = 11.sp); Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun DashboardFilterChip(text: String, isSelected: Boolean, isDark: Boolean, onClick: () -> Unit) {
    Surface(color = if (isSelected) AppTheme.PremiumGold else AppTheme.inputBg(isDark), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, if (isSelected) AppTheme.PremiumGold else AppTheme.cardBorder(isDark)), modifier = Modifier.clickable { onClick() }) {
        Text(text, color = if (isSelected) Color.White else AppTheme.subText(isDark), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}
