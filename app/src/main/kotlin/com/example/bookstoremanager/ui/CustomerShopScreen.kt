package com.example.bookstoremanager.ui

import com.example.bookstoremanager.*
import com.example.bookstoremanager.data.*
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// السطران اللذان كانا ينقصان لحل المشكلة:
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
// ============================================
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.FirebaseDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class CustomerTab { HOME, FAVORITES, CART, ORDERS, PROFILE }

// ==========================================
// 💾 دالة حفظ الطلبات الأونلاين في سجل حركة المخزون
// ==========================================
fun saveOnlineOrderTransactions(context: Context, order: CustomerOrder) {
    val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)
    val jsonStr = prefs.getString("transactions_data", "[]") ?: "[]"

    try {
        val array = JSONArray(jsonStr)
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        order.items.forEach { item ->
            val obj = JSONObject()
            obj.put("productId", item.productId)
            obj.put("isSale", true)
            obj.put("date", currentDate)
            obj.put("partyName", "${order.customerName} (طلب أونلاين)")
            obj.put("quantity", item.quantity)
            obj.put("unitPrice", item.price)
            obj.put("total", item.quantity * item.price)
            array.put(obj)
        }
        prefs.edit().putString("transactions_data", array.toString()).apply()
    } catch (e: Exception) { e.printStackTrace() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerShopView(
    storeId: String,
    inventoryManager: InventoryManager,
    isDark: Boolean,
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("CustomerPrefs", Context.MODE_PRIVATE)
    val orderManager = remember { CustomerOrderManager(context) }

    var allProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var deliveryFee by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }

    var currentTab by remember { mutableStateOf(CustomerTab.HOME) }
    val favoriteIds = remember { mutableStateListOf<String>() }
    val cart = remember { mutableStateListOf<OrderItem>() }

    var customerName by remember { mutableStateOf(prefs.getString("cust_name", "") ?: "") }
    var customerPhone by remember { mutableStateOf(prefs.getString("cust_phone", "") ?: "") }
    var customerAddress by remember { mutableStateOf(prefs.getString("cust_address", "") ?: "") }

    LaunchedEffect(storeId) {
        if (storeId.isNotBlank()) {
            val storeRef = FirebaseDatabase.getInstance().getReference("Kasebo_Stores").child(storeId)
            storeRef.child("deliveryFee").get().addOnSuccessListener { snapshot ->
                deliveryFee = snapshot.value?.toString()?.toDoubleOrNull() ?: 0.0
            }

            storeRef.child("products").addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        val list = mutableListOf<Product>()
                        for (child in snapshot.children) {
                            try {
                                val id = child.child("id").value?.toString() ?: ""
                                val name = child.child("name").value?.toString() ?: ""
                                val barcode = child.child("barcode").value?.toString() ?: ""
                                val purchasePrice = child.child("purchasePrice").value?.toString()?.toDoubleOrNull() ?: 0.0
                                val sellingPrice = child.child("sellingPrice").value?.toString()?.toDoubleOrNull() ?: 0.0
                                val stockQuantity = child.child("stockQuantity").value?.toString()?.toDoubleOrNull() ?: 0.0
                                val category = child.child("category").value?.toString() ?: "عام"
                                val unitType = child.child("unitType").value?.toString() ?: "قطعة"
                                val imageUrl = child.child("imageUrl").value?.toString() ?: ""

                                if (id.isNotBlank() && stockQuantity > 0.0) {
                                    list.add(Product(
                                            id = id, name = name, barcode = barcode,
                                            purchasePrice = purchasePrice, sellingPrice = sellingPrice,
                                            stockQuantity = stockQuantity, category = category,
                                            unitType = unitType, imageUrl = imageUrl
                                    ))
                                }
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                        allProducts = list
                        isLoading = false
                    }
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                        isLoading = false
                    }
            })
        } else {
            isLoading = false
        }
    }

    val productsTotal = cart.sumOf { it.getTotalPrice() }
    val finalTotal = productsTotal + deliveryFee

    Scaffold(
        containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
        bottomBar = {
            Surface(
                color = AppTheme.card(isDark),
                shadowElevation = 16.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { BottomNavIcon(Icons.Default.Home, "الرئيسية", currentTab == CustomerTab.HOME, isDark) { currentTab = CustomerTab.HOME } }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { BottomNavIcon(Icons.Default.Favorite, "المفضلة", currentTab == CustomerTab.FAVORITES, isDark) { currentTab = CustomerTab.FAVORITES } }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Box(contentAlignment = Alignment.TopCenter) {
                            BottomNavIcon(Icons.Default.ShoppingCart, "السلة", currentTab == CustomerTab.CART, isDark) { currentTab = CustomerTab.CART }
                            if (cart.isNotEmpty()) {
                                Badge(containerColor = AppTheme.ColorDanger, modifier = Modifier.offset(x = 12.dp, y = (-4).dp)) { Text("${cart.sumOf { it.quantity }.toInt()}", color = Color.White, fontSize = 10.sp) }
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { BottomNavIcon(Icons.AutoMirrored.Filled.List, "طلباتي", currentTab == CustomerTab.ORDERS, isDark) { currentTab = CustomerTab.ORDERS } }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { BottomNavIcon(Icons.Default.Person, "حسابي", currentTab == CustomerTab.PROFILE, isDark) { currentTab = CustomerTab.PROFILE } }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (currentTab) {
                CustomerTab.HOME -> HomeContent(allProducts, isLoading, isDark, deliveryFee, favoriteIds, cart)
                CustomerTab.FAVORITES -> FavoritesContent(allProducts, favoriteIds, isDark, cart)
                CustomerTab.CART -> CartContent(cart, allProducts, deliveryFee, finalTotal, isDark, customerName, customerPhone, customerAddress, storeId, orderManager, onCheckoutSuccess = { currentTab = CustomerTab.ORDERS })
                CustomerTab.ORDERS -> MyOrdersContent(storeId, customerPhone, orderManager, isDark)
                CustomerTab.PROFILE -> ProfileContent(name = customerName, phone = customerPhone, address = customerAddress, isDark = isDark, onSave = { n, p, a -> customerName = n; customerPhone = p; customerAddress = a; prefs.edit().putString("cust_name", n).putString("cust_phone", p).putString("cust_address", a).apply(); Toast.makeText(context, "تم حفظ بياناتك بنجاح ✅", Toast.LENGTH_SHORT).show() }, onClose = onClose)
            }
        }
    }
}

@Composable
fun MyOrdersContent(storeId: String, customerPhone: String, orderManager: CustomerOrderManager, isDark: Boolean) {
    var myOrders by remember { mutableStateOf<List<CustomerOrder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(customerPhone, storeId) {
        if (customerPhone.isNotBlank() && storeId.isNotBlank()) {
            orderManager.listenToCustomerSpecificOrders(storeId, customerPhone) { orders -> myOrders = orders; isLoading = false }
        } else { isLoading = false }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 32.dp)) {
        Text("تتبع طلباتي 📦", fontSize = 28.sp, fontWeight = FontWeight.Black, color = AppTheme.text(isDark))
        Spacer(Modifier.height(24.dp))
        if (customerPhone.isBlank()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) { Text("يرجى إكمال بياناتك (رقم الهاتف) في صفحة حسابي.", color = AppTheme.subText(isDark), fontSize = 14.sp) }
        } else if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppTheme.GoldPrimary) }
        } else if (myOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) { Text("ليس لديك أي طلبات سابقة.", color = AppTheme.subText(isDark), fontSize = 14.sp) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize().weight(1f)) {
                items(myOrders, key = { it.orderId }) { order -> CustomerOrderTimelineCard(order = order, isDark = isDark) }
            }
        }
    }
}

