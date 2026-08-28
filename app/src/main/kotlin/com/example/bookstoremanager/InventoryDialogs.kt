package com.example.bookstoremanager

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

import com.example.bookstoremanager.data.*
import com.example.bookstoremanager.ui.*

class ProductRowState {
    var name by mutableStateOf("")
    var barcode by mutableStateOf("")
    var purchasePrice by mutableStateOf("")
    var sellingPrice by mutableStateOf("")
    var quantity by mutableStateOf("1")
    var category by mutableStateOf("")
    var unitType by mutableStateOf("")
    var imageUrl by mutableStateOf("")
}

@Composable
fun SafeDropdownSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    isDark: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedOption, onValueChange = {}, readOnly = true,
            label = { Text(label, color = AppTheme.subText(isDark)) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = AppTheme.subText(isDark)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AppTheme.inputBg(isDark),
                unfocusedContainerColor = AppTheme.inputBg(isDark),
                focusedTextColor = AppTheme.text(isDark),
                unfocusedTextColor = AppTheme.text(isDark),
                focusedBorderColor = AppTheme.GoldPrimary,
                unfocusedBorderColor = AppTheme.cardBorder(isDark)
            ),
            shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()
        )
        Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(AppTheme.card(isDark))) {
            if (options.isEmpty()) {
                DropdownMenuItem(text = { Text("يرجى الإضافة من الإعدادات", color = AppTheme.subText(isDark)) }, onClick = { expanded = false })
            } else {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option, color = AppTheme.text(isDark), fontWeight = FontWeight.Bold) }, onClick = { onOptionSelected(option); expanded = false })
                }
            }
        }
    }
}

