package com.example.bookstoremanager.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.UUID

class AuthManager(private val context: Context) {

    private val dbRef = FirebaseDatabase.getInstance().reference
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    // 🚀 توليد كود متجر فريد وأكثر أماناً (مضاد للتخمين - Brute-force)
    fun generateUniqueStoreCode(): String {
        val uuidPart = UUID.randomUUID().toString().substring(0, 8).uppercase()
        return "KSB-$uuidPart"
    }

    // 📝 إنشاء حساب جديد لأول مرة وتوليد كود متجر فريد
    fun registerNewStore(
        email: String,
        pass: String,
        onSuccess: (storeCode: String) -> Unit,
        onFailure: (errorMessage: String) -> Unit
    ) {
        val cleanEmail = email.trim()
        val generatedStoreCode = generateUniqueStoreCode()
        val sanitizedEmailKey = cleanEmail.lowercase().replace(".", "_").replace("@", "_")

        // 1️⃣ التسجيل الفعلي في قائمة Firebase Authentication (الآمنة والمشفرة)
        auth.createUserWithEmailAndPassword(cleanEmail, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""

                    // 2️⃣ إعداد بيانات المستخدم والمتجر (بدون كلمة المرور! 🛑)
                    val userData = hashMapOf<String, Any>(
                        "email" to cleanEmail,
                        "storeCode" to generatedStoreCode,
                        "uid" to uid,
                        "createdTimestamp" to System.currentTimeMillis()
                    )

                    val storeData = hashMapOf<String, Any>(
                        "storeUserEmail" to cleanEmail,
                        "storeCode" to generatedStoreCode,
                        "ownerUid" to uid,
                        "createdTimestamp" to System.currentTimeMillis()
                    )

                    // 3️⃣ الحفظ في Realtime Database تحت Kasebo_Users و Kasebo_Stores
                    dbRef.child("Kasebo_Users").child(sanitizedEmailKey).setValue(userData)
                    dbRef.child("Kasebo_Stores").child(generatedStoreCode).setValue(storeData)
                        .addOnSuccessListener {
                            saveSessionLocally(generatedStoreCode)
                            onSuccess(generatedStoreCode)
                        }
                        .addOnFailureListener { e ->
                            onFailure(e.message ?: "حدث خطأ أثناء حفظ بيانات المتجر")
                        }
                } else {
                    onFailure(task.exception?.message ?: "فشل إنشاء الحساب، تأكد من صحة البيانات أو قوة كلمة المرور.")
                }
            }
    }

    // 🔑 تسجيل الدخول بالبريد الإلكتروني وكلمة السر (آمن 100%)
    fun loginStore(
        email: String,
        pass: String,
        inventoryManager: InventoryManager,
        onSuccess: (storeCode: String) -> Unit,
        onFailure: (errorMessage: String) -> Unit
    ) {
        val cleanEmail = email.trim()
        val sanitizedEmailKey = cleanEmail.lowercase().replace(".", "_").replace("@", "_")

        if (cleanEmail.isBlank() || pass.isBlank()) {
            onFailure("يرجى إدخال البريد الإلكتروني وكلمة السر!")
            return
        }

        // 1️⃣ التحقق من الحساب في Firebase Auth (هو من يتأكد من صحة كلمة المرور سرياً)
        auth.signInWithEmailAndPassword(cleanEmail, pass)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    // 2️⃣ البحث عن كود المتجر المرتبط بهذا البريد
                    dbRef.child("Kasebo_Users").child(sanitizedEmailKey).get()
                        .addOnSuccessListener { snapshot ->
                            if (snapshot.exists()) {
                                val foundStoreCode = snapshot.child("storeCode").getValue(String::class.java) ?: ""

                                if (foundStoreCode.isNotBlank()) {
                                    // 3️⃣ جلب بيانات المتجر بالكامل بناءً على الكود
                                    inventoryManager.fetchDataFromFirebase(
                                        storeId = foundStoreCode,
                                        onSuccess = {
                                            saveSessionLocally(foundStoreCode)
                                            onSuccess(foundStoreCode)
                                        },
                                        onFailure = {
                                            // قد يفشل الجلب لأسباب في الشبكة، لكن المتجر موجود، نسجل الدخول
                                            saveSessionLocally(foundStoreCode)
                                            onSuccess(foundStoreCode)
                                        }
                                    )
                                } else {
                                    onFailure("لم يتم العثور على كود متجر مرتبط بهذا الحساب!")
                                }
                            } else {
                                onFailure("بيانات المستخدم غير مكتملة في قاعدة البيانات.")
                            }
                        }
                        .addOnFailureListener { e ->
                            onFailure(e.message ?: "فشل الاتصال بقاعدة البيانات")
                        }
                } else {
                    // Firebase Auth هو من يرفض الدخول إذا كانت الكلمة خاطئة
                    onFailure("البريد الإلكتروني أو كلمة المرور غير صحيحة.")
                }
            }
    }

    // 🔎 البحث عن متجر التاجر المخصص لتصفح الزبائن
    fun searchMerchantStore(
        storeCode: String,
        onFound: () -> Unit,
        onNotFound: () -> Unit
    ) {
        val cleanCode = storeCode.trim().uppercase()
        dbRef.child("Kasebo_Stores").child(cleanCode).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    onFound()
                } else {
                    onNotFound()
                }
            }
            .addOnFailureListener {
                onNotFound()
            }
    }

    // 💾 حفظ جلسة الدخول محلياً على الجهاز
    private fun saveSessionLocally(storeCode: String) {
        val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("store_secret_id", storeCode).apply()
    }

    // 🚪 تسجيل الخروج من الحساب
    fun logout() {
        auth.signOut()
        val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)
        prefs.edit().remove("store_secret_id").apply()
    }
}
