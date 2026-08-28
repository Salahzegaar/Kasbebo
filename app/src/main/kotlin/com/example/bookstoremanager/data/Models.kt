package com.example.bookstoremanager.data

import java.util.Locale

// 📦 نموذج بيانات المنتج
data class Product(
    val id: String = "",
    var name: String = "",
    var barcode: String = "",
    var purchasePrice: Double = 0.0,
    var sellingPrice: Double = 0.0,
    var wholesalePrice: Double = 0.0,
    var stockQuantity: Double = 0.0,
    var minStockAlert: Double = 5.0,
    var category: String = "عام",
    var unitType: String = "قطعة",
    var imageUrl: String = ""
) {
    fun calculateUnitProfit(): Double {
        return sellingPrice - purchasePrice
    }
}

// 🚀 سجل المبيعات
data class SaleRecord(
    val productName: String = "",
    val quantity: Double = 0.0,
    val revenue: Double = 0.0,
    val profit: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

// 💸 سجل المصروفات
data class ExpenseRecord(
    val id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

// 📑 سجل الديون والالتزامات
data class DebtRecord(
    val id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val isReceivable: Boolean = true
)

// 🛒 عناصر سلة الشراء
data class CartItem(
    val product: Product = Product(),
    var quantity: Double = 1.0
) {
    fun getTotalPrice() = product.sellingPrice * quantity
}

// ==========================================
// 📦 كلاسات الطلبات السحابية
// ==========================================
data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val price: Double = 0.0,
    val quantity: Double = 0.0
) {
    fun getTotalPrice() = price * quantity
}

data class CustomerOrder(
    val orderId: String = "",
    val storeId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val items: List<OrderItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val status: String = "جديدة",
    val timestamp: Long = System.currentTimeMillis()
)

// 👤 كلاس حسابات العملاء
class CustomerAccount(
    val id: String = "",
    var name: String = "",
    var lastName: String = "",
    var phone: String = "",
    var socialMedia: String = "",
    val records: MutableList<DebtRecord> = mutableListOf()
) {
    fun getFullName() = if (lastName.isNotBlank()) "$name $lastName" else name
    fun getTotalReceivable() = records.filter { it.isReceivable }.sumOf { it.amount }
    fun getTotalPayable() = records.filter { !it.isReceivable }.sumOf { it.amount }
    fun getNetBalance() = getTotalReceivable() - getTotalPayable()
}

// ==========================================
// 🇩🇿 دوال مساعدة لتنسيق الأرقام والعملة
// ==========================================
fun Double.formatClean(): String {
    return if (this % 1.0 == 0.0) {
        this.toLong().toString()
    } else {
        String.format(Locale.getDefault(), "%.2f", this).replace(Regex("\\.?0+$"), "")
    }
}

fun Double.toDzd(): String {
    return "${this.formatClean()} دج"
}
