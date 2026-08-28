package com.example.bookstoremanager.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
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

@Composable
fun InventoryTab(inventoryManager: InventoryManager, refreshTrigger: Int, isDark: Boolean, onDataChanged: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)

    val isAdmin = prefs.getString("user_role", "Admin") == "Admin"

    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("الكل") }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showBulkAddDialog by remember { mutableStateOf(false) }
    var showCartDialog by remember { mutableStateOf(false) }
    var showQuickAuditDialog by remember { mutableStateOf(false) }
    var showSearchScanner by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToCart by remember { mutableStateOf<Product?>(null) }

    val cartSize = remember(refreshTrigger) { try { inventoryManager.getShoppingCart().size } catch (e: Exception) { 0 } }

    val allProducts = remember(refreshTrigger) { inventoryManager.getProductList().toList() }
    val availableCount = remember(allProducts) { allProducts.count { it.stockQuantity > 0 } }
    val outOfStockCount = remember(allProducts) { allProducts.count { it.stockQuantity <= 0 } }

    val filteredProducts = remember(refreshTrigger, searchQuery, filterMode) {
        allProducts.filter {
            (it.name.contains(searchQuery, ignoreCase = true) ||
                it.barcode.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)) &&
            when (filterMode) {
                "متوفر" -> it.stockQuantity > 0
                "نفاد المخزون" -> it.stockQuantity <= 0
                else -> true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {

            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("المنتجات والمخزون", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))

                if (isAdmin) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(onClick = { showQuickAuditDialog = true }, modifier = Modifier.size(36.dp).background(AppTheme.inputBg(isDark), RoundedCornerShape(10.dp))) {
                            Icon(Icons.Default.Build, contentDescription = "جرد سريع", tint = AppTheme.GoldPrimary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { showBulkAddDialog = true }, modifier = Modifier.size(36.dp).background(AppTheme.inputBg(isDark), RoundedCornerShape(10.dp))) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "كاتالوج جماعي", tint = AppTheme.GoldPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    PremiumTextField(value = searchQuery, onValueChange = { searchQuery = it }, label = "ابحث بالاسم أو الباركود...", isDark = isDark, icon = Icons.Default.Search)
                }
                IconButton(onClick = { showSearchScanner = true }, modifier = Modifier.size(52.dp).background(AppTheme.GoldPrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "مسح باركود وإضافة للسلة", tint = AppTheme.GoldPrimary, modifier = Modifier.size(24.dp))
                }
            }

            StatCard(title = "إجمالي المنتجات", value = "${allProducts.size}", icon = Icons.Default.Category, color = Color(0xFF3B82F6), isDark = isDark)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(title = "المنتجات المتوفرة", value = "$availableCount", icon = Icons.Default.CheckCircle, color = AppTheme.ColorSuccess, isDark = isDark, modifier = Modifier.weight(1f).clickable { filterMode = "متوفر" })
                StatCard(title = "غير متوفرة", value = "$outOfStockCount", icon = Icons.Default.Warning, color = AppTheme.ColorDanger, isDark = isDark, modifier = Modifier.weight(1f).clickable { filterMode = "نفاد المخزون" })
            }

            if (filterMode != "الكل") {
                TextButton(onClick = { filterMode = "الكل" }, modifier = Modifier.fillMaxWidth()) {
                    Text("إلغاء الفلتر (عرض الكل)", color = AppTheme.GoldPrimary, fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(Modifier.height(8.dp))
            }

            if (filteredProducts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("لا توجد منتجات.", color = AppTheme.subText(isDark), fontSize = 13.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize().weight(1f)) {
                    itemsIndexed(filteredProducts, key = { index, prod -> "${prod.id}_$index" }) { _, prod ->
                        ProductPremiumCard(
                            product = prod, isDark = isDark, isAdmin = isAdmin,
                            onAddToCart = { productToCart = prod },
                            onEdit = { productToEdit = prod },
                            onDelete = { try { inventoryManager.removeProduct(prod.id); onDataChanged() } catch (e: Exception) { Toast.makeText(context, "خطأ في الحذف", Toast.LENGTH_SHORT).show() } }
                        )
                    }
                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }

        Column(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (cartSize > 0) {
                ExtendedFloatingActionButton(onClick = { showCartDialog = true }, containerColor = AppTheme.ColorSuccess, contentColor = Color.White, shape = RoundedCornerShape(16.dp)) {
                    Text("🛒 السلة ($cartSize)", fontWeight = FontWeight.Bold)
                }
            }
            if (isAdmin) {
                FloatingActionButton(onClick = { showAddProductDialog = true }, containerColor = AppTheme.GoldPrimary, contentColor = AppTheme.GoldPrimaryText, shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة منتج")
                }
            }
        }
    }

    if (showSearchScanner) {
        BarcodeScannerDialog(
            onBarcodeScanned = { code -> showSearchScanner = false; inventoryManager.scanAndAddToCart(code) { success, msg -> Toast.makeText(context, msg, if(success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show(); if (success) { onDataChanged() } else { searchQuery = code } } },
            onDismiss = { showSearchScanner = false }
        )
    }

    if (showAddProductDialog) AddProductDialog(inventoryManager, isDark, { showAddProductDialog = false }, onDataChanged)
    if (showBulkAddDialog) BulkAddProductsDialog(inventoryManager, isDark, { showBulkAddDialog = false }, onDataChanged)
    if (showQuickAuditDialog) QuickStockAuditDialog(inventoryManager, isDark, { showQuickAuditDialog = false }, onDataChanged)

    if (productToEdit != null) { EditProductDialog(product = productToEdit!!, isDark = isDark, onDismiss = { productToEdit = null }, onSave = { updatedProduct -> inventoryManager.updateProduct(updatedProduct); productToEdit = null; onDataChanged() }) }

    if (productToCart != null) {
        val targetProduct = productToCart!!
        AddToCartDialog(targetProduct, isDark, { productToCart = null }) { qty ->
            if (inventoryManager.addToCart(targetProduct, qty)) { onDataChanged(); productToCart = null; Toast.makeText(context, "تمت الإضافة للسلة!", Toast.LENGTH_SHORT).show() } else { Toast.makeText(context, "الكمية المطلوبة غير متوفرة!", Toast.LENGTH_SHORT).show() }
        }
    }

    if (showCartDialog) {
        CartDialog(inventoryManager, isDark, onDataChanged, { showCartDialog = false }) { invoice ->
            showCartDialog = false; onDataChanged(); Toast.makeText(context, "تم تسجيل الدفع بنجاح!", Toast.LENGTH_SHORT).show()
            val sendIntent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, invoice); type = "text/plain" }
            context.startActivity(Intent.createChooser(sendIntent, "مشاركة الفاتورة"))
        }
    }
}

@Composable
fun ProductPremiumCard(product: Product, isDark: Boolean, isAdmin: Boolean, onAddToCart: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isAvailable = product.stockQuantity > 0

    Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AppTheme.cardBorder(isDark)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = product.name, color = AppTheme.text(isDark), fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Spacer(Modifier.width(6.dp))
                        Box(modifier = Modifier.size(8.dp).background(if (isAvailable) AppTheme.ColorSuccess else AppTheme.ColorDanger, CircleShape))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "الفئة: ${product.category}  |  الوحدة: ${product.unitType}", color = AppTheme.subText(isDark), fontSize = 11.sp)
                    if(product.barcode.isNotBlank()) { Text(text = "الرمز (Barcode): ${product.barcode}", color = AppTheme.subText(isDark), fontSize = 11.sp) }
                }
                Surface(color = AppTheme.inputBg(isDark), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Inventory2, contentDescription = null, tint = AppTheme.GoldPrimary, modifier = Modifier.size(24.dp)) }
                }
            }

            HorizontalDivider(color = AppTheme.inputBg(isDark).copy(alpha = 0.5f))

            if (isAdmin) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("الشراء: ${product.purchasePrice.toDzd()}", fontSize = 11.sp, color = AppTheme.subText(isDark))
                    val profit = product.sellingPrice - product.purchasePrice
                    Text("الربح: ${profit.toDzd()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (profit >= 0) Color(0xFF3B82F6) else AppTheme.ColorDanger)
                }
                HorizontalDivider(color = AppTheme.inputBg(isDark).copy(alpha = 0.5f))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isAdmin) {
                        ActionIconButton(Icons.Default.Delete, Color(0xFFEF4444)) { onDelete() }
                    }
                    ActionIconButton(Icons.Default.Edit, Color(0xFF3B82F6)) { onEdit() }
                    ActionIconButton(Icons.Default.ShoppingCart, AppTheme.GoldPrimary) { onAddToCart() }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = product.sellingPrice.toDzd(), color = AppTheme.text(isDark), fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Text(text = if(isAvailable) "المخزون ${product.stockQuantity.formatClean()}" else "نفد المخزون", color = if (isAvailable) AppTheme.ColorSuccess else AppTheme.ColorDanger, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, color: Color, isDark: Boolean, modifier: Modifier = Modifier) {
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, color.copy(alpha = 0.3f)), modifier = modifier) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
                Text(title, fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
            }
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun ActionIconButton(icon: ImageVector, color: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.12f)).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
    }
}