@Composable
fun CustomerOrderTimelineCard(order: CustomerOrder, isDark: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("طلب #${order.orderId.takeLast(4)}", fontWeight = FontWeight.Bold, color = AppTheme.text(isDark), fontSize = 15.sp)
                Text("${order.totalAmount} دج", color = AppTheme.GoldPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text("يحتوي على ${order.items.size} منتج", color = AppTheme.subText(isDark), fontSize = 12.sp)
            Spacer(Modifier.height(20.dp))
            if (order.status == "Rejected") {
                Box(modifier = Modifier.fillMaxWidth().background(AppTheme.ColorDanger.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(10.dp), contentAlignment = Alignment.Center) { Text("تم رفض الطلب ❌", color = AppTheme.ColorDanger, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TimelineStep(label = "تم الاستلام", isActive = true, isDark = isDark)
                    TimelineDivider(isActive = order.status == "Processing" || order.status == "Delivered", isDark = isDark, modifier = Modifier.weight(1f))
                    TimelineStep(label = "قيد التجهيز", isActive = order.status == "Processing" || order.status == "Delivered", isDark = isDark)
                    TimelineDivider(isActive = order.status == "Delivered", isDark = isDark, modifier = Modifier.weight(1f))
                    TimelineStep(label = "تم التسليم", isActive = order.status == "Delivered", isDark = isDark)
                }
            }
        }
    }
}

@Composable
fun TimelineStep(label: String, isActive: Boolean, isDark: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(22.dp).background(if (isActive) AppTheme.ColorSuccess else AppTheme.inputBg(isDark), CircleShape), contentAlignment = Alignment.Center) { if (isActive) { Box(modifier = Modifier.size(8.dp).background(Color.White, CircleShape)) } }
        Spacer(Modifier.height(6.dp))
        Text(text = label, fontSize = 11.sp, color = if (isActive) AppTheme.text(isDark) else AppTheme.subText(isDark), fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun TimelineDivider(isActive: Boolean, isDark: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(3.dp).background(if (isActive) AppTheme.ColorSuccess else AppTheme.inputBg(isDark)))
}

@Composable
fun HomeContent(allProducts: List<Product>, isLoading: Boolean, isDark: Boolean, deliveryFee: Double, favoriteIds: MutableList<String>, cart: MutableList<OrderItem>) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }
    val categories = listOf("الكل") + allProducts.map { it.category }.distinct()

    val filteredProducts = allProducts.filter { prod ->
        val matchesSearch = prod.name.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "الكل" || prod.category == selectedCategory
        matchesSearch && matchesCategory
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(top = 32.dp, start = 20.dp, end = 20.dp, bottom = 10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Surface(color = AppTheme.GoldPrimary.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp)) { Text("التوصيل: $deliveryFee دج", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppTheme.GoldPrimary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) }
            }
            Spacer(Modifier.height(12.dp))
            Text("المتجر الرقمي", fontSize = 34.sp, fontWeight = FontWeight.Black, color = AppTheme.text(isDark))
            Spacer(Modifier.height(4.dp))
            Text("اطلب منتجاتك المفضلة الآن!", fontSize = 14.sp, color = AppTheme.subText(isDark))
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("بحث عن منتج...", color = AppTheme.subText(isDark)) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = AppTheme.subText(isDark)) }, singleLine = true, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = AppTheme.card(isDark), unfocusedContainerColor = AppTheme.card(isDark), focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent))
        }

        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat
                Surface(color = if (isSelected) AppTheme.GoldPrimary else AppTheme.card(isDark), shape = RoundedCornerShape(20.dp), modifier = Modifier.clickable { selectedCategory = cat }, shadowElevation = if (isSelected) 4.dp else 0.dp) { Text(text = cat, color = if (isSelected) AppTheme.GoldPrimaryText else AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppTheme.GoldPrimary) }
        } else if (filteredProducts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("لا توجد منتجات مطابقة.", color = AppTheme.subText(isDark)) }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
                gridItems(filteredProducts) { product -> ProductCardUI(product, favoriteIds, cart, isDark, context) }
            }
        }
    }
}

