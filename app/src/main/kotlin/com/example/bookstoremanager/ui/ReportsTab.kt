package com.example.bookstoremanager.ui

import com.example.bookstoremanager.*
import com.example.bookstoremanager.data.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReportsTab(
    inventoryManager: InventoryManager,
    suppliersManager: SuppliersManager? = null,
    employeesManager: EmployeesManager? = null,
    isDark: Boolean,
    onBack: () -> Unit
) {
    val totalCash = inventoryManager.getTotalCashCollected()
    val totalExp = inventoryManager.getTotalExpenses()
    val netProfit = inventoryManager.getNetRealProfit()
    val inventoryCost = inventoryManager.getTotalInventoryCost()
    val totalReceivable = inventoryManager.getTotalStoreReceivable()
    val totalPayable = inventoryManager.getTotalStorePayable()

    val productCount = inventoryManager.getProductList().size
    val customerCount = inventoryManager.getCustomerList().size
    val supplierCount = suppliersManager?.getSuppliersList()?.size ?: 0
    val employeeCount = employeesManager?.getEmployeeList()?.size ?: 0
    val salesHistorySize = inventoryManager.getSalesHistory().size

    Column(
        modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // رأس الصفحة وزر الرجوع
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "عودة", tint = AppTheme.GoldPrimary)
                }
                Text("📊 التقارير التفصيلية الشاملة", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
            }
        }

        // بطاقة الأداء المالي العام
        Card(
            colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("الأداء المالي العام", color = AppTheme.GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                HorizontalDivider(color = AppTheme.inputBg(isDark))

                ReportMetricRow("إجمالي المبيعات المحصلة", "$totalCash دج", AppTheme.text(isDark), isDark)
                ReportMetricRow("صافي الربح الفعلي", "$netProfit دج", AppTheme.ColorSuccess, isDark)
                ReportMetricRow("إجمالي المصروفات", "$totalExp دج", AppTheme.ColorDanger, isDark)
                ReportMetricRow("قيمة رأس مال المخزون", "$inventoryCost دج", AppTheme.GoldPrimary, isDark)
            }
        }

        // بطاقة حالة الديون والأرصدة
        Card(
            colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("حالة الديون والأرصدة", color = AppTheme.GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                HorizontalDivider(color = AppTheme.inputBg(isDark))

                ReportMetricRow("إجمالي ديون لك (عند الزبائن)", "$totalReceivable دج", AppTheme.ColorSuccess, isDark)
                ReportMetricRow("إجمالي ديون عليك (للموردين)", "$totalPayable دج", AppTheme.ColorDanger, isDark)
            }
        }

        // بطاقة إحصائيات ونشاط المتجر
        Card(
            colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("إحصائيات ونشاط المتجر", color = AppTheme.GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                HorizontalDivider(color = AppTheme.inputBg(isDark))

                ReportMetricRow("عدد المنتجات بالمخزون", "$productCount منتج", AppTheme.text(isDark), isDark)
                ReportMetricRow("عدد عمليات البيع المسجلة", "$salesHistorySize عملية", AppTheme.text(isDark), isDark)
                ReportMetricRow("عدد الزبائن", "$customerCount عميل", AppTheme.text(isDark), isDark)
                ReportMetricRow("عدد الموردين المسجلين", "$supplierCount مورد", AppTheme.text(isDark), isDark)
                ReportMetricRow("عدد الموظفين المسجلين", "$employeeCount موظف", AppTheme.text(isDark), isDark)
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun ReportMetricRow(title: String, value: String, valueColor: Color, isDark: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = AppTheme.subText(isDark), fontSize = 12.sp)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