// ==========================================
// 🧩 قائمة منسدلة لاختيار الأعمدة (مكون مساعد)
// ==========================================
@Composable
fun CsvColumnSelector(label: String, headers: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, isDark: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = if (selectedIndex in headers.indices) headers[selectedIndex] else "تخطي (لا يوجد)"

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = AppTheme.subText(isDark), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.inputBg(isDark), RoundedCornerShape(10.dp))
            .border(1.dp, AppTheme.cardBorder(isDark), RoundedCornerShape(10.dp))
            .clickable { expanded = true }
            .padding(14.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(selectedText, color = AppTheme.text(isDark), fontSize = 13.sp)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = AppTheme.subText(isDark))
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(AppTheme.card(isDark))
            ) {
                DropdownMenuItem(
                    text = { Text("تخطي (لا يوجد)", color = AppTheme.text(isDark)) },
                    onClick = { onSelect(-1); expanded = false }
                )
                headers.forEachIndexed { index, header ->
                    val cleanHeader = if (header.isBlank()) "العمود رقم ${index + 1}" else header
                    DropdownMenuItem(
                        text = { Text(cleanHeader, color = AppTheme.text(isDark)) },
                        onClick = { onSelect(index); expanded = false }
                    )
                }
            }
        }
    }
}

// ==========================================
// 📥 نافذة إضافة / استيراد المنتجات المتقدمة
// ==========================================
@Composable
fun BulkAddProductsDialog(
    inventoryManager: InventoryManager,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    var isImporting by remember { mutableStateOf(false) }

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var csvHeaders by remember { mutableStateOf<List<String>>(emptyList()) }
    var showMappingDialog by remember { mutableStateOf(false) }

    var nameCol by remember { mutableIntStateOf(-1) }
    var barcodeCol by remember { mutableIntStateOf(-1) }
    var purchaseCol by remember { mutableIntStateOf(-1) }
    var sellCol by remember { mutableIntStateOf(-1) }
    var qtyCol by remember { mutableIntStateOf(-1) }
    var catCol by remember { mutableIntStateOf(-1) }

    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedUri = it
            csvHeaders = inventoryManager.getCsvHeaders(context, it)

            if (csvHeaders.isNotEmpty()) {
                nameCol = if (csvHeaders.size > 1) 1 else 0
                barcodeCol = if (csvHeaders.isNotEmpty()) 0 else -1
                purchaseCol = if (csvHeaders.size > 2) 2 else -1
                sellCol = if (csvHeaders.size > 3) 3 else -1
                qtyCol = if (csvHeaders.size > 4) 4 else -1
                catCol = if (csvHeaders.size > 5) 5 else -1

                showMappingDialog = true
            } else {
                Toast.makeText(context, "الملف فارغ أو صيغته غير مدعومة!", Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showMappingDialog && selectedUri != null) {
        AlertDialog(
            onDismissRequest = { showMappingDialog = false },
            containerColor = AppTheme.card(isDark),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("🔗 ربط أعمدة ملف الإكسيل", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("اختر العمود الذي يطابق كل حقل في متجرك (اضغط على الحقل للتغيير):", color = AppTheme.subText(isDark), fontSize = 13.sp)

                    CsvColumnSelector("اسم المنتج (إجباري)", csvHeaders, nameCol, { nameCol = it }, isDark)
                    CsvColumnSelector("الباركود", csvHeaders, barcodeCol, { barcodeCol = it }, isDark)
                    CsvColumnSelector("سعر الشراء", csvHeaders, purchaseCol, { purchaseCol = it }, isDark)
                    CsvColumnSelector("سعر البيع", csvHeaders, sellCol, { sellCol = it }, isDark)
                    CsvColumnSelector("الكمية المتوفرة", csvHeaders, qtyCol, { qtyCol = it }, isDark)
                    CsvColumnSelector("القسم / التصنيف", csvHeaders, catCol, { catCol = it }, isDark)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameCol == -1) {
                            Toast.makeText(context, "يجب تحديد عمود لاسم المنتج على الأقل!", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        showMappingDialog = false
                        isImporting = true

                        // ⚠️ هنا كان الخطأ: تم إصلاح الفواصل بدقة
                        inventoryManager.importProductsFromCsvMapped(
                            context = context,
                            uri = selectedUri!!,
                            nameIdx = nameCol,
                            barcodeIdx = barcodeCol,
                            purchaseIdx = purchaseCol,
                            sellIdx = sellCol,
                            qtyIdx = qtyCol,
                            catIdx = catCol,
                            hasHeaderRow = true,
                            onSuccess = { count ->
                                isImporting = false
                                Toast.makeText(context, "نجاح! تم استيراد $count منتج ✅", Toast.LENGTH_LONG).show()
                                onSave()
                                onDismiss()
                            },
                            onFailure = { errorMsg ->
                                isImporting = false
                                Toast.makeText(context, "خطأ: $errorMsg", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.GoldPrimary)
                ) {
                    Text("تأكيد وبدء الاستيراد 🚀", color = AppTheme.GoldPrimaryText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMappingDialog = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("إلغاء", color = AppTheme.subText(isDark))
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = AppTheme.card(isDark),
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.List, null, tint = AppTheme.GoldPrimary)
                    Text("استيراد المنتجات المتقدم 📋", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "هذه الميزة الذكية تتيح لك رفع أي ملف إكسيل (CSV) حتى لو كانت أعمدته غير مرتبة.\n\nالخطوات:\n1. اختر الملف من هاتفك.\n2. سيقرأ التطبيق ملفك.\n3. ستظهر لك نافذة لربط الأعمدة (مثلاً: تحدد أين السعر وأين الاسم بدقة).",
                        color = AppTheme.subText(isDark), fontSize = 12.sp, lineHeight = 20.sp
                    )

                    Button(
                        onClick = { if (!isImporting) csvPickerLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.ColorSuccess),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.UploadFile, contentDescription = "استيراد", tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("اختر ملف الإكسيل (CSV) 📥", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("إغلاق", color = AppTheme.subText(isDark), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
