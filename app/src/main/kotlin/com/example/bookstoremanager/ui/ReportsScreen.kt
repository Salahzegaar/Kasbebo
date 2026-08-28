package com.example.bookstoremanager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bookstoremanager.AppTheme
import com.example.bookstoremanager.data.*

@Composable
fun ReportsTab(
    inventoryManager: InventoryManager,
    suppliersManager: SuppliersManager,
    employeesManager: EmployeesManager,
    isDark: Boolean,
    onBack: () -> Unit
) {
    var currentReportScreen by remember { mutableStateOf("menu") }

    when (currentReportScreen) {
        "menu" -> ReportsMenuScreen(isDark, onBack = onBack, onNavigate = { currentReportScreen = it })

        // 💰 التقارير المالية
        "analytics" -> AdvancedAnalyticsScreen(inventoryManager, isDark, onBack = { currentReportScreen = "menu" })
        "taxes" -> TaxesReportScreen(inventoryManager, isDark, onBack = { currentReportScreen = "menu" })

        // 📦 تقارير المخزون
        "products_list" -> ProductsListReportScreen(inventoryManager, isDark, onBack = { currentReportScreen = "menu" })
        "stock_movement" -> StockMovementReportScreen(inventoryManager, onBack = { currentReportScreen = "menu" })
        "audit" -> AuditReportScreen(inventoryManager, onBack = { currentReportScreen = "menu" })
        "stagnant_products" -> StagnantProductsReportScreen(inventoryManager, isDark, onBack = { currentReportScreen = "menu" })
        "stock_alerts" -> StockAlertsReportScreen(inventoryManager, isDark, onBack = { currentReportScreen = "menu" })

        // 👥 تقارير الديون
        "customer_debts" -> CustomerDebtsReportScreen(onBack = { currentReportScreen = "menu" })
        "supplier_debts" -> SupplierDebtsReportScreen(onBack = { currentReportScreen = "menu" })

        else -> GenericEmptyReportScreen("قيد التطوير", Icons.Default.Build, AppTheme.GoldPrimary, isDark, onBack = { currentReportScreen = "menu" })
    }
}

@Composable
fun ReportsMenuScreen(isDark: Boolean, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(AppTheme.bg(isDark)).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "عودة", tint = AppTheme.text(isDark)) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("التقارير", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
                Icon(Icons.Default.Article, contentDescription = null, tint = AppTheme.GoldPrimary)
            }
        }

        ReportMenuCard("التحليل المالي والأرباح", Icons.Default.TrendingUp, Color(0xFFF43F5E), isDark) { onNavigate("analytics") }
        ReportMenuCard("قائمة المنتجات في المخزون", Icons.Default.Inventory2, Color(0xFFD946EF), isDark) { onNavigate("products_list") }
        ReportMenuCard("وضعية ديون العملاء", Icons.Default.Group, Color(0xFF64748B), isDark) { onNavigate("customer_debts") }
        ReportMenuCard("وضعية ديون الموردين", Icons.Default.LocalShipping, Color(0xFF0EA5E9), isDark) { onNavigate("supplier_debts") }
        ReportMenuCard("حركة المخزون", Icons.Default.SyncAlt, Color(0xFF6366F1), isDark) { onNavigate("stock_movement") }
        ReportMenuCard("الجرد", Icons.Default.FactCheck, Color(0xFF10B981), isDark) { onNavigate("audit") }
        ReportMenuCard("الجباية", Icons.Default.AccountBalance, Color(0xFF06B6D4), isDark) { onNavigate("taxes") }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth().padding(end = 8.dp)) {
            Text("التنبيهات", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Notifications, null, tint = AppTheme.text(isDark))
        }

        ReportMenuCard("المنتجات الراكدة", Icons.Default.Inventory, Color(0xFFF97316), isDark) { onNavigate("stagnant_products") }
        ReportMenuCard("تنبيه المخزون", Icons.Default.Warning, Color(0xFFF59E0B), isDark) { onNavigate("stock_alerts") }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun ReportMenuCard(title: String, icon: ImageVector, color: Color, isDark: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().height(80.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, AppTheme.inputBg(isDark))) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(color.copy(alpha = 0.7f)))
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark), fontSize = 16.sp)
                    Spacer(Modifier.width(16.dp))
                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp)) }
                }
            }
        }
    }
}

@Composable
fun GenericEmptyReportScreen(title: String, icon: ImageVector, color: Color, isDark: Boolean, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(AppTheme.bg(isDark)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "عودة", tint = AppTheme.text(isDark)) }
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(color = color.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.size(100.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(50.dp)) }
                }
                Text("لا توجد بيانات حالياً", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = color)) { Text("العودة للقائمة", color = Color.White) }
            }
        }
    }
}
