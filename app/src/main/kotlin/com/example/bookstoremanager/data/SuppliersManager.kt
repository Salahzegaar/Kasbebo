package com.example.bookstoremanager.data

import android.content.Context

// تحديث احترافي للكلاس ليشمل كل معلومات المورد
class SupplierAccount(
    val id: String = "",
    var code: String = "",              // رمز المورد
    var name: String = "",              // الاسم
    var activity: String = "",          // النشاط
    var phone: String = "",             // الهاتف
    var fax: String = "",               // الفاكس
    var state: String = "",             // الولاية
    var city: String = "",              // المدينة
    var address: String = "",           // العنوان
    var email: String = "",             // البريد الإلكتروني
    var commercialRegister: String = "",// السجل التجاري
    var nis: String = "",               // رقم NIS
    var nif: String = "",               // رقم NIF
    var article: String = "",           // المادة
    val records: MutableList<DebtRecord> = mutableListOf()
) {
    fun getTotalReceivable() = records.filter { it.isReceivable }.sumOf { it.amount }
    fun getTotalPayable() = records.filter { !it.isReceivable }.sumOf { it.amount }
    fun getNetBalance() = getTotalReceivable() - getTotalPayable()
}

class SuppliersManager(private val context: Context? = null) {
    private val suppliersList = mutableListOf<SupplierAccount>()

    fun getSuppliersList(): List<SupplierAccount> = suppliersList

    fun addSupplier(supplier: SupplierAccount) {
        val index = suppliersList.indexOfFirst { it.id == supplier.id }
        if (index != -1) suppliersList[index] = supplier else suppliersList.add(supplier)
    }

    fun removeSupplier(supplierId: String) {
        suppliersList.removeIf { it.id == supplierId }
    }
}
