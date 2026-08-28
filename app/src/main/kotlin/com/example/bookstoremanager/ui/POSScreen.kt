package com.example.bookstoremanager.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bookstoremanager.*
import com.example.bookstoremanager.data.*

@Composable
fun POSScreen(isDark: Boolean, inventoryManager: InventoryManager, onBack: () -> Unit) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val cartItems = remember { mutableStateListOf<CartItem>() }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showScannerDialog by remember { mutableStateOf(false) }

    val totalAmount = cartItems.sumOf { it.product.sellingPrice * it.quantity }

    val searchResults = remember(searchQuery) {
        if (searchQuery.isNotBlank()) {
            inventoryManager.getProductList().filter { p ->
                p.name.contains(searchQuery, ignoreCase = true) || p.barcode.contains(searchQuery)
            }
        } else emptyList()
    }

    Scaffold(
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().background(AppTheme.card(isDark)).padding(16.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = AppTheme.text(isDark))
                }
                Spacer(Modifier.width(8.dp))
                Text(text = "الكاشير السريع 🛒", color = AppTheme.text(isDark), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        },
        bottomBar = {
            Surface(color = AppTheme.card(isDark), shadowElevation = 16.dp) {
                Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الإجمالي المطلوب:", fontSize = 18.sp, color = AppTheme.subText(isDark))
                        Text(
                            text = "${totalAmount.formatClean()} د.ج",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFEF4444)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { cartItems.clear() },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.inputBg(isDark))
                        ) {
                            Text("إفراغ", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (cartItems.isNotEmpty()) showPaymentDialog = true
                                else Toast.makeText(context, "السلة فارغة", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.GoldPrimary)
                        ) {
                            Text("الدفع والتأكيد", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = AppTheme.bg(isDark)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("بحث بالاسم أو الباركود...", color = AppTheme.subText(isDark)) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = AppTheme.GoldPrimary) },
                        trailingIcon = {
                            IconButton(onClick = { showScannerDialog = true }) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "مسح باركود", tint = AppTheme.GoldPrimary)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTheme.text(isDark),
                            unfocusedTextColor = AppTheme.text(isDark),
                            focusedContainerColor = AppTheme.inputBg(isDark),
                            unfocusedContainerColor = AppTheme.inputBg(isDark),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = AppTheme.GoldPrimary,
                            cursorColor = AppTheme.GoldPrimary
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (searchQuery.isNotBlank()) {
                Text(
                    text = "نتائج البحث:",
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.GoldPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults) { product ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (product.stockQuantity > 0) {
                                    val existing = cartItems.find { item -> item.product.id == product.id }
                                    if (existing != null) {
                                        val idx = cartItems.indexOf(existing)
                                        cartItems[idx] = existing.copy(quantity = existing.quantity + 1.0)
                                    } else {
                                        cartItems.add(CartItem(product, 1.0))
                                    }
                                    searchQuery = ""
                                } else {
                                    Toast.makeText(context, "نفدت الكمية!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)),
                            border = BorderStroke(1.dp, AppTheme.GoldPrimary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(product.name, color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${product.sellingPrice.toDzd()} | المخزون: ${product.stockQuantity}", color = AppTheme.subText(isDark), fontSize = 12.sp)
                                }
                                Icon(Icons.Default.Add, null, tint = AppTheme.GoldPrimary)
                            }
                        }
                    }
                    if (searchResults.isEmpty()) {
                        item { Text("لا توجد منتجات مطابقة.", color = AppTheme.subText(isDark)) }
                    }
                }
            } else {
                Text(
                    text = "المنتجات في السلة (${cartItems.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.text(isDark),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cartItems) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)),
                            border = BorderStroke(1.dp, AppTheme.inputBg(isDark))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.product.name, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark), fontSize = 14.sp)
                                    Text(item.product.sellingPrice.toDzd(), color = AppTheme.subText(isDark), fontSize = 12.sp)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.background(AppTheme.inputBg(isDark), RoundedCornerShape(8.dp)).padding(4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (item.quantity > 1.0) {
                                                val i = cartItems.indexOf(item)
                                                cartItems[i] = item.copy(quantity = item.quantity - 1.0)
                                            } else {
                                                cartItems.remove(item)
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("-", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }

                                    Text(
                                        text = item.quantity.formatClean(),
                                        fontWeight = FontWeight.Bold,
                                        color = AppTheme.text(isDark),
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )

                                    IconButton(
                                        onClick = {
                                            if (item.quantity < item.product.stockQuantity) {
                                                val i = cartItems.indexOf(item)
                                                cartItems[i] = item.copy(quantity = item.quantity + 1.0)
                                            } else {
                                                Toast.makeText(context, "الكمية لا تكفي", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("+", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showPaymentDialog) {
            AlertDialog(
                onDismissRequest = { showPaymentDialog = false },
                containerColor = AppTheme.card(isDark),
                title = { Text("تأكيد الدفع", fontWeight = FontWeight.Bold, color = AppTheme.text(isDark)) },
                text = { Text("المبلغ المطلـوب:\n${totalAmount.formatClean()} د.ج", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark)) },
                confirmButton = {
                    Button(
                        onClick = {
                            // 🌟 1. إرسال الطلبات إلى السلة الموحدة
                            inventoryManager.getShoppingCart().clear()
                            cartItems.forEach { inventoryManager.addToCart(it.product, it.quantity) }

                            // 🌟 2. استدعاء الدالة الموحدة التي تقوم بكل شيء بأمان تام
                            inventoryManager.checkoutCart()

                            // 🌟 3. توليد الفاتورة محمي بـ try-catch لتفادي الانهيار
                            try {
                                PdfInvoiceGenerator.generateAndSavePdf(context = context, cartItems = cartItems, totalAmount = totalAmount)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            Toast.makeText(context, "تم البيع بنجاح!", Toast.LENGTH_SHORT).show()
                            cartItems.clear()
                            showPaymentDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.GoldPrimary)
                    ) {
                        Text("تأكيد واستلام الكاش ✅", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPaymentDialog = false }) { Text("إلغاء", color = AppTheme.subText(isDark)) }
                }
            )
        }

        if (showScannerDialog) {
            BarcodeScannerDialog(
                onBarcodeScanned = { scannedCode ->
                    showScannerDialog = false
                    val product = inventoryManager.getProductList().find { it.barcode == scannedCode }
                    if (product != null) {
                        if (product.stockQuantity > 0) {
                            val existing = cartItems.find { item -> item.product.id == product.id }
                            if (existing != null) {
                                val idx = cartItems.indexOf(existing)
                                cartItems[idx] = existing.copy(quantity = existing.quantity + 1.0)
                            } else {
                                cartItems.add(CartItem(product, 1.0))
                            }
                            Toast.makeText(context, "تمت إضافة: ${product.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "المنتج نافد من المخزون!", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "لم يتم العثور على منتج بهذا الباركود", Toast.LENGTH_LONG).show()
                    }
                },
                onDismiss = { showScannerDialog = false }
            )
        }
    }
}
