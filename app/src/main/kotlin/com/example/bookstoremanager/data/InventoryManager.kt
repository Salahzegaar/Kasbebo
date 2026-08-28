package com.example.bookstoremanager.data

import com.example.bookstoremanager.*
import android.content.Context
import android.net.Uri
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class InventoryManager(private val context: Context) {
    private val dbHelper = LocalDatabaseHelper(context)
    private val jsonFile = File(context.filesDir, "kasebo_local_store.json")

    private val productList = mutableListOf<Product>()
    private val salesHistory = mutableListOf<SaleRecord>()
    private val expenseList = mutableListOf<ExpenseRecord>()
    private val customerList = mutableListOf<CustomerAccount>()
    private val shoppingCart = mutableListOf<CartItem>()
    private val incomingOrdersList = mutableListOf<CustomerOrder>()

    private var totalCashCollected = 0.0
    private var totalRealProfit = 0.0

    init {
        loadFromLocalDatabase()
        startRealtimeSync()
    }

    private fun getCurrentStoreId(): String {
        val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)
        return prefs.getString("store_secret_id", "") ?: ""
    }

    // مزامنة تلقائية لجميع البيانات الحالية مع Firebase (نسخة آمنة)
    fun autoSyncToCloud() {
        try {
            val storeId = getCurrentStoreId()
            if (storeId.isNotBlank()) {
                val database = FirebaseDatabase.getInstance().getReference("Kasebo_Stores").child(storeId)
                // 🛠️ التحديث الآمن: استخدام Map وتحديث الأجزاء فقط لمنع مسح طلبات الزبائن
                val updates = hashMapOf<String, Any>(
                    "totalCash" to totalCashCollected,
                    "totalProfit" to totalRealProfit,
                    "products" to productList.toList(),
                    "customers" to customerList.toList(),
                    "expenses" to expenseList.toList(),
                    "sales" to salesHistory.toList()
                )
                database.updateChildren(updates).addOnFailureListener { e -> e.printStackTrace() }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun startRealtimeSync(onUpdate: () -> Unit = {}) {
        val storeId = getCurrentStoreId()
        if (storeId.isBlank()) return

        val storeRef = FirebaseDatabase.getInstance().getReference("Kasebo_Stores").child(storeId)
        storeRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return
                    try {
                        totalCashCollected = snapshot.child("totalCash").value?.toString()?.toDoubleOrNull() ?: 0.0
                        totalRealProfit = snapshot.child("totalProfit").value?.toString()?.toDoubleOrNull() ?: 0.0

                        customerList.clear()
                        for (cSnap in snapshot.child("customers").children) {
                            val id = cSnap.child("id").value?.toString() ?: ""
                            val name = cSnap.child("name").value?.toString() ?: ""
                            val last = cSnap.child("lastName").value?.toString() ?: cSnap.child("last").value?.toString() ?: ""
                            val phone = cSnap.child("phone").value?.toString() ?: ""
                            val social = cSnap.child("socialMedia").value?.toString() ?: cSnap.child("social").value?.toString() ?: ""

                            val cust = CustomerAccount(id, name, last, phone, social)
                            for (rSnap in cSnap.child("records").children) {
                                val rId = rSnap.child("id").value?.toString() ?: ""
                                val title = rSnap.child("title").value?.toString() ?: ""
                                val amount = rSnap.child("amount").value?.toString()?.toDoubleOrNull() ?: 0.0
                                val isRec = rSnap.child("receivable").value as? Boolean ?: (rSnap.child("isRec").value as? Boolean ?: true)
                                cust.records.add(DebtRecord(rId, title, amount, isRec))
                            }
                            customerList.add(cust)
                        }

                        saveCustomerAndFinancials()
                        onUpdate()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadFromLocalDatabase() {
        try {
            productList.clear()
            productList.addAll(dbHelper.getAllProducts())
            salesHistory.clear()
            salesHistory.addAll(dbHelper.getAllSales())
            expenseList.clear()
            expenseList.addAll(dbHelper.getAllExpenses())
            loadCustomerAndFinancials()
            if (productList.isEmpty() && jsonFile.exists()) {
                importFromJson(jsonFile.readText())
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun loadCustomerAndFinancials() {
        try {
            if (!jsonFile.exists()) return
            val jsonStr = jsonFile.readText()
            if (jsonStr.isBlank()) return
            val root = JSONObject(jsonStr)

            totalCashCollected = root.optDouble("totalCash", 0.0)
            totalRealProfit = root.optDouble("totalProfit", 0.0)

            customerList.clear()
            val cArr = root.optJSONArray("customers")
            if (cArr != null) {
                for (i in 0 until cArr.length()) {
                    try {
                        val o = cArr.getJSONObject(i)
                        val cust = CustomerAccount(
                            o.optString("id", ""), o.optString("name", ""), o.optString("last", ""), o.optString("phone", ""), o.optString("social", "")
                        )
                        val rArr = o.optJSONArray("records")
                        if (rArr != null) {
                            for (j in 0 until rArr.length()) {
                                try {
                                    val ro = rArr.getJSONObject(j)
                                    cust.records.add(DebtRecord(ro.optString("id", ""), ro.optString("title", ""), ro.optString("amount", "0.0").toDoubleOrNull() ?: 0.0, ro.optBoolean("isRec", true)))
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                        }
                        customerList.add(cust)
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun saveCustomerAndFinancials() {
        try {
            val root = JSONObject()
            root.put("totalCash", totalCashCollected)
            root.put("totalProfit", totalRealProfit)
            val cArr = JSONArray()
            for (c in customerList) {
                val o = JSONObject().apply { put("id", c.id); put("name", c.name); put("last", c.lastName); put("phone", c.phone); put("social", c.socialMedia) }
                val rArr = JSONArray()
                for (r in c.records) {
                    rArr.put(JSONObject().apply { put("id", r.id); put("title", r.title); put("amount", r.amount); put("isRec", r.isReceivable) })
                }
                o.put("records", rArr)
                cArr.put(o)
            }
            root.put("customers", cArr)
            jsonFile.writeText(root.toString())
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun addProduct(product: Product) {
        val existingIndex = productList.indexOfFirst { it.id == product.id || (it.name.trim().lowercase() == product.name.trim().lowercase() && product.name.isNotBlank()) }
        val productToSave = if (existingIndex >= 0) {
            val originalId = productList[existingIndex].id
            val updated = product.copy(id = originalId)
            productList[existingIndex] = updated
            updated
        } else {
            productList.add(product)
            product
        }
        dbHelper.insertOrUpdateProduct(productToSave)
        autoSyncToCloud()
    }

    fun addProducts(newProducts: List<Product>) {
        if (newProducts.isNotEmpty()) {
            for (p in newProducts) {
                val existingIndex = productList.indexOfFirst { it.id == p.id || (it.name.trim().lowercase() == p.name.trim().lowercase() && p.name.isNotBlank()) }
                if (existingIndex >= 0) {
                    productList[existingIndex] = p.copy(id = productList[existingIndex].id)
                } else {
                    productList.add(p)
                }
            }
            dbHelper.insertBatchProducts(newProducts)
            autoSyncToCloud()
        }
    }

    fun updateProduct(product: Product) {
        productList.removeAll { it.name.trim().lowercase() == product.name.trim().lowercase() && it.id != product.id }
        var index = productList.indexOfFirst { it.id == product.id }
        if (index < 0) index = productList.indexOfFirst { it.name.trim().lowercase() == product.name.trim().lowercase() }
        val productToSave = if (index >= 0) {
            val originalId = productList[index].id
            val updated = product.copy(id = originalId)
            productList[index] = updated
            updated
        } else {
            productList.add(product)
            product
        }
        dbHelper.insertOrUpdateProduct(productToSave)
        autoSyncToCloud()
    }

    fun removeProduct(id: String): Boolean {
        val removed = productList.removeIf { it.id == id }
        if (removed) {
            dbHelper.deleteProduct(id)
            autoSyncToCloud()
        }
        return removed
    }

    // ==========================================
    // 🔍 قراءة عناوين الأعمدة من ملف CSV
    // ==========================================
    fun getCsvHeaders(context: Context, uri: Uri): List<String> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream!!))
            val firstLine = reader.readLine() ?: ""
            inputStream.close()

            val separator = if (firstLine.contains(";")) ";" else ","
            if (firstLine.isBlank()) emptyList() else firstLine.split(separator).map { it.trim() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==========================================
    // 📥 استيراد المنتجات المطور (بدعم ربط الأعمدة الديناميكي)
    // ==========================================
    fun importProductsFromCsvMapped(
        context: Context,
        uri: Uri,
        nameIdx: Int,
        barcodeIdx: Int,
        purchaseIdx: Int,
        sellIdx: Int,
        qtyIdx: Int,
        catIdx: Int,
        hasHeaderRow: Boolean,
        onSuccess: (Int) -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                onFailure("لم نتمكن من قراءة الملف!")
                return
            }

            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = reader.readLines()

            if (lines.isEmpty() || (hasHeaderRow && lines.size <= 1)) {
                onFailure("الملف فارغ أو لا يحتوي على بيانات منتجات!")
                return
            }

            val productsToSaveLocally = mutableListOf<Product>()
            val startIndex = if (hasHeaderRow) 1 else 0
            var addedOrUpdatedCount = 0

            for (i in startIndex until lines.size) {
                val line = lines[i]
                if (line.isBlank()) continue

                val separator = if (line.contains(";")) ";" else ","
                val columns = line.split(separator)

                val name = if (nameIdx in columns.indices && nameIdx != -1) columns[nameIdx].trim() else ""
                val barcode = if (barcodeIdx in columns.indices && barcodeIdx != -1) columns[barcodeIdx].trim() else ""

                val purchasePriceStr = if (purchaseIdx in columns.indices && purchaseIdx != -1) columns[purchaseIdx].trim() else "0"
                val sellingPriceStr = if (sellIdx in columns.indices && sellIdx != -1) columns[sellIdx].trim() else "0"
                val qtyStr = if (qtyIdx in columns.indices && qtyIdx != -1) columns[qtyIdx].trim() else "0"

                val purchasePrice = purchasePriceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                val sellingPrice = sellingPriceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                val stock = qtyStr.replace(",", ".").toDoubleOrNull() ?: 0.0

                val category = if (catIdx in columns.indices && catIdx != -1) columns[catIdx].trim() else "عام"

                if (name.isNotBlank()) {
                    val existingIndex = productList.indexOfFirst {
                        (it.barcode.isNotBlank() && it.barcode == barcode) ||
                        (it.name.trim().equals(name, ignoreCase = true))
                    }

                    if (existingIndex >= 0) {
                        val existingProduct = productList[existingIndex]
                        val updatedProduct = existingProduct.copy(
                            purchasePrice = if (purchaseIdx != -1) purchasePrice else existingProduct.purchasePrice,
                            sellingPrice = if (sellIdx != -1) sellingPrice else existingProduct.sellingPrice,
                            stockQuantity = existingProduct.stockQuantity + stock,
                            category = if (category.isNotBlank() && category != "عام") category else existingProduct.category,
                            barcode = barcode.ifBlank { existingProduct.barcode }
                        )
                        productList[existingIndex] = updatedProduct
                        productsToSaveLocally.add(updatedProduct)
                    } else {
                        val newProduct = Product(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            barcode = barcode,
                            purchasePrice = purchasePrice,
                            sellingPrice = sellingPrice,
                            stockQuantity = stock,
                            category = if (category.isBlank()) "عام" else category
                        )
                        productList.add(newProduct)
                        productsToSaveLocally.add(newProduct)
                    }
                    addedOrUpdatedCount++
                }
            }

            if (productsToSaveLocally.isNotEmpty()) {
                dbHelper.insertBatchProducts(productsToSaveLocally)
                autoSyncToCloud()
                onSuccess(addedOrUpdatedCount)
            } else {
                onFailure("لم يتم العثور على بيانات صالحة! تأكد من ربط عمود 'اسم المنتج' بشكل صحيح.")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            onFailure("حدث خطأ: تأكد أن الملف بصيغة CSV ومغلق في التطبيقات الأخرى.")
        }
    }

    fun addExpense(expense: ExpenseRecord) { expenseList.add(0, expense); dbHelper.insertExpense(expense); autoSyncToCloud() }
    fun removeExpense(id: String): Boolean {
        val removed = expenseList.removeIf { it.id == id }
        if (removed) {
            // 🛠️ الإصلاح: حذف المصروف من قاعدة البيانات المحلية حتى لا يعود مجدداً
            dbHelper.deleteExpense(id)
            autoSyncToCloud()
        }
        return removed
    }

    fun addCustomer(customer: CustomerAccount) { customerList.add(0, customer); saveCustomerAndFinancials(); autoSyncToCloud() }
    fun removeCustomer(id: String): Boolean { val removed = customerList.removeIf { it.id == id }; if (removed) { saveCustomerAndFinancials(); autoSyncToCloud() }; return removed }
    fun addDebtRecordToCustomer(customerId: String, record: DebtRecord): Boolean { val added = customerList.find { it.id == customerId }?.records?.add(0, record) != null; if (added) { saveCustomerAndFinancials(); autoSyncToCloud() }; return added }
    fun removeDebtRecordFromCustomer(customerId: String, recordId: String): Boolean { val removed = customerList.find { it.id == customerId }?.records?.removeIf { it.id == recordId } ?: false; if (removed) { saveCustomerAndFinancials(); autoSyncToCloud() }; return removed }

    fun getShoppingCart(): MutableList<CartItem> = shoppingCart

    fun addToCart(product: Product, quantity: Double): Boolean {
        if (product.stockQuantity < quantity) return false
        val existing = shoppingCart.find { it.product.id == product.id }
        if (existing != null) {
            if (existing.quantity + quantity > product.stockQuantity) return false
            existing.quantity += quantity
        } else {
            shoppingCart.add(CartItem(product, quantity))
        }
        return true
    }

    fun scanAndAddToCart(scannedCode: String, onResult: (Boolean, String) -> Unit) {
        val code = scannedCode.trim()
        val product = productList.find { it.barcode.trim() == code || it.id.trim() == code || it.name.trim().equals(code, ignoreCase = true) }
        if (product != null) {
            val added = addToCart(product, 1.0)
            if (added) onResult(true, "تمت إضافة: ${product.name}") else onResult(false, "نفذت الكمية في المخزون (${product.name})")
        } else {
            onResult(false, "المنتج غير مسجل في المخزون ($code)")
        }
    }

    fun removeFromCart(productId: String) { shoppingCart.removeIf { it.product.id == productId } }
    fun getCartTotal(): Double = shoppingCart.sumOf { it.getTotalPrice() }

    fun checkoutCart(): String {
        if (shoppingCart.isEmpty()) return ""

        // 1️⃣ توليد معرّف فريد وتوقيت موحد للعملية بأكملها (لتجنب التكرار وربط العناصر في التقارير)
        val transactionTimestamp = System.currentTimeMillis()
        val invoiceId = "INV-" + UUID.randomUUID().toString().substring(0, 6).uppercase()
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(transactionTimestamp))

        var invoiceText = "🧾 فاتورة مبيعات - كاسبو Kasebo\n"
        invoiceText += "رقم الفاتورة: $invoiceId\n"
        invoiceText += "التاريخ: $date\n"
        invoiceText += "----------------\n"

        var total = 0.0

        for (item in shoppingCart) {
            val p = productList.find { it.id == item.product.id }
            val qty = item.quantity

            if (p != null && p.stockQuantity >= qty) {
                // 2️⃣ خصم المخزون
                p.stockQuantity -= qty
                val revenue = p.sellingPrice * qty
                val profit = revenue - (p.purchasePrice * qty)

                // 3️⃣ تحديث الإيرادات والأرباح
                totalCashCollected += revenue
                totalRealProfit += profit

                // 4️⃣ تسجيل البيع في السجل المركزي الموحد (باستخدام التوقيت الموحد للربط)
                val saleRecord = SaleRecord(p.name, qty, revenue, profit, transactionTimestamp)
                salesHistory.add(0, saleRecord)

                // 5️⃣ الحفظ في قاعدة البيانات المحلية (المنتج + المبيعات) بشكل مباشر
                dbHelper.insertOrUpdateProduct(p)
                dbHelper.insertSale(saleRecord)

                // 6️⃣ إرسال تنبيه في حال اقترب المخزون من النفاذ
                if (p.stockQuantity <= p.minStockAlert) {
                    try {
                        NotificationHelper.showNotification(context, "⚠️ تنبيه مخزون", "المنتج '${p.name}' المتبقي ${p.stockQuantity.formatClean()} ${p.unitType} فقط!")
                    } catch (e: Exception) { e.printStackTrace() }
                }

                invoiceText += "▪ ${p.name}\n  ${qty.formatClean()} ${p.unitType} × ${p.sellingPrice.formatClean()} = ${revenue.formatClean()} دج\n"
                total += revenue
            }
        }
        invoiceText += "----------------\n💰 المجموع الإجمالي: ${total.formatClean()} دج\nشكراً لزيارتكم!"

        // 7️⃣ تفريغ السلة وتحديث السحابة لمرة واحدة فقط للعملية ككل
        shoppingCart.clear()
        saveCustomerAndFinancials()
        autoSyncToCloud()

        return invoiceText
    }

    fun sendCustomerOrderToCloud(storeId: String, customerName: String, customerPhone: String, deliveryAddress: String = "", cartItems: List<CartItem>, totalAmount: Double, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val targetStoreId = if (storeId.isNotBlank()) storeId else getCurrentStoreId()
        if (targetStoreId.isBlank()) { onFailure("كود المتجر غير صحيح!"); return }

        val storeRef = FirebaseDatabase.getInstance().getReference("Kasebo_Stores").child(targetStoreId)
        val orderId = "ORD-" + System.currentTimeMillis().toString().takeLast(6)

        val orderItemsList = cartItems.map { OrderItem(productId = it.product.id, productName = it.product.name, price = it.product.sellingPrice, quantity = it.quantity) }

        val newOrder = CustomerOrder(
            orderId = orderId, storeId = targetStoreId, customerName = customerName.ifBlank { "زبون إلكتروني" },
            customerPhone = customerPhone, deliveryAddress = deliveryAddress, items = orderItemsList,
            totalAmount = totalAmount, status = "جديدة", timestamp = System.currentTimeMillis()
        )

        storeRef.child("products").get().addOnSuccessListener { snapshot ->
            val updatedProductsList = mutableListOf<Product>()
            for (pChild in snapshot.children) {
                try {
                    val id = pChild.child("id").value?.toString() ?: pChild.key ?: ""
                    val name = pChild.child("name").value?.toString() ?: ""
                    val barcode = pChild.child("barcode").value?.toString() ?: ""
                    val purchasePrice = (pChild.child("purchasePrice").value?.toString() ?: pChild.child("purchase").value?.toString())?.toDoubleOrNull() ?: 0.0
                    val sellingPrice = (pChild.child("sellingPrice").value?.toString() ?: pChild.child("sell").value?.toString())?.toDoubleOrNull() ?: 0.0
                    val wholesalePrice = pChild.child("wholesalePrice").value?.toString()?.toDoubleOrNull() ?: 0.0
                    var stockQuantity = (pChild.child("stockQuantity").value?.toString() ?: pChild.child("qty").value?.toString())?.toDoubleOrNull() ?: 0.0
                    val minStockAlert = pChild.child("minStockAlert").value?.toString()?.toDoubleOrNull() ?: 5.0
                    val category = pChild.child("category").value?.toString() ?: pChild.child("cat").value?.toString() ?: "عام"
                    val unitType = pChild.child("unitType").value?.toString() ?: pChild.child("unit").value?.toString() ?: "قطعة"
                    val imageUrl = pChild.child("imageUrl").value?.toString() ?: ""

                    if (name.isNotBlank()) {
                        val cartItem = cartItems.find { it.product.id == id }
                        if (cartItem != null) {
                            stockQuantity = (stockQuantity - cartItem.quantity).coerceAtLeast(0.0)
                        }
                        updatedProductsList.add(
                            Product(
                                id = id, name = name, barcode = barcode, purchasePrice = purchasePrice,
                                sellingPrice = sellingPrice, stockQuantity = stockQuantity, category = category,
                                unitType = unitType, imageUrl = imageUrl, wholesalePrice = wholesalePrice, minStockAlert = minStockAlert
                            )
                        )
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }

            val updates = HashMap<String, Any>()
            updates["orders/$orderId"] = newOrder
            updates["products"] = updatedProductsList

            storeRef.updateChildren(updates).addOnSuccessListener { onSuccess() }.addOnFailureListener { e -> onFailure(e.message ?: "فشل إرسال الطلب") }
        }.addOnFailureListener { e -> onFailure("فشل الاتصال: ${e.message}") }
    }

    fun listenForIncomingCustomerOrders(storeId: String, onOrdersUpdated: (List<CustomerOrder>) -> Unit) {
        val targetStoreId = if (storeId.isNotBlank()) storeId else getCurrentStoreId()
        if (targetStoreId.isBlank()) return

        val ordersRef = FirebaseDatabase.getInstance().getReference("Kasebo_Stores").child(targetStoreId).child("orders")
        val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)

        ordersRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    incomingOrdersList.clear()
                    val lastNotifiedTime = prefs.getLong("last_notified_order_time", System.currentTimeMillis() - 10000)
                    var newestOrderTime = lastNotifiedTime

                    for (child in snapshot.children) {
                        val order = child.getValue(CustomerOrder::class.java)
                        if (order != null) {
                            incomingOrdersList.add(order)
                            if ((order.status == "جديدة" || order.status == "New") && order.timestamp > lastNotifiedTime) {
                                NotificationHelper.showNotification(context, "🛍️ طلب زبون جديد!", "وصلك طلب بقيمة ${order.totalAmount.formatClean()} دج")
                                if (order.timestamp > newestOrderTime) newestOrderTime = order.timestamp
                            }
                        }
                    }
                    if (newestOrderTime > lastNotifiedTime) prefs.edit().putLong("last_notified_order_time", newestOrderTime).apply()
                    incomingOrdersList.sortByDescending { it.timestamp }
                    onOrdersUpdated(incomingOrdersList)
                }
                override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun updateOrderStatus(storeId: String, orderId: String, newStatus: String) {
        val targetStoreId = if (storeId.isNotBlank()) storeId else getCurrentStoreId()
        if (targetStoreId.isBlank() || orderId.isBlank()) return
        FirebaseDatabase.getInstance().getReference("Kasebo_Stores").child(targetStoreId).child("orders").child(orderId).child("status").setValue(newStatus)
    }

    // ========================================================
    // ⚠️ الأسطر التي سقطت في الكود السابق وأدت إلى الـ 442 خطأ! ⚠️
    // ========================================================
    fun getTotalStoreReceivable(): Double = customerList.sumOf { it.getTotalReceivable() }
    fun getTotalStorePayable(): Double = 0.0

    fun resetDailyData() {
        totalCashCollected = 0.0
        totalRealProfit = 0.0
        salesHistory.clear()
        expenseList.clear()
        shoppingCart.clear()
        dbHelper.resetDaily()
        saveCustomerAndFinancials()
        autoSyncToCloud()
    }

    fun getLowStockCount(): Int = productList.count { it.stockQuantity <= it.minStockAlert }
    fun getProductList(): MutableList<Product> = productList
    fun getSalesHistory(): MutableList<SaleRecord> = salesHistory
    fun getExpenseList(): MutableList<ExpenseRecord> = expenseList
    fun getCustomerList(): MutableList<CustomerAccount> = customerList
    fun getTotalCashCollected(): Double = totalCashCollected
    fun getTotalRealProfit(): Double = totalRealProfit
    fun setTotalCashCollected(value: Double) { totalCashCollected = value; saveCustomerAndFinancials(); autoSyncToCloud() }
    fun setTotalRealProfit(value: Double) { totalRealProfit = value; saveCustomerAndFinancials(); autoSyncToCloud() }
    fun getTotalExpenses(): Double = expenseList.sumOf { it.amount }
    fun getNetRealProfit(): Double = totalRealProfit - getTotalExpenses()
    fun getTotalInventoryCost(): Double = productList.sumOf { it.purchasePrice * it.stockQuantity }

    fun exportToJson(): String {
        return try {
            val root = JSONObject()
            root.put("totalCash", totalCashCollected)
            root.put("totalProfit", totalRealProfit)
            val pArr = JSONArray()
            for (p in productList) {
                pArr.put(JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("barcode", p.barcode)
                        put("purchasePrice", p.purchasePrice)
                        put("sellingPrice", p.sellingPrice)
                        put("stockQuantity", p.stockQuantity)
                        put("category", p.category)
                        put("unitType", p.unitType)
                        put("imageUrl", p.imageUrl)
                        put("wholesalePrice", p.wholesalePrice)
                        put("minStockAlert", p.minStockAlert)
                })
            }
            root.put("products", pArr)
            root.toString()
        } catch (e: Exception) { "{}" }
    }

    fun importFromJson(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            val pArr = root.optJSONArray("products")
            if (pArr != null) {
                val list = mutableListOf<Product>()
                for (i in 0 until pArr.length()) {
                    val o = pArr.getJSONObject(i)
                    list.add(
                        Product(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            name = o.optString("name", ""),
                            barcode = o.optString("barcode", ""),
                            purchasePrice = o.optDouble("purchasePrice", o.optDouble("purchase", 0.0)),
                            sellingPrice = o.optDouble("sellingPrice", o.optDouble("sell", 0.0)),
                            stockQuantity = o.optDouble("stockQuantity", o.optDouble("qty", 0.0)),
                            category = o.optString("category", "عام"),
                            unitType = o.optString("unitType", "قطعة"),
                            imageUrl = o.optString("imageUrl", ""),
                            wholesalePrice = o.optDouble("wholesalePrice", 0.0),
                            minStockAlert = o.optDouble("minStockAlert", 5.0)
                        )
                    )
                }
                addProducts(list)
            }
            true
        } catch (e: Exception) { false }
    }

    fun syncToFirebase(storeId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        try {
            if (storeId.isBlank()) { onFailure("يجب إدخال كود المتجر!"); return }
            val database = FirebaseDatabase.getInstance().getReference("Kasebo_Stores").child(storeId)
            // 🛠️ التحديث الآمن: استخدام updateChildren بدلاً من setValue
            val updates = hashMapOf<String, Any>(
                "totalCash" to totalCashCollected,
                "totalProfit" to totalRealProfit,
                "products" to productList.toList(),
                "customers" to customerList.toList(),
                "expenses" to expenseList.toList(),
                "sales" to salesHistory.toList()
            )
            database.updateChildren(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.message ?: "خطأ") }
        } catch (e: Exception) { onFailure(e.message ?: "خطأ غير متوقع") }
    }

    fun fetchDataFromFirebase(storeId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val cleanStoreId = storeId.trim()
        if (cleanStoreId.isBlank()) { onFailure("يرجى إدخال كود المتجر!"); return }

        val database = FirebaseDatabase.getInstance().getReference("Kasebo_Stores").child(cleanStoreId)
        database.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                try {
                    totalCashCollected = snapshot.child("totalCash").value?.toString()?.toDoubleOrNull() ?: 0.0
                    totalRealProfit = snapshot.child("totalProfit").value?.toString()?.toDoubleOrNull() ?: 0.0

                    productList.clear()
                    val productsSnap = snapshot.child("products")
                    val newProds = mutableListOf<Product>()
                    for (pChild in productsSnap.children) {
                        try {
                            val id = pChild.child("id").value?.toString() ?: pChild.key ?: ""
                            val name = pChild.child("name").value?.toString() ?: ""
                            val barcode = pChild.child("barcode").value?.toString() ?: ""
                            val pPrice = pChild.child("purchasePrice").value?.toString()?.toDoubleOrNull() ?: 0.0
                            val sPrice = pChild.child("sellingPrice").value?.toString()?.toDoubleOrNull() ?: 0.0
                            val stockQty = pChild.child("stockQuantity").value?.toString()?.toDoubleOrNull() ?: 0.0
                            val category = pChild.child("category").value?.toString() ?: "عام"
                            val unitType = pChild.child("unitType").value?.toString() ?: "قطعة"
                            val imageUrl = pChild.child("imageUrl").value?.toString() ?: ""
                            val wPrice = pChild.child("wholesalePrice").value?.toString()?.toDoubleOrNull() ?: 0.0
                            val minAlert = pChild.child("minStockAlert").value?.toString()?.toDoubleOrNull() ?: 5.0

                            if (name.isNotBlank()) {
                                val p = Product(
                                    id = id, name = name, barcode = barcode, purchasePrice = pPrice, sellingPrice = sPrice,
                                    stockQuantity = stockQty, category = category, unitType = unitType, imageUrl = imageUrl,
                                    wholesalePrice = wPrice, minStockAlert = minAlert
                                )
                                productList.add(p)
                                newProds.add(p)
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                    dbHelper.insertBatchProducts(newProds)
                    saveCustomerAndFinancials()
                    onSuccess()
                } catch (e: Exception) { onFailure("خطأ أثناء معالجة البيانات: ${e.message}") }
            } else { onFailure("لم يتم العثور على متجر بهذا الكود في السحابة!") }
        }.addOnFailureListener { e -> onFailure("فشل الاتصال بالسحابة: ${e.message}") }
    }
}