@Composable
fun QuickStockAuditDialog(
    inventoryManager: InventoryManager,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onDataChanged: () -> Unit
) {
    val prefs = LocalContext.current.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)
    val isAdmin = prefs.getString("user_role", "Admin") == "Admin"

    val productList = remember { inventoryManager.getProductList() }
    var searchQuery by remember { mutableStateOf("") }
    val filteredList = remember(searchQuery, productList) {
        if (searchQuery.isBlank()) productList
        else productList.filter { it.name.contains(searchQuery, ignoreCase = true) || it.barcode.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = AppTheme.card(isDark), shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Build, null, tint = AppTheme.GoldPrimary)
                Text("⚡ وضع الجرد السريع", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().height(420.dp)) {
                PremiumTextField(value = searchQuery, onValueChange = { searchQuery = it }, label = "بحث...", isDark = isDark, icon = Icons.Default.Search)
                if (filteredList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text("لا يوجد منتجات مطابقة", color = AppTheme.subText(isDark)) }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                        itemsIndexed(filteredList) { _, prod ->
                            var qtyInput by remember { mutableStateOf(prod.stockQuantity.formatClean()) }
                            Surface(color = AppTheme.inputBg(isDark), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, AppTheme.cardBorder(isDark)), modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prod.name, color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("الوحدة: ${prod.unitType} | السعر: ${prod.sellingPrice.toDzd()}", color = AppTheme.subText(isDark), fontSize = 10.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (isAdmin) {
                                            OutlinedButton(
                                                onClick = {
                                                    val current = qtyInput.toDoubleOrNull() ?: prod.stockQuantity
                                                    if (current > 0) {
                                                        qtyInput = (current - 1).coerceAtLeast(0.0).formatClean()
                                                        prod.stockQuantity = qtyInput.toDoubleOrNull() ?: 0.0
                                                        inventoryManager.updateProduct(prod)
                                                        onDataChanged()
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) { Text("-", fontWeight = FontWeight.Bold, color = AppTheme.text(isDark)) }
                                        }
                                        Text(text = qtyInput, color = AppTheme.GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.widthIn(min = 35.dp))
                                        if (isAdmin) {
                                            OutlinedButton(
                                                onClick = {
                                                    val current = qtyInput.toDoubleOrNull() ?: prod.stockQuantity
                                                    qtyInput = (current + 1).formatClean()
                                                    prod.stockQuantity = qtyInput.toDoubleOrNull() ?: 0.0
                                                    inventoryManager.updateProduct(prod)
                                                    onDataChanged()
                                                },
                                                modifier = Modifier.size(32.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) { Text("+", fontWeight = FontWeight.Bold, color = AppTheme.text(isDark)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { PrimaryAppButton(text = "إغلاق", modifier = Modifier.fillMaxWidth(), onClick = onDismiss) }
    )
}

@Composable
fun BulkAddProductsDialog(
    inventoryManager: InventoryManager,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)

    val categoriesList = remember { prefs.getString("store_categories", "عام")?.split(",")?.filter { it.isNotBlank() } ?: listOf("عام") }
    val unitsList = remember { prefs.getString("store_units", "قطعة")?.split(",")?.filter { it.isNotBlank() } ?: listOf("قطعة") }
    val productRows = remember { mutableStateListOf(ProductRowState().apply { category = categoriesList.firstOrNull() ?: "عام"; unitType = unitsList.firstOrNull() ?: "قطعة" }) }
    var activeRowIndexForImage by remember { mutableStateOf<Int?>(null) }

    val rowImagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            activeRowIndexForImage?.let { index ->
                if (index in productRows.indices) {
                    productRows[index].imageUrl = uriToBase64(context, it)
                    Toast.makeText(context, "تم إرفاق الصورة 🖼️", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = AppTheme.card(isDark),
        unfocusedContainerColor = AppTheme.card(isDark),
        focusedTextColor = AppTheme.text(isDark),
        unfocusedTextColor = AppTheme.text(isDark),
        focusedBorderColor = AppTheme.GoldPrimary,
        unfocusedBorderColor = AppTheme.cardBorder(isDark)
    )

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = AppTheme.card(isDark), shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.List, null, tint = AppTheme.GoldPrimary)
                Text("الكاتالوج الجماعي 📋", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().height(500.dp)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
                    itemsIndexed(productRows) { index, row ->
                        Card(colors = CardDefaults.cardColors(containerColor = AppTheme.inputBg(isDark)), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, AppTheme.cardBorder(isDark)), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("منتج رقم ${index + 1}", color = AppTheme.GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(onClick = { activeRowIndexForImage = index; rowImagePickerLauncher.launch("image/*") }) {
                                            Text(if (row.imageUrl.isBlank()) "📷 صورة" else "✅ تم", fontSize = 11.sp, color = AppTheme.GoldPrimary)
                                        }
                                        if (productRows.size > 1) {
                                            IconButton(onClick = { productRows.removeAt(index) }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Delete, null, tint = AppTheme.ColorDanger, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                                OutlinedTextField(value = row.name, onValueChange = { row.name = it }, placeholder = { Text("اسم المنتج", color = AppTheme.subText(isDark), fontSize = 12.sp) }, modifier = Modifier.fillMaxWidth().height(50.dp), singleLine = true, shape = RoundedCornerShape(8.dp), colors = inputColors)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = row.purchasePrice, onValueChange = { row.purchasePrice = it }, placeholder = { Text("سعر الشراء") }, modifier = Modifier.weight(1f).height(50.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(8.dp), colors = inputColors)
                                    OutlinedTextField(value = row.sellingPrice, onValueChange = { row.sellingPrice = it }, placeholder = { Text("سعر البيع") }, modifier = Modifier.weight(1f).height(50.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(8.dp), colors = inputColors)
                                    OutlinedTextField(value = row.quantity, onValueChange = { row.quantity = it }, placeholder = { Text("الكمية") }, modifier = Modifier.weight(0.8f).height(50.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(8.dp), colors = inputColors)
                                }
                            }
                        }
                    }
                }
                SecondaryAppButton(text = "➕ سطر جديد", isDark = isDark, modifier = Modifier.fillMaxWidth()) {
                    productRows.add(ProductRowState().apply { category = categoriesList.firstOrNull() ?: "عام"; unitType = unitsList.firstOrNull() ?: "قطعة" })
                }
            }
        },
        confirmButton = {
            PrimaryAppButton(text = "حفظ وإضافة للمخزون", modifier = Modifier.fillMaxWidth()) {
                val productsToAdd = mutableListOf<Product>()
                var successCount = 0
                for ((_, row) in productRows.withIndex()) {
                    if (row.name.isBlank()) continue
                    val purchase = row.purchasePrice.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val sell = row.sellingPrice.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val qty = row.quantity.replace(",", ".").toDoubleOrNull() ?: 1.0
                    if (sell > 0) {
                        productsToAdd.add(
                            Product(
                                id = UUID.randomUUID().toString(),
                                name = row.name.trim(),
                                barcode = row.barcode.trim(),
                                purchasePrice = purchase,
                                sellingPrice = sell,
                                stockQuantity = qty,
                                category = row.category.ifBlank { "عام" },
                                unitType = row.unitType.ifBlank { "قطعة" },
                                imageUrl = row.imageUrl
                            )
                        )
                        successCount++
                    }
                }
                if (productsToAdd.isNotEmpty()) {
                    inventoryManager.addProducts(productsToAdd)
                    Toast.makeText(context, "تمت إضافة $successCount منتجات بنجاح!", Toast.LENGTH_SHORT).show()
                    onSave()
                    onDismiss()
                } else {
                    Toast.makeText(context, "يرجى إدخال اسم وسعر بيع صالح!", Toast.LENGTH_SHORT).show()
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = AppTheme.subText(isDark)) } }
    )
}

@Composable
fun CartDialog(
    inventoryManager: InventoryManager,
    isDark: Boolean,
    onDataChanged: () -> Unit,
    onDismiss: () -> Unit,
    onCheckout: (String) -> Unit
) {
    val context = LocalContext.current
    var cartItems by remember { mutableStateOf(inventoryManager.getShoppingCart().toList()) }
    val total = cartItems.sumOf { it.getTotalPrice() }
    var customerPhoneInput by remember { mutableStateOf("") }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = AppTheme.text(isDark),
        unfocusedTextColor = AppTheme.text(isDark),
        focusedContainerColor = AppTheme.inputBg(isDark),
        unfocusedContainerColor = AppTheme.inputBg(isDark),
        focusedBorderColor = AppTheme.GoldPrimary,
        unfocusedBorderColor = AppTheme.cardBorder(isDark)
    )

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = AppTheme.card(isDark), shape = RoundedCornerShape(20.dp),
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("🛒 سلة المبيعات", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = AppTheme.subText(isDark)) }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                if (cartItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { Text("السلة فارغة حالياً", color = AppTheme.subText(isDark), fontSize = 13.sp) }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(cartItems) { _, item ->
                            Surface(color = AppTheme.inputBg(isDark), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, AppTheme.cardBorder(isDark)), modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.product.name, color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${item.product.sellingPrice.toDzd()} × ${item.quantity.formatClean()} ${item.product.unitType}", color = AppTheme.subText(isDark), fontSize = 11.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(item.getTotalPrice().toDzd(), color = AppTheme.GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Box(
                                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(AppTheme.ColorDanger.copy(alpha = 0.12f)).clickable {
                                                inventoryManager.removeFromCart(item.product.id)
                                                cartItems = inventoryManager.getShoppingCart().toList()
                                                onDataChanged()
                                            },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Delete, null, tint = AppTheme.ColorDanger, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = AppTheme.inputBg(isDark))
                OutlinedTextField(value = customerPhoneInput, onValueChange = { customerPhoneInput = it }, label = { Text("اسم / رقم هاتف الزبون", fontSize = 11.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = fieldColors)

                Card(colors = CardDefaults.cardColors(containerColor = AppTheme.inputBg(isDark)), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, AppTheme.cardBorder(isDark)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("الإجمالي المطلوب:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppTheme.text(isDark))
                            Text(total.toDzd(), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = AppTheme.GoldPrimary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (cartItems.isEmpty()) return@Button
                        val invoice = inventoryManager.checkoutCart()
                        if (invoice.isNotBlank()) { onCheckout(invoice); onDataChanged() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.GoldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("إصدار الفاتورة وتفريغ السلة", color = AppTheme.GoldPrimaryText, fontWeight = FontWeight.Bold) }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (cartItems.isEmpty()) return@OutlinedButton
                            val invoice = inventoryManager.checkoutCart()
                            if (invoice.isNotBlank()) {
                                shareInvoiceToWhatsApp(context, invoice, customerPhoneInput)
                                onCheckout(invoice)
                                onDataChanged()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AppTheme.ColorSuccess),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) { Text("💬 واتساب", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppTheme.ColorSuccess) }

                    OutlinedButton(
                        onClick = {
                            if (cartItems.isEmpty()) return@OutlinedButton
                            PdfInvoiceGenerator.generateAndSavePdf(context, cartItems, total, 0.0, customerPhoneInput)
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AppTheme.cardBorder(isDark)),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) { Text("📄 حفظ PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark)) }
                }
            }
        },
        dismissButton = null
    )
}

@Composable
fun AddProductDialog(
    inventoryManager: InventoryManager,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onDataChanged: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)
    val isAdmin = prefs.getString("user_role", "Admin") == "Admin"

    val categoriesList = remember { prefs.getString("store_categories", "عام")?.split(",")?.filter { it.isNotBlank() } ?: listOf("عام") }
    val unitsList = remember { prefs.getString("store_units", "قطعة")?.split(",")?.filter { it.isNotBlank() } ?: listOf("قطعة") }

    var name by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(categoriesList.firstOrNull() ?: "عام") }
    var selectedUnit by remember { mutableStateOf(unitsList.firstOrNull() ?: "قطعة") }
    var selectedImageUri by remember { mutableStateOf("") }

    // 📸 حالة للتحكم في ظهور نافذة ماسح الباركود
    var showScanner by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = AppTheme.text(isDark),
        unfocusedTextColor = AppTheme.text(isDark),
        focusedContainerColor = AppTheme.inputBg(isDark),
        unfocusedContainerColor = AppTheme.inputBg(isDark),
        focusedBorderColor = AppTheme.GoldPrimary,
        unfocusedBorderColor = AppTheme.cardBorder(isDark),
        focusedLabelColor = AppTheme.GoldPrimary,
        unfocusedLabelColor = AppTheme.subText(isDark)
    )

    AlertDialog(
        onDismissRequest = onDismiss, 
        containerColor = AppTheme.card(isDark), 
        shape = RoundedCornerShape(20.dp),
        title = { Text("إضافة منتج جديد 📦", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم المنتج") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = fieldColors)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isAdmin) {
                        OutlinedTextField(value = purchasePrice, onValueChange = { purchasePrice = it }, label = { Text("سعر الشراء") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = fieldColors)
                    }
                    OutlinedTextField(value = sellingPrice, onValueChange = { sellingPrice = it }, label = { Text("سعر البيع") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = fieldColors)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) { SafeDropdownSelector("القسم", categoriesList, category, { category = it }, isDark) }
                    Box(modifier = Modifier.weight(1f)) { SafeDropdownSelector("الوحدة", unitsList, selectedUnit, { selectedUnit = it }, isDark) }
                }
                OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("الكمية") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = fieldColors)
                
                // 📸 حقل الباركود المحدث مع زر الكاميرا
                OutlinedTextField(
                    value = barcode, 
                    onValueChange = { barcode = it }, 
                    label = { Text("الباركود") }, 
                    trailingIcon = {
                        IconButton(onClick = { showScanner = true }) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "مسح الباركود",
                                tint = AppTheme.GoldPrimary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(10.dp), 
                    colors = fieldColors
                )
            }
        },
        confirmButton = {
            PrimaryAppButton(text = "حفظ المنتج", modifier = Modifier.fillMaxWidth()) {
                val pPrice = purchasePrice.replace(",", ".").toDoubleOrNull() ?: 0.0
                val sPrice = sellingPrice.replace(",", ".").toDoubleOrNull() ?: 0.0
                val qty = quantity.replace(",", ".").toDoubleOrNull() ?: 0.0
                if (name.isNotBlank() && sPrice > 0) {
                    val newProduct = Product(
                        id = System.currentTimeMillis().toString(),
                        name = name.trim(),
                        barcode = barcode.trim(),
                        purchasePrice = pPrice,
                        sellingPrice = sPrice,
                        stockQuantity = qty,
                        category = category,
                        unitType = selectedUnit,
                        imageUrl = selectedImageUri
                    )
                    inventoryManager.addProduct(newProduct)
                    onDataChanged()
                    onDismiss()
                    Toast.makeText(context, "تمت إضافة المنتج بنجاح!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "الرجاء إدخال اسم وسعر بيع صحيحين", Toast.LENGTH_SHORT).show()
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = AppTheme.subText(isDark)) } }
    )

    // 📸 استدعاء نافذة الماسح عند الضغط على الزر
    if (showScanner) {
        BarcodeScannerDialog(
            onBarcodeScanned = { scannedCode ->
                barcode = scannedCode
                showScanner = false
            },
            onDismiss = { showScanner = false }
        )
    }
}

// ==========================================
// ✏️ نافذة تعديل بيانات المنتج (نسخة نظيفة ومستقرة)
// ==========================================
@Composable
fun EditProductDialog(
    product: Product,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    // 👈 السطر السحري الذي كان مفقوداً!
    val context = LocalContext.current

    var nameInput by remember { mutableStateOf(product.name) }
    var barcodeInput by remember { mutableStateOf(product.barcode) }
    var purchasePriceInput by remember { mutableStateOf(product.purchasePrice.formatClean()) }
    var sellingPriceInput by remember { mutableStateOf(product.sellingPrice.formatClean()) }
    var stockQuantityInput by remember { mutableStateOf(product.stockQuantity.formatClean()) }
    var categoryInput by remember { mutableStateOf(product.category) }
    var unitTypeInput by remember { mutableStateOf(product.unitType) }
    var selectedImageUri by remember { mutableStateOf(product.imageUrl) }
    var showEditScanner by remember { mutableStateOf(false) }

    // 🛠️ معالجة الفاصلة والنقطة للأرقام بدقة لتجنب اختفاء السعر
    val purchaseVal = purchasePriceInput.replace(",", ".").toDoubleOrNull() ?: 0.0
    val sellingVal = sellingPriceInput.replace(",", ".").toDoubleOrNull() ?: 0.0
    val netProfit = sellingVal - purchaseVal
    val profitPercentage = if (purchaseVal > 0.0) (netProfit / purchaseVal) * 100 else 0.0

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = AppTheme.text(isDark),
        unfocusedTextColor = AppTheme.text(isDark),
        focusedContainerColor = AppTheme.inputBg(isDark),
        unfocusedContainerColor = AppTheme.inputBg(isDark),
        focusedBorderColor = AppTheme.GoldPrimary,
        unfocusedBorderColor = AppTheme.cardBorder(isDark),
        focusedLabelColor = AppTheme.GoldPrimary,
        unfocusedLabelColor = AppTheme.subText(isDark)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.card(isDark),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "✏️ تعديل بيانات المنتج",
                color = AppTheme.text(isDark),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
                .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("اسم المنتج") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = purchasePriceInput,
                        onValueChange = { purchasePriceInput = it },
                        label = { Text("سعر الشراء") },
                        trailingIcon = { Text("دج", color = AppTheme.subText(isDark), fontSize = 11.sp) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = fieldColors
                    )

                    OutlinedTextField(
                        value = sellingPriceInput,
                        onValueChange = { sellingPriceInput = it },
                        label = { Text("سعر البيع") },
                        trailingIcon = { Text("دج", color = AppTheme.subText(isDark), fontSize = 11.sp) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = fieldColors
                    )
                }

                if (sellingVal > 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (netProfit >= 0) AppTheme.ColorSuccess.copy(alpha = 0.15f) else AppTheme.ColorDanger.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "📊 مؤشر الربحية المتوقع:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.text(isDark)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "الربح: ${netProfit.toDzd()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (netProfit >= 0) AppTheme.ColorSuccess else AppTheme.ColorDanger
                                )
                                Text(
                                    text = "الهامش: ${String.format(java.util.Locale.getDefault(), "%.1f", profitPercentage)}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.subText(isDark)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = stockQuantityInput,
                    onValueChange = { stockQuantityInput = it },
                    label = { Text("الكمية بالمخزون") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors
                )

                OutlinedTextField(
                    value = barcodeInput,
                    onValueChange = { barcodeInput = it },
                    label = { Text("رقم الباركود (اختياري)") },
                    trailingIcon = {
                        IconButton(onClick = { showEditScanner = true }) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "مسح الباركود",
                                tint = AppTheme.GoldPrimary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors
                )

                OutlinedTextField(
                    value = categoryInput,
                    onValueChange = { categoryInput = it },
                    label = { Text("القسم / التصنيف") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanBarcode = barcodeInput.trim()
                    val cleanName = nameInput.trim()

                    val updatedProduct = product.copy(
                        name = cleanName,
                        barcode = cleanBarcode,
                        // 🛠️ الحفظ الآمن للأرقام لمنع أي اختفاء لسعر الشراء أو الكمية
                        purchasePrice = purchasePriceInput.replace(",", ".").toDoubleOrNull() ?: 0.0,
                        sellingPrice = sellingPriceInput.replace(",", ".").toDoubleOrNull() ?: 0.0,
                        stockQuantity = stockQuantityInput.replace(",", ".").toDoubleOrNull() ?: 0.0,
                        category = categoryInput.trim().ifBlank { "عام" },
                        unitType = unitTypeInput,
                        imageUrl = selectedImageUri
                    )

                    onSave(updatedProduct)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.GoldPrimary)
            ) {
                Text("حفظ التعديلات", color = AppTheme.GoldPrimaryText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("إلغاء", color = AppTheme.subText(isDark))
            }
        }
    )

    if (showEditScanner) {
        BarcodeScannerDialog(
            onBarcodeScanned = { scannedCode ->
                barcodeInput = scannedCode
                showEditScanner = false
                Toast.makeText(context, "تم مسح الباركود: $scannedCode", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showEditScanner = false }
        )
    }
}

@Composable
fun AddToCartDialog(
    product: Product,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onAddToCart: (qty: Double) -> Unit
) {
    var qtyStr by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.card(isDark),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("إضافة للسلة: ${product.name}", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("المتوفر: ${product.stockQuantity.formatClean()} ${product.unitType}", color = AppTheme.subText(isDark), fontSize = 12.sp)

                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it },
                    label = { Text("الكمية المطلوبة") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTheme.text(isDark),
                        unfocusedTextColor = AppTheme.text(isDark),
                        focusedContainerColor = AppTheme.inputBg(isDark),
                        unfocusedContainerColor = AppTheme.inputBg(isDark),
                        focusedBorderColor = AppTheme.GoldPrimary,
                        unfocusedBorderColor = AppTheme.cardBorder(isDark)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = qtyStr.replace(",", ".").toDoubleOrNull() ?: 1.0
                    if (qty > 0) onAddToCart(qty)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.GoldPrimary)
            ) {
                Text("إضافة للسلة", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = AppTheme.subText(isDark))
            }
        }
    )
}
