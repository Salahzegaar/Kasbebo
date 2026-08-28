package com.example.bookstoremanager

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

// استيراد الحزم الأساسية
import com.example.bookstoremanager.data.*
import com.example.bookstoremanager.ui.*

class MainActivity : ComponentActivity() {
    private val inventoryManager by lazy { InventoryManager(this) }
    private val authManager by lazy { AuthManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🛡️ طلب الأذونات المتقدمة (الكاميرا، الإشعارات، والبلوتوث للطابعات)
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 100)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
            if (permissions.any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) {
                requestPermissions(permissions, 102)
            }
        }

        setContent {
            val prefs = getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)
            var currentStoreId by remember { mutableStateOf(prefs.getString("store_secret_id", "") ?: "") }

            var isDark by remember { mutableStateOf(prefs.getBoolean("is_dark_mode", true)) }
            var isLoggedIn by remember { mutableStateOf(currentStoreId.isNotBlank()) }
            var customerBrowsingStoreId by remember { mutableStateOf<String?>(null) }
            var refreshTrigger by remember { mutableStateOf(0) }
            var newlyCreatedStoreCode by remember { mutableStateOf<String?>(null) }

            // ☁️ بمجرد الدخول، الاستماع للطلبات وجلب رمز الإشعارات (FCM Token)
            LaunchedEffect(currentStoreId, isLoggedIn) {
                if (currentStoreId.isNotBlank() && isLoggedIn) {
                    inventoryManager.listenForIncomingCustomerOrders(
                        storeId = currentStoreId,
                        onOrdersUpdated = { _ ->
                            refreshTrigger++
                            inventoryManager.fetchDataFromFirebase(
                                storeId = currentStoreId,
                                onSuccess = { refreshTrigger++ },
                                onFailure = { }
                            )
                        }
                    )

                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val token = task.result
                            if (!token.isNullOrBlank()) {
                                FirebaseDatabase.getInstance().getReference("Kasebo_Stores")
                                .child(currentStoreId)
                                .child("fcmToken")
                                .setValue(token)
                            }
                        }
                    }
                }
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)) {

                    // 🚀 البحث عن التحديثات الإجبارية
                    CheckForAppUpdates(isDark = isDark)

                    // 🎉 إظهار كود المتجر للمستخدم الجديد
                    if (newlyCreatedStoreCode != null) {
                        NewStoreWelcomeDialog(
                            storeCode = newlyCreatedStoreCode!!,
                            isDark = isDark,
                            onDismiss = {
                                newlyCreatedStoreCode = null
                                isLoggedIn = true
                                refreshTrigger++
                            }
                        )
                    }

                    if (customerBrowsingStoreId != null) {
                        CustomerShopView(
                            storeId = customerBrowsingStoreId!!,
                            inventoryManager = inventoryManager,
                            isDark = isDark,
                            onClose = { customerBrowsingStoreId = null }
                        )
                    } else if (!isLoggedIn) {
                        LoginAuthScreen(
                            isDark = isDark,
                            onLoginSuccess = { result ->
                                currentStoreId = prefs.getString("store_secret_id", "") ?: ""
                                isLoggedIn = true
                            },
                            onCustomerModeOpen = { storeId ->
                                customerBrowsingStoreId = storeId.toString()
                            }
                        )
                    } else {
                        StoreManagerApp(
                            inventoryManager = inventoryManager,
                            isDarkMode = isDark,
                            onThemeChange = { newTheme ->
                                isDark = newTheme
                                prefs.edit().putBoolean("is_dark_mode", newTheme).apply()
                            },
                            onDataChanged = { refreshTrigger++ }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 🚀 نظام جلب تحديثات كاسبو تلقائياً
// ==========================================
@Composable
fun CheckForAppUpdates(isDark: Boolean) {
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateUrl by remember { mutableStateOf("") }
    val context = LocalContext.current

    val currentVersionCode = try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }
    } catch (e: Exception) {
        1
    }

    LaunchedEffect(Unit) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("Kasebo_App_Update")
        databaseRef.get().addOnSuccessListener { snapshot ->
            val latestVersion = snapshot.child("LatestVersion").getValue(Int::class.java) ?: currentVersionCode
            updateUrl = snapshot.child("updateUrl").getValue(String::class.java) ?: ""

            if (latestVersion > currentVersionCode && updateUrl.isNotBlank()) {
                showUpdateDialog = true
            }
        }
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = AppTheme.card(isDark),
            title = { Text("🚀 توفر تحديث جديد لتطبيق كاسبو", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("يتوفر إصدار جديد من التطبيق يحتوي على تحسينات هامة للأمان والمزامنة. يرجى التحديث الآن.", color = AppTheme.subText(isDark), fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "الرابط غير صالح!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.GoldPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("تحميل التحديث الآن 📥", color = AppTheme.GoldPrimaryText, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            }
        )
    }
}

// ==========================================
// 🎉 نافذة الترحيب بالمتجر الجديد
// ==========================================
@Composable
fun NewStoreWelcomeDialog(
    storeCode: String,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { },
        containerColor = AppTheme.card(isDark),
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("🎉 تم إنشاء متجرك بنجاح!", color = AppTheme.GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(4.dp))
                Text("احفظ كود متجرك السري", color = AppTheme.text(isDark), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                    .fillMaxWidth()
                    .background(AppTheme.inputBg(isDark), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = storeCode,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppTheme.GoldPrimary,
                        letterSpacing = 2.sp
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("💡 لماذا هذا الكود مهم جداً؟", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("1️⃣ هو مفتاح المزامنة للوصول لمتجرك من أجهزة أخرى.", color = AppTheme.subText(isDark), fontSize = 12.sp)
                    Text("2️⃣ يمكنك إعطاؤه لزبائنك لطلب المنتجات والتسوق إلكترونياً.", color = AppTheme.subText(isDark), fontSize = 12.sp)
                    Text("3️⃣ يلزمك لإعادة فتح حسابك في حال تم تغيير الهاتف.", color = AppTheme.subText(isDark), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("StoreCode", storeCode)
                        clipboard.setPrimaryClip(clip)

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "🔑 كود متجري في تطبيق كاسبو Kasebo هو: $storeCode\nاستخدم هذا الكود للبحث والتسوق مباشرة."
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "حفظ/مشاركة كود المتجر عبر..."))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.GoldPrimary)
                ) {
                    Text("نسخ الكود ومشاركته 📤", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.inputBg(isDark))
                ) {
                    Text("فهمت ذلك، الدخول للمتجر 🚀", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}
