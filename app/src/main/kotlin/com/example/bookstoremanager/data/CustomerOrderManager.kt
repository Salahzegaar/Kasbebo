package com.example.bookstoremanager.data

import android.content.Context
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CustomerOrderManager(private val context: Context? = null) {

    // 🛒 إرسال الطلب من الزبون إلى قاعدة البيانات
    fun placeOrder(storeId: String, order: CustomerOrder, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("Kasebo_Stores")
            .child(storeId)
            .child("orders")
        
        val orderId = "ORD-" + System.currentTimeMillis().toString().takeLast(6)
        val cleanOrder = order.copy(
            orderId = orderId, 
            storeId = storeId,
            customerPhone = order.customerPhone.trim(),
            status = "New"
        )

        databaseRef.child(orderId).setValue(cleanOrder)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.message ?: "خطأ في إرسال الطلب") }
    }

    // 🔔 الاستماع لطلبات المتجر
    fun listenToStoreOrders(storeId: String, onOrdersUpdate: (List<CustomerOrder>) -> Unit) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("Kasebo_Stores")
            .child(storeId)
            .child("orders")

        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ordersList = mutableListOf<CustomerOrder>()
                for (child in snapshot.children) {
                    val order = child.getValue(CustomerOrder::class.java)
                    if (order != null) {
                        ordersList.add(order)
                    }
                }
                onOrdersUpdate(ordersList.sortedByDescending { it.timestamp })
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 🔍 الاستماع لطلبات الزبون برقم الهاتف
    fun listenToCustomerSpecificOrders(storeId: String, customerPhone: String, onOrdersUpdate: (List<CustomerOrder>) -> Unit) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("Kasebo_Stores")
            .child(storeId)
            .child("orders")

        val cleanPhone = customerPhone.trim()

        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ordersList = mutableListOf<CustomerOrder>()
                for (child in snapshot.children) {
                    val order = child.getValue(CustomerOrder::class.java)
                    if (order != null && order.customerPhone.trim() == cleanPhone) {
                        ordersList.add(order)
                    }
                }
                onOrdersUpdate(ordersList.sortedByDescending { it.timestamp })
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 🔄 تحديث حالة الطلب مع التأكد من عدم تكرار خصم المخزون أو الفواتير نهائياً
    fun updateOrderStatus(storeId: String, orderId: String, newStatus: String, onSuccess: () -> Unit) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("Kasebo_Stores").child(storeId)
        
        databaseRef.get().addOnSuccessListener { fullSnapshot ->
            val productsSnap = fullSnapshot.child("products")
            val targetOrderSnap = fullSnapshot.child("orders").child(orderId)
            
            val currentStatus = targetOrderSnap.child("status").value?.toString() ?: ""
            
            val isAlreadyProcessed = targetOrderSnap.child("isProcessed").value as? Boolean ?: (currentStatus == "Delivered")
            
            val shouldProcess = !isAlreadyProcessed && (newStatus == "Processing" || newStatus == "Delivered")

            val updatedProductsList = mutableListOf<Product>()
            val newSalesList = mutableListOf<SaleRecord>()
            var addedCash = 0.0
            var addedProfit = 0.0

            for (pChild in productsSnap.children) {
                val id = pChild.child("id").value?.toString() ?: pChild.key ?: ""
                val name = pChild.child("name").value?.toString() ?: ""
                val barcode = pChild.child("barcode").value?.toString() ?: ""
                val purchase = pChild.child("purchasePrice").value?.toString()?.toDoubleOrNull()
                    ?: pChild.child("purchase").value?.toString()?.toDoubleOrNull() ?: 0.0
                val sell = pChild.child("sellingPrice").value?.toString()?.toDoubleOrNull()
                    ?: pChild.child("sell").value?.toString()?.toDoubleOrNull() ?: 0.0
                var qty = pChild.child("stockQuantity").value?.toString()?.toDoubleOrNull()
                    ?: pChild.child("qty").value?.toString()?.toDoubleOrNull() ?: 0.0
                val cat = pChild.child("category").value?.toString()
                    ?: pChild.child("cat").value?.toString() ?: "عام"
                val unit = pChild.child("unitType").value?.toString()
                    ?: pChild.child("unit").value?.toString() ?: "قطعة"
                val wPrice = pChild.child("wholesalePrice").value?.toString()?.toDoubleOrNull() ?: 0.0
                val minAlert = pChild.child("minStockAlert").value?.toString()?.toDoubleOrNull() ?: 5.0
                val imgUrl = pChild.child("imageUrl").value?.toString() ?: ""

                if (shouldProcess) {
                    for (itemSnap in targetOrderSnap.child("items").children) {
                        val pId = itemSnap.child("productId").value?.toString() ?: ""
                        val itemQty = itemSnap.child("quantity").value?.toString()?.toDoubleOrNull() ?: 0.0
                        val itemPrice = itemSnap.child("price").value?.toString()?.toDoubleOrNull() ?: sell

                        if (pId == id && itemQty > 0.0) {
                            qty = (qty - itemQty).coerceAtLeast(0.0)
                            val rev = itemPrice * itemQty
                            val prof = rev - (purchase * itemQty)
                            addedCash += rev
                            addedProfit += prof
                            newSalesList.add(SaleRecord(name, itemQty, rev, prof, System.currentTimeMillis()))
                        }
                    }
                }
                
                // 💡 تصحيح الخطأ: استخدام updatedProductsList والتأكد من إرسال كافة متغيرات Product المضافة حديثاً
                updatedProductsList.add(
                    Product(
                        id = id,
                        name = name,
                        barcode = barcode,
                        purchasePrice = purchase,
                        sellingPrice = sell,
                        wholesalePrice = wPrice, // الحقل الجديد
                        stockQuantity = qty,
                        minStockAlert = minAlert, // الحقل الجديد
                        category = cat,
                        unitType = unit,
                        imageUrl = imgUrl // الحقل الجديد
                    )
                )
            }

            val updates = HashMap<String, Any>()
            updates["orders/$orderId/status"] = newStatus
            
            if (shouldProcess) {
                updates["orders/$orderId/isProcessed"] = true
                updates["products"] = updatedProductsList
                val oldCash = fullSnapshot.child("totalCash").value?.toString()?.toDoubleOrNull() ?: 0.0
                val oldProfit = fullSnapshot.child("totalProfit").value?.toString()?.toDoubleOrNull() ?: 0.0
                updates["totalCash"] = oldCash + addedCash
                updates["totalProfit"] = oldProfit + addedProfit
                
                val existingSales = mutableListOf<SaleRecord>()
                for (sChild in fullSnapshot.child("sales").children) {
                    val sName = sChild.child("productName").value?.toString() 
                        ?: sChild.child("name").value?.toString() ?: ""
                    val sQty = sChild.child("quantity").value?.toString()?.toDoubleOrNull() 
                        ?: sChild.child("qty").value?.toString()?.toDoubleOrNull() ?: 0.0
                    val sRev = sChild.child("revenue").value?.toString()?.toDoubleOrNull() 
                        ?: sChild.child("rev").value?.toString()?.toDoubleOrNull() ?: 0.0
                    val sProf = sChild.child("profit").value?.toString()?.toDoubleOrNull() 
                        ?: sChild.child("prof").value?.toString()?.toDoubleOrNull() ?: 0.0
                    val sTs = sChild.child("timestamp").value?.toString()?.toLongOrNull() ?: System.currentTimeMillis()
                    
                    if (sName.isNotBlank()) {
                        existingSales.add(SaleRecord(sName, sQty, sRev, sProf, sTs))
                    }
                }
                existingSales.addAll(0, newSalesList)
                updates["sales"] = existingSales
            }

            databaseRef.updateChildren(updates).addOnSuccessListener { onSuccess() }
        }
    }
}
