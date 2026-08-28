package com.example.bookstoremanager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bookstoremanager.*
import com.example.bookstoremanager.data.*

@Composable
fun AdvancedAnalyticsScreen(inventoryManager: InventoryManager, isDark: Boolean, onBack: () -> Unit) {
    val totalSales = inventoryManager.getTotalCashCollected()
    val totalProfit = inventoryManager.getNetRealProfit()
    val allProducts = inventoryManager.getProductList()
    val monthlySalesEst = totalSales * 15.5
    val monthlyProfitEst = totalProfit * 15.5
    val topSellingProducts = allProducts.filter { it.stockQuantity < 10 }.sortedBy { it.stockQuantity }.take(5)

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.bg(isDark)).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "عودة", tint = AppTheme.text(isDark)) }
            Text("التحليل المالي 📊", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AnalyticsDashboardCard("مبيعات اليوم", totalSales, Color(0xFF10B981), isDark, Modifier.weight(1f))
            AnalyticsDashboardCard("أرباح اليوم", totalProfit, AppTheme.GoldPrimary, isDark, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AnalyticsDashboardCard("مبيعات الشهر", monthlySalesEst, Color(0xFF3B82F6), isDark, Modifier.weight(1f))
            AnalyticsDashboardCard("أرباح الشهر", monthlyProfitEst, Color(0xFF8B5CF6), isDark, Modifier.weight(1f))
        }

        Spacer(Modifier.height(8.dp))
        Text("المنتجات الأكثر مبيعاً (Top 5)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))

        if (topSellingProducts.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), modifier = Modifier.fillMaxWidth().height(100.dp)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("لا توجد بيانات كافية.", color = AppTheme.subText(isDark)) }
            }
        } else {
            topSellingProducts.forEachIndexed { index, product ->
                Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), border = BorderStroke(1.dp, AppTheme.inputBg(isDark)), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Surface(color = AppTheme.GoldPrimary.copy(alpha = 0.2f), shape = CircleShape, modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text("#${index + 1}", color = AppTheme.GoldPrimary, fontWeight = FontWeight.Bold) }
                        }
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(product.name, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark), fontSize = 14.sp)
                            Text("السعر: ${product.sellingPrice.formatClean()} د.ج", color = AppTheme.subText(isDark), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun AnalyticsDashboardCard(title: String, value: Double, color: Color, isDark: Boolean, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, color.copy(alpha = 0.3f)), modifier = modifier.height(100.dp)) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 13.sp, color = AppTheme.text(isDark), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("${value.formatClean()} دج", fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun TaxesReportScreen(inventoryManager: InventoryManager, isDark: Boolean, onBack: () -> Unit) {
    val totalSales = inventoryManager.getTotalCashCollected()
    val estimatedTVA = totalSales * 0.19

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.bg(isDark)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "عودة", tint = AppTheme.text(isDark)) }
            Text("تقرير الجباية (TVA) 🏦", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
        }

        Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("إجمالي المبيعات (TTC):", color = AppTheme.text(isDark))
                    Text(totalSales.toDzd(), fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
                }
                HorizontalDivider(color = AppTheme.inputBg(isDark))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("المبيعات الصافية (HT):", color = AppTheme.text(isDark))
                    Text((totalSales - estimatedTVA).toDzd(), fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
                }
                HorizontalDivider(color = AppTheme.inputBg(isDark))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("الضريبة المقدرة (19%):", color = Color(0xFF06B6D4), fontWeight = FontWeight.Bold)
                    Text(estimatedTVA.toDzd(), fontWeight = FontWeight.Black, color = Color(0xFF06B6D4), fontSize = 18.sp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("ملاحظة: هذه الأرقام تقديرية.", color = AppTheme.subText(isDark), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}