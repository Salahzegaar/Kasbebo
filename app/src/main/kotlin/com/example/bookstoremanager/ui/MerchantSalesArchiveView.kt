package com.example.bookstoremanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bookstoremanager.AppTheme
import com.example.bookstoremanager.data.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MerchantSalesArchiveView(
    inventoryManager: InventoryManager,
    isDark: Boolean,
    onBack: () -> Unit
) {
    // جلب المبيعات وعكس القائمة (لإظهار أحدث المبيعات في الأعلى) مع مراقبة الحجم لتحديث الواجهة فوراً
    val salesList = remember(inventoryManager.getSalesHistory().size) {
        inventoryManager.getSalesHistory().toList().reversed()
    }
    val totalRevenue = salesList.sumOf { it.revenue }
    val totalProfit = salesList.sumOf { it.profit }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.bg(isDark))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // 💎 رأس الصفحة الفاخر (Header)
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "رجوع", tint = AppTheme.text(isDark))
                }
                Text(
                    text = "أرشيف المبيعات",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppTheme.text(isDark)
                )
            }
            // شارة عدد العمليات بتصميم زجاجي
            Surface(
                color = AppTheme.PremiumGold.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "${salesList.size} عملية",
                    color = AppTheme.PremiumGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        // ==========================================
        // 💎 بطاقة الملخص المالي (Hero Card)
        // ==========================================
        PremiumCard(isDark = isDark) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("إجمالي المبيعات", color = AppTheme.subText(isDark), fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(totalRevenue.toDzd(), color = AppTheme.PremiumGold, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
                
                VerticalDivider(modifier = Modifier.height(40.dp), color = AppTheme.cardBorder(isDark))
                
                Column(horizontalAlignment = Alignment.End) {
                    Text("إجمالي الأرباح", color = AppTheme.subText(isDark), fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(totalProfit.toDzd(), color = AppTheme.ColorSuccess, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }
        }

        // ==========================================
        // 💎 قائمة الفواتير والمبيعات
        // ==========================================
        if (salesList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Receipt, null, tint = AppTheme.subText(isDark).copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("لا توجد فواتير أو مبيعات مسجلة بعد", color = AppTheme.subText(isDark), fontSize = 15.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(bottom = 90.dp) // لعدم التداخل مع الشريط السفلي
            ) {
                items(salesList) { sale ->
                    PremiumSaleInvoiceCard(sale = sale, isDark = isDark)
                }
            }
        }
    }
}

// ==========================================
// 💎 تصميم بطاقة الفاتورة المستقلة
// ==========================================
@Composable
fun PremiumSaleInvoiceCard(sale: SaleRecord, isDark: Boolean) {
    val formattedDate = remember(sale.timestamp) {
        val sdf = SimpleDateFormat("yyyy-MM-dd | HH:mm", Locale.getDefault())
        sdf.format(Date(sale.timestamp))
    }

    PremiumCard(isDark = isDark) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = sale.productName,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.text(isDark),
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "الكمية: ${sale.quantity.formatClean()}",
                    color = AppTheme.subText(isDark),
                    fontSize = 13.sp
                )
                Text(
                    text = formattedDate,
                    color = AppTheme.subText(isDark),
                    fontSize = 11.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = sale.revenue.toDzd(),
                    color = AppTheme.PremiumGold,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "الربح: +${sale.profit.toDzd()}",
                    color = AppTheme.ColorSuccess,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
