package com.example.bookstoremanager.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.bookstoremanager.* // استيراد للملفات العامة إن وُجدت

class LocalDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "kasebo_local.db"
        // 🛠️ تم رفع الإصدار من 1 إلى 2 لتطبيق التحديث بدون حذف البيانات القديمة
        private const val DATABASE_VERSION = 2

        // أسماء الجداول
        private const val TABLE_PRODUCTS = "products"
        private const val TABLE_SALES = "sales"
        private const val TABLE_EXPENSES = "expenses"

        // أعمدة جدول المنتجات
        private const val COL_ID = "id"
        private const val COL_NAME = "name"
        private const val COL_BARCODE = "barcode"
        private const val COL_PURCHASE = "purchasePrice"
        private const val COL_SELL = "sellingPrice"
        private const val COL_STOCK = "stockQuantity"
        private const val COL_CATEGORY = "category"
        private const val COL_UNIT = "unitType"
        private const val COL_IMAGE = "imageUrl"

        // 🛠️ الحقول الجديدة التي تمت إضافتها لتطابق السحابة
        private const val COL_WHOLESALE = "wholesalePrice"
        private const val COL_MIN_ALERT = "minStockAlert"

        // أعمدة جدول المبيعات
        private const val COL_SALE_NAME = "productName"
        private const val COL_SALE_QTY = "quantity"
        private const val COL_SALE_REV = "revenue"
        private const val COL_SALE_PROFIT = "profit"
        private const val COL_SALE_TIME = "timestamp"

        // أعمدة جدول المصروفات
        private const val COL_EXP_ID = "id"
        private const val COL_EXP_TITLE = "title"
        private const val COL_EXP_AMOUNT = "amount"
        private const val COL_EXP_TIME = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createProductsTable = ("CREATE TABLE $TABLE_PRODUCTS ("
            + "$COL_ID TEXT PRIMARY KEY,"
            + "$COL_NAME TEXT,"
            + "$COL_BARCODE TEXT,"
            + "$COL_PURCHASE REAL,"
            + "$COL_SELL REAL,"
            + "$COL_STOCK REAL,"
            + "$COL_CATEGORY TEXT,"
            + "$COL_UNIT TEXT,"
            + "$COL_IMAGE TEXT,"
            + "$COL_WHOLESALE REAL,"
            + "$COL_MIN_ALERT REAL)")
        db.execSQL(createProductsTable)

        val createSalesTable = ("CREATE TABLE $TABLE_SALES ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "$COL_SALE_NAME TEXT,"
            + "$COL_SALE_QTY REAL,"
            + "$COL_SALE_REV REAL,"
            + "$COL_SALE_PROFIT REAL,"
            + "$COL_SALE_TIME INTEGER)")
        db.execSQL(createSalesTable)

        val createExpensesTable = ("CREATE TABLE $TABLE_EXPENSES ("
            + "$COL_EXP_ID TEXT PRIMARY KEY,"
            + "$COL_EXP_TITLE TEXT,"
            + "$COL_EXP_AMOUNT REAL,"
            + "$COL_EXP_TIME INTEGER)")
        db.execSQL(createExpensesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 🛠️ الإصلاح الجذري: إبقاء البيانات القديمة وإضافة الأعمدة الجديدة فقط
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE_PRODUCTS ADD COLUMN $COL_WHOLESALE REAL DEFAULT 0.0")
                db.execSQL("ALTER TABLE $TABLE_PRODUCTS ADD COLUMN $COL_MIN_ALERT REAL DEFAULT 5.0")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ==========================================
    // العمليات الخاصة بالمنتجات
    // ==========================================
    fun insertOrUpdateProduct(product: Product) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_ID, product.id)
            put(COL_NAME, product.name)
            put(COL_BARCODE, product.barcode)
            put(COL_PURCHASE, product.purchasePrice)
            put(COL_SELL, product.sellingPrice)
            put(COL_STOCK, product.stockQuantity)
            put(COL_CATEGORY, product.category)
            put(COL_UNIT, product.unitType)
            put(COL_IMAGE, product.imageUrl)
            put(COL_WHOLESALE, product.wholesalePrice)
            put(COL_MIN_ALERT, product.minStockAlert)
        }
        db.insertWithOnConflict(TABLE_PRODUCTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    fun insertBatchProducts(products: List<Product>) {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            for (product in products) {
                val values = ContentValues().apply {
                    put(COL_ID, product.id)
                    put(COL_NAME, product.name)
                    put(COL_BARCODE, product.barcode)
                    put(COL_PURCHASE, product.purchasePrice)
                    put(COL_SELL, product.sellingPrice)
                    put(COL_STOCK, product.stockQuantity)
                    put(COL_CATEGORY, product.category)
                    put(COL_UNIT, product.unitType)
                    put(COL_IMAGE, product.imageUrl)
                    put(COL_WHOLESALE, product.wholesalePrice)
                    put(COL_MIN_ALERT, product.minStockAlert)
                }
                db.insertWithOnConflict(TABLE_PRODUCTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    fun getAllProducts(): List<Product> {
        val list = mutableListOf<Product>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_PRODUCTS", null)
        if (cursor.moveToFirst()) {
            do {
                val wholesaleIdx = cursor.getColumnIndex(COL_WHOLESALE)
                val minAlertIdx = cursor.getColumnIndex(COL_MIN_ALERT)

                val product = Product(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(COL_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                    barcode = cursor.getString(cursor.getColumnIndexOrThrow(COL_BARCODE)),
                    purchasePrice = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PURCHASE)),
                    sellingPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_SELL)),
                    stockQuantity = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_STOCK)),
                    category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)),
                    unitType = cursor.getString(cursor.getColumnIndexOrThrow(COL_UNIT)),
                    imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE)),
                    // 🛠️ استخراج الحقول الجديدة بأمان
                    wholesalePrice = if (wholesaleIdx != -1) cursor.getDouble(wholesaleIdx) else 0.0,
                    minStockAlert = if (minAlertIdx != -1) cursor.getDouble(minAlertIdx) else 5.0
                )
                list.add(product)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun deleteProduct(id: String) {
        val db = this.writableDatabase
        db.delete(TABLE_PRODUCTS, "$COL_ID=?", arrayOf(id))
        db.close()
    }

    // ==========================================
    // العمليات الخاصة بالمبيعات والمصروفات
    // ==========================================
    fun insertSale(sale: SaleRecord) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_SALE_NAME, sale.productName)
            put(COL_SALE_QTY, sale.quantity)
            put(COL_SALE_REV, sale.revenue)
            put(COL_SALE_PROFIT, sale.profit)
            put(COL_SALE_TIME, sale.timestamp)
        }
        db.insert(TABLE_SALES, null, values)
        db.close()
    }

    fun getAllSales(): List<SaleRecord> {
        val list = mutableListOf<SaleRecord>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_SALES ORDER BY $COL_SALE_TIME DESC", null)
        if (cursor.moveToFirst()) {
            do {
                val sale = SaleRecord(
                    productName = cursor.getString(cursor.getColumnIndexOrThrow(COL_SALE_NAME)),
                    quantity = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_SALE_QTY)),
                    revenue = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_SALE_REV)),
                    profit = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_SALE_PROFIT)),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_SALE_TIME))
                )
                list.add(sale)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun insertExpense(expense: ExpenseRecord) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_EXP_ID, expense.id)
            put(COL_EXP_TITLE, expense.title)
            put(COL_EXP_AMOUNT, expense.amount)
            put(COL_EXP_TIME, expense.timestamp)
        }
        db.insertWithOnConflict(TABLE_EXPENSES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    fun deleteExpense(id: String) {
        val db = this.writableDatabase
        db.delete(TABLE_EXPENSES, "$COL_EXP_ID=?", arrayOf(id))
        db.close()
    }

    fun getAllExpenses(): List<ExpenseRecord> {
        val list = mutableListOf<ExpenseRecord>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_EXPENSES ORDER BY $COL_EXP_TIME DESC", null)
        if (cursor.moveToFirst()) {
            do {
                val exp = ExpenseRecord(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXP_ID)),
                    title = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXP_TITLE)),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_EXP_AMOUNT)),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_EXP_TIME))
                )
                list.add(exp)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun resetDaily() {
        val db = this.writableDatabase
        db.delete(TABLE_SALES, null, null)
        db.delete(TABLE_EXPENSES, null, null)
        db.close()
    }
}