@Composable
fun FavoritesContent(allProducts: List<Product>, favoriteIds: MutableList<String>, isDark: Boolean, cart: MutableList<OrderItem>) {
    val favProducts = allProducts.filter { favoriteIds.contains(it.id) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 32.dp)) {
        Text("المفضلة ❤️", fontSize = 28.sp, fontWeight = FontWeight.Black, color = AppTheme.text(isDark))
        Spacer(Modifier.height(24.dp))
        if (favProducts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("قائمتك المفضلة فارغة حالياً.", color = AppTheme.subText(isDark), fontSize = 16.sp) }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
                gridItems(favProducts) { product -> ProductCardUI(product, favoriteIds, cart, isDark, context) }
            }
        }
    }
}

@Composable
fun ProductCardUI(product: Product, favoriteIds: MutableList<String>, cart: MutableList<OrderItem>, isDark: Boolean, context: Context) {
    val isFav = favoriteIds.contains(product.id)

    val bitmap = remember(product.imageUrl) {
        if (product.imageUrl.isNotBlank()) {
            try {
                val uri = android.net.Uri.parse(product.imageUrl)
                if (android.os.Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                    android.graphics.ImageDecoder.decodeBitmap(source)
                }
            } catch (e: Exception) { null }
        } else { null }
    }

    Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(16.dp)).background(AppTheme.GoldPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = product.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Text(text = product.name.take(1).uppercase(), fontSize = 40.sp, fontWeight = FontWeight.Black, color = AppTheme.GoldPrimary.copy(alpha = 0.4f))
                }

                IconButton(
                    onClick = { if (isFav) favoriteIds.remove(product.id) else favoriteIds.add(product.id) },
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp).size(28.dp).background(AppTheme.card(isDark).copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "مفضلة", tint = if (isFav) AppTheme.ColorDanger else AppTheme.subText(isDark), modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(text = product.name, color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
            Text(text = product.category, color = AppTheme.subText(isDark), fontSize = 11.sp)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${product.sellingPrice} دج", color = AppTheme.GoldPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Surface(
                    color = AppTheme.GoldPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp).clickable {
                        val existingIndex = cart.indexOfFirst { it.productId == product.id }
                        if (existingIndex >= 0) {
                            val current = cart[existingIndex]
                            if (current.quantity < product.stockQuantity) {
                                cart[existingIndex] = current.copy(quantity = current.quantity + 1.0)
                            } else { Toast.makeText(context, "الكمية تجاوزت المخزون المتاح!", Toast.LENGTH_SHORT).show() }
                        } else {
                            cart.add(OrderItem(product.id, product.name, product.sellingPrice, 1.0))
                            Toast.makeText(context, "تمت الإضافة 🛒", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة", tint = AppTheme.GoldPrimaryText, modifier = Modifier.padding(6.dp))
                }
            }
        }
    }
}

