package com.example.bookstoremanager.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bookstoremanager.*
import com.example.bookstoremanager.data.*
import org.json.JSONArray

@Composable
fun CustomerDebtsReportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)
    var customers by remember { mutableStateOf<List<CustomerAccount>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val jsonStr = prefs.getString("customers_data", "[]") ?: "[]"
            val array = JSONArray(jsonStr)
            val list = mutableListOf<CustomerAccount>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val customer = CustomerAccount(id = obj.optString("id", ""), name = obj.optString("name", ""), phone = obj.optString("phone", ""))
                val recordsArr = obj.optJSONArray("records") ?: JSONArray()
                for (j in 0 until recordsArr.length()) {
                    val rec = recordsArr.getJSONObject(j)
                    customer.records.add(DebtRecord(amount = rec.optDouble("amount", 0.0), isReceivable = rec.optBoolean("isReceivable", true)))
                }
                list.add(customer)
            }
            customers = list
        } catch (e: Exception) { }
    }

    val filteredCustomers = customers.filter { it.name.contains(searchQuery, ignoreCase = true) }
    val totalFacture = customers.sumOf { it.getTotalReceivable() }
    val totalVerse = customers.sumOf { it.getTotalPayable() }
    val resteAPayer = customers.sumOf { it.getNetBalance() }
    val crediteurs = customers.count { it.getNetBalance() < 0 }
    val debiteurs = customers.count { it.getNetBalance() > 0 }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E293B)).padding(16.dp).statusBarsPadding(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEF4444)).clickable { Toast.makeText(context, "الطباعة قيد التحضير", Toast.LENGTH_SHORT).show() }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Description, contentDescription = "PDF", tint = Color.White) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("وضعية العملاء", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp); Icon(Icons.Default.Group, null, tint = Color(0xFF38BDF8)) }
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White) }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SituationCard("العملاء", "${customers.size}", Icons.Default.Group, Color(0xFF3B82F6), Color(0xFFEFF6FF), Modifier.weight(1f))
                SituationCard("إجمالي الفواتير", "${totalFacture.formatClean()} دج", Icons.Default.Receipt, Color(0xFF8B5CF6), Color(0xFFF5F3FF), Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SituationCard("إجمالي المدفوع", "${totalVerse.formatClean()} دج", Icons.Default.AttachMoney, Color(0xFF10B981), Color(0xFFECFDF5), Modifier.weight(1f))
                SituationCard("الباقي للدفع", "${resteAPayer.formatClean()} دج", Icons.Default.Warning, Color(0xFFEF4444), Color(0xFFFEF2F2), Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SituationCard("دائنون", "$crediteurs", Icons.Default.Person, Color(0xFFF59E0B), Color(0xFFFFFBEB), Modifier.weight(1f))
                SituationCard("مدينون", "$debiteurs", Icons.Default.PersonOutline, Color(0xFFF43F5E), Color(0xFFFFF1F2), Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("البحث عن عميل...", color = Color.Gray) }, trailingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedBorderColor = Color(0xFFE2E8F0), unfocusedBorderColor = Color(0xFFE2E8F0)))
            Spacer(Modifier.height(8.dp))

            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E293B)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الاسم", color = Color.White, modifier = Modifier.weight(1.5f), fontSize = 12.sp)
                        Text("المبلغ", color = Color.White, modifier = Modifier.weight(1.5f), fontSize = 12.sp)
                        Text("المدفوع", color = Color.White, modifier = Modifier.weight(1.5f), fontSize = 12.sp)
                        Text("الباقي", color = Color.White, modifier = Modifier.weight(1.5f), fontSize = 12.sp)
                        Text("الحالة", color = Color.White, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, fontSize = 12.sp)
                    }
                    if (filteredCustomers.isEmpty()) { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("لا يوجد عملاء", color = Color.Gray) }
                    } else { filteredCustomers.forEach { customer ->
                            val montant = customer.getTotalReceivable(); val verse = customer.getTotalPayable(); val reste = customer.getNetBalance()
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(customer.name, color = Color.Black, modifier = Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(montant.formatClean(), color = Color.Black, modifier = Modifier.weight(1.5f), fontSize = 11.sp)
                                Text(verse.formatClean(), color = Color.Black, modifier = Modifier.weight(1.5f), fontSize = 11.sp)
                                Text(reste.formatClean(), color = Color.Black, modifier = Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Box(modifier = Modifier.weight(1.2f), contentAlignment = Alignment.Center) {
                                    if (reste > 0) { Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(6.dp)) { Text("غير مسددة", color = Color(0xFFEF4444), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) }
                                    } else { Surface(color = Color(0xFFD1FAE5), shape = RoundedCornerShape(6.dp)) { Text("مسددة", color = Color(0xFF10B981), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) } }
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        }
    }
}

data class SupplierReportItem(val id: String, val name: String, val totalAchete: Double, val totalVerse: Double, val reste: Double)

@Composable
fun SupplierDebtsReportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)
    var suppliers by remember { mutableStateOf<List<SupplierReportItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val jsonStr = prefs.getString("suppliers_data", "[]") ?: "[]"
            val array = JSONArray(jsonStr)
            val list = mutableListOf<SupplierReportItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.optString("name", "")
                val recordsArr = obj.optJSONArray("records") ?: JSONArray()
                var achete = 0.0; var verse = 0.0
                for (j in 0 until recordsArr.length()) {
                    val rec = recordsArr.getJSONObject(j)
                    val amount = rec.optDouble("amount", 0.0); val isPayment = rec.optBoolean("isPayment", false)
                    if (isPayment) verse += amount else achete += amount
                }
                list.add(SupplierReportItem(id = obj.optString("id", ""), name = name, totalAchete = achete, totalVerse = verse, reste = achete - verse))
            }
            suppliers = list
        } catch (e: Exception) { }
    }

    val filteredSuppliers = suppliers.filter { it.name.contains(searchQuery, ignoreCase = true) }
    val totalAcheteAll = suppliers.sumOf { it.totalAchete }; val totalVerseAll = suppliers.sumOf { it.totalVerse }; val resteAPayerAll = suppliers.sumOf { it.reste }
    val crediteurs = suppliers.count { it.reste > 0 }; val debiteurs = suppliers.count { it.reste < 0 }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E293B)).padding(16.dp).statusBarsPadding(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEF4444)).clickable { Toast.makeText(context, "الطباعة قيد التحضير", Toast.LENGTH_SHORT).show() }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Description, contentDescription = "PDF", tint = Color.White) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("وضعية الموردين", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp); Icon(Icons.Default.LocalShipping, null, tint = Color(0xFF38BDF8)) }
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White) }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SituationCard("الموردين", "${suppliers.size}", Icons.Default.LocalShipping, Color(0xFF3B82F6), Color(0xFFEFF6FF), Modifier.weight(1f))
                SituationCard("إجمالي المشتريات", "${totalAcheteAll.formatClean()} دج", Icons.Default.Receipt, Color(0xFF8B5CF6), Color(0xFFF5F3FF), Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SituationCard("إجمالي المدفوع", "${totalVerseAll.formatClean()} دج", Icons.Default.AttachMoney, Color(0xFF10B981), Color(0xFFECFDF5), Modifier.weight(1f))
                SituationCard("الباقي للدفع", "${resteAPayerAll.formatClean()} دج", Icons.Default.Warning, Color(0xFFEF4444), Color(0xFFFEF2F2), Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SituationCard("دائنون", "$crediteurs", Icons.Default.Domain, Color(0xFFF59E0B), Color(0xFFFFFBEB), Modifier.weight(1f))
                SituationCard("مدينون", "$debiteurs", Icons.Default.PersonOff, Color(0xFFF43F5E), Color(0xFFFFF1F2), Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("البحث عن مورد...", color = Color.Gray) }, trailingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedBorderColor = Color(0xFFE2E8F0), unfocusedBorderColor = Color(0xFFE2E8F0)))
            Spacer(Modifier.height(8.dp))

            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E293B)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الاسم", color = Color.White, modifier = Modifier.weight(1.5f), fontSize = 12.sp)
                        Text("المبلغ", color = Color.White, modifier = Modifier.weight(1.5f), fontSize = 12.sp)
                        Text("المدفوع", color = Color.White, modifier = Modifier.weight(1.5f), fontSize = 12.sp)
                        Text("الباقي", color = Color.White, modifier = Modifier.weight(1.5f), fontSize = 12.sp)
                        Text("الحالة", color = Color.White, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, fontSize = 12.sp)
                    }
                    if (filteredSuppliers.isEmpty()) { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("لا يوجد موردين", color = Color.Gray) }
                    } else { filteredSuppliers.forEach { supplier ->
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(supplier.name, color = Color.Black, modifier = Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(supplier.totalAchete.formatClean(), color = Color.Black, modifier = Modifier.weight(1.5f), fontSize = 11.sp)
                                Text(supplier.totalVerse.formatClean(), color = Color.Black, modifier = Modifier.weight(1.5f), fontSize = 11.sp)
                                Text(supplier.reste.formatClean(), color = Color.Black, modifier = Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Box(modifier = Modifier.weight(1.2f), contentAlignment = Alignment.Center) {
                                    if (supplier.reste > 0) { Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(6.dp)) { Text("غير مسددة", color = Color(0xFFEF4444), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) }
                                    } else { Surface(color = Color(0xFFD1FAE5), shape = RoundedCornerShape(6.dp)) { Text("مسددة", color = Color(0xFF10B981), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) } }
                                }
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
fun SituationCard(title: String, value: String, icon: ImageVector, color: Color, bgColor: Color, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = bgColor), shape = RoundedCornerShape(12.dp), modifier = modifier.height(90.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Surface(color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(32.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(18.dp)) } }
                Text(value, fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color.Black)
            }
            Text(title, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.align(Alignment.End), fontWeight = FontWeight.Bold)
        }
    }
}