@Composable
fun CartContent(cart: MutableList<OrderItem>, allProducts: List<Product>, deliveryFee: Double, finalTotal: Double, isDark: Boolean, customerName: String, customerPhone: String, customerAddress: String, storeId: String, orderManager: CustomerOrderManager, onCheckoutSuccess: () -> Unit) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 32.dp)) {
        Text("سلة مشترياتي 🛒", fontSize = 28.sp, fontWeight = FontWeight.Black, color = AppTheme.text(isDark))
        Spacer(Modifier.height(24.dp))

        if (cart.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) { Text("السلة فارغة. أضف بعض المنتجات!", color = AppTheme.subText(isDark), fontSize = 16.sp) }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(cart) { item ->
                    val maxStock = allProducts.find { it.id == item.productId }?.stockQuantity ?: 9999.0
                    Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productName, color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("${item.price} دج", color = AppTheme.subText(isDark), fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(color = AppTheme.inputBg(isDark), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(32.dp).clickable { val i = cart.indexOf(item); if (item.quantity > 1.0) cart[i] = item.copy(quantity = item.quantity - 1.0) else cart.removeAt(i) }) { Box(contentAlignment = Alignment.Center) {Text("-", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 18.sp)} }
                                Text("${item.quantity.formatClean()}", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Surface(color = AppTheme.GoldPrimary, shape = RoundedCornerShape(8.dp), modifier = Modifier.size(32.dp).clickable { val i = cart.indexOf(item); if (item.quantity < maxStock) cart[i] = item.copy(quantity = item.quantity + 1.0) }) { Box(contentAlignment = Alignment.Center) {Text("+", color = AppTheme.GoldPrimaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp)} }
                            }
                        }
                    }
                }
            }

            Surface(color = AppTheme.card(isDark), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("المنتجات", color = AppTheme.subText(isDark)); Text("${finalTotal - deliveryFee} دج", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold) }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("التوصيل", color = AppTheme.subText(isDark)); Text("$deliveryFee دج", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold) }
                    HorizontalDivider(color = AppTheme.inputBg(isDark))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("الإجمالي الكلي", color = AppTheme.text(isDark), fontWeight = FontWeight.Black, fontSize = 18.sp); Text("$finalTotal دج", color = AppTheme.GoldPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp) }

                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (customerName.isNotBlank() && customerPhone.isNotBlank() && customerAddress.isNotBlank()) {
                                val order = CustomerOrder(orderId = UUID.randomUUID().toString(), customerName = customerName, customerPhone = customerPhone, deliveryAddress = customerAddress, items = cart.toList(), totalAmount = finalTotal, status = "New")
                                orderManager.placeOrder(storeId, order, { Toast.makeText(context, "تم إرسال طلبك بنجاح!", Toast.LENGTH_LONG).show(); cart.clear(); onCheckoutSuccess() }, { Toast.makeText(context, "فشل الإرسال", Toast.LENGTH_SHORT).show() })
                            } else { Toast.makeText(context, "الرجاء إكمال بياناتك في صفحة (حسابي) أولاً!", Toast.LENGTH_LONG).show() }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.GoldPrimary),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إرسال الطلب الآن 🚀", color = AppTheme.GoldPrimaryText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileContent(name: String, phone: String, address: String, isDark: Boolean, onSave: (String, String, String) -> Unit, onClose: () -> Unit) {
    var tempName by remember { mutableStateOf(name) }
    var tempPhone by remember { mutableStateOf(phone) }
    var tempAddress by remember { mutableStateOf(address) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 32.dp)) {
        Text("الملف الشخصي 👤", fontSize = 28.sp, fontWeight = FontWeight.Black, color = AppTheme.text(isDark))
        Spacer(Modifier.height(4.dp))
        Text("سيتم إرسال هذه البيانات مع كل طلب تقوم به لتسهيل التوصيل.", color = AppTheme.subText(isDark), fontSize = 13.sp)
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(value = tempName, onValueChange = { tempName = it }, label = { Text("الاسم الكامل") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = tempPhone, onValueChange = { tempPhone = it }, label = { Text("رقم الهاتف") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = tempAddress, onValueChange = { tempAddress = it }, label = { Text("عنوان التوصيل (الحي، المدينة)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        }
        Spacer(Modifier.weight(1f))

        Button(
            onClick = { onSave(tempName, tempPhone, tempAddress) },
            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.GoldPrimary),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("حفظ التحديثات", color = AppTheme.GoldPrimaryText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = AppTheme.ColorDanger.copy(alpha = 0.1f), contentColor = AppTheme.ColorDanger), modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "خروج", modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("الخروج من المتجر", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun BottomNavIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, isDark: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).padding(vertical = 4.dp)) {
        Icon(imageVector = icon, contentDescription = label, tint = if (isSelected) AppTheme.GoldPrimary else AppTheme.subText(isDark), modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(4.dp))
        if (isSelected) { Box(modifier = Modifier.size(4.dp).background(AppTheme.GoldPrimary, CircleShape)) } else { Spacer(Modifier.height(4.dp)) }
    }
}

// ==========================================
// ب. واجهة إدارة الطلبات للتاجر (Merchant Orders Management)
// ==========================================
@Composable
fun MerchantOrdersView(storeId: String, isDark: Boolean) {
    val context = LocalContext.current
    val orderManager = remember { CustomerOrderManager(context) }
    var ordersList by remember { mutableStateOf<List<CustomerOrder>>(emptyList()) }
    var selectedTab by remember { mutableStateOf("New") }

    LaunchedEffect(storeId) {
        if (storeId.isNotBlank()) { orderManager.listenToStoreOrders(storeId) { fetchedOrders -> ordersList = fetchedOrders } }
    }

    val filteredOrders = ordersList.filter { it.status == selectedTab }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("إدارة الطلبات الواردة", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
            Badge(containerColor = AppTheme.GoldPrimary) { Text("${ordersList.size}", color = AppTheme.GoldPrimaryText, modifier = Modifier.padding(4.dp), fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChipItem(text = "جديدة", isSelected = selectedTab == "New", isDark = isDark) { selectedTab = "New" }
            FilterChipItem(text = "قيد المعالجة", isSelected = selectedTab == "Processing", isDark = isDark) { selectedTab = "Processing" }
            FilterChipItem(text = "المكتملة", isSelected = selectedTab == "Delivered", isDark = isDark) { selectedTab = "Delivered" }
        }

        if (filteredOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) { Text("لا توجد طلبات في هذه القائمة حالياً.", color = AppTheme.subText(isDark), fontSize = 14.sp) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize().weight(1f)) {
                items(filteredOrders, key = { it.orderId }) { order ->
                    var expanded by remember { mutableStateOf(false) }
                    Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(color = AppTheme.GoldPrimary.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) { Text("#${order.orderId.takeLast(4)}", fontWeight = FontWeight.Bold, color = AppTheme.GoldPrimary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) }
                                    Text(order.customerName, color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                Text("${order.totalAmount} دج", color = AppTheme.ColorSuccess, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            }
                            Text("📞 ${order.customerPhone} | 📍 ${order.deliveryAddress}", color = AppTheme.subText(isDark), fontSize = 12.sp)
                            TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(24.dp)) { Text(if (expanded) "إخفاء التفاصيل ▲" else "عرض المنتجات ▼", color = AppTheme.GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold) }

                            if (order.status != "Delivered" || expanded) {
                                HorizontalDivider(color = AppTheme.inputBg(isDark), thickness = 1.dp)
                                order.items.forEach { item ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("• ${item.productName} (x${item.quantity})", color = AppTheme.subText(isDark), fontSize = 12.sp)
                                        Text("${item.getTotalPrice()} دج", color = AppTheme.subText(isDark), fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                if (order.status == "New") {
                                    Button(onClick = { orderManager.updateOrderStatus(storeId, order.orderId, "Processing") { Toast.makeText(context, "تم قبول الطلب بنجاح ✅", Toast.LENGTH_SHORT).show() } }, colors = ButtonDefaults.buttonColors(containerColor = AppTheme.ColorSuccess), modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(12.dp)) { Text("قبول الطلب", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                                    Button(onClick = { orderManager.updateOrderStatus(storeId, order.orderId, "Rejected") { Toast.makeText(context, "تم الرفض ❌", Toast.LENGTH_SHORT).show() } }, colors = ButtonDefaults.buttonColors(containerColor = AppTheme.ColorDanger.copy(alpha = 0.1f), contentColor = AppTheme.ColorDanger), modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(12.dp)) { Text("رفض", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                                } else if (order.status == "Processing") {
                                    Button(
                                        onClick = {
                                            // 🛠️ الإصلاح الجذري: استدعاء المحرك الخلفي فقط وهو سيتكفل بالباقي بأمان.
                                            // (تم مسح الكود اليدوي لتجنب الخصم المزدوج للمخزون)
                                            orderManager.updateOrderStatus(storeId, order.orderId, "Delivered") {
                                                Toast.makeText(context, "تم التسليم وتم تحديث المخزون والإيرادات تلقائياً! 🎉", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.GoldPrimary),
                                        modifier = Modifier.fillMaxWidth().height(40.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("تحديد كـ تم التسليم 🚚", color = AppTheme.GoldPrimaryText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                } else if (order.status == "Delivered") {
                                    Box(modifier = Modifier.fillMaxWidth().background(AppTheme.ColorSuccess.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(10.dp), contentAlignment = Alignment.Center) { Text("✅ مكتمل ومؤرشف في المخزون", color = AppTheme.ColorSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
