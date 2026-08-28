package com.example.bookstoremanager.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

import com.example.bookstoremanager.*
import com.example.bookstoremanager.data.*

@Composable
fun SettingsScreen(
    inventoryManager: InventoryManager,
    isDark: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onDataChanged: () -> Unit,
    onBack: () -> Unit
) {
    var activePage by remember { mutableStateOf("menu") }

    BackHandler(enabled = activePage != "menu") { activePage = "menu" }

    when (activePage) {
        "menu" -> SettingsMainMenu(isDark, onBack = onBack, onNavigate = { activePage = it })
        "company" -> CompanyInfoSubScreen(isDark, onBack = { activePage = "menu" })
        "ticket" -> TicketInfoSubScreen(isDark, onBack = { activePage = "menu" })
        "units" -> ListManagerSubScreen("الوحدات ⚖️", "تسيير وحدات المنتجات", "عند التعديل يتم تحديث حقل الوحدة في المنتجات.", "اسم الوحدة", "store_units", "قطعة,كيلو,رطل,لتر,صندوق", isDark, onBack = { activePage = "menu" })
        "categories" -> ListManagerSubScreen("الفئات 🏷️", "تسيير فئات المنتجات", "عند التعديل يتم تحديث فئة المنتجات.", "اسم الفئة", "store_categories", "عام", isDark, onBack = { activePage = "menu" })
        "passwords" -> PasswordsSubScreen(isDark, onBack = { activePage = "menu" })
        "users" -> UsersSubScreen(isDark, onBack = { activePage = "menu" })
        "audit" -> AuditSubScreen(isDark, onBack = { activePage = "menu" })
        "other" -> OtherSettingsSubScreen(
            isDark = isDark,
            inventoryManager = inventoryManager,
            onThemeChange = onThemeChange,
            onDataChanged = onDataChanged,
            onBack = { activePage = "menu" }
        )
        "backup" -> BackupSubScreen(inventoryManager, isDark, onDataChanged, onBack = { activePage = "menu" })
        // 🚀 إضافة توجيه التصفح لشاشة الإعدادات المتقدمة الجديدة
        "advanced_store" -> AdvancedStoreSettingsScreen(isDark = isDark, onBack = { activePage = "menu" })
    }
}

@Composable
fun getSettingsFieldColors(isDark: Boolean) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AppTheme.text(isDark), unfocusedTextColor = AppTheme.text(isDark),
    focusedContainerColor = AppTheme.inputBg(isDark), unfocusedContainerColor = AppTheme.inputBg(isDark),
    focusedBorderColor = AppTheme.GoldPrimary, unfocusedBorderColor = AppTheme.cardBorder(isDark),
    focusedLabelColor = AppTheme.GoldPrimary, unfocusedLabelColor = AppTheme.subText(isDark)
)

// ==========================================
// 1️⃣ القائمة الرئيسية (Main Menu)
// ==========================================
@Composable
fun SettingsMainMenu(isDark: Boolean, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(AppTheme.bg(isDark)).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "عودة", tint = AppTheme.text(isDark)) }
            Text("⚙️ الإعدادات", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
            Spacer(modifier = Modifier.size(48.dp))
        }

        SettingsMenuItem("معلومات الشركة", "إدارة بيانات مؤسستك وسجلك التجاري.", Icons.Default.Business, Color(0xFF3B82F6), isDark) { onNavigate("company") }
        SettingsMenuItem("معلومات التذكرة", "رسالة الترحيب وتذييل التذكرة", Icons.Default.Receipt, Color(0xFFF59E0B), isDark) { onNavigate("ticket") }
        SettingsMenuItem("الوحدات", "يفتح صفحة التسيير المخصصة.", Icons.Default.Scale, Color(0xFFEF4444), isDark) { onNavigate("units") }
        SettingsMenuItem("الفئات", "يفتح صفحة التسيير المخصصة.", Icons.Default.Label, Color(0xFF8B5CF6), isDark) { onNavigate("categories") }
        SettingsMenuItem("كلمات المرور", "تغيير كلمة مرور المستخدمين.", Icons.Default.VpnKey, Color(0xFFF59E0B), isDark) { onNavigate("passwords") }
        SettingsMenuItem("المستخدمون", "إضافة المستخدمين وتحديد صلاحياتهم.", Icons.Default.GroupAdd, Color(0xFFEF4444), isDark) { onNavigate("users") }
        SettingsMenuItem("تدقيق الجرد", "سجل تصحيحات المخزون المؤكدة.", Icons.Default.FactCheck, Color(0xFF3B82F6), isDark) { onNavigate("audit") }
        SettingsMenuItem("النسخ الاحتياطي", "نسخ يدوي وسجل ونسخ تلقائي.", Icons.Default.Storage, Color(0xFFF59E0B), isDark) { onNavigate("backup") }

        // 🚀 عنصر القائمة الجديد لإعدادات المتجر المتقدمة
        SettingsMenuItem("إعدادات المتجر المتقدمة", "المخزون السالب، التنبيهات، نماذج الفواتير.", Icons.Default.SettingsApplications, Color(0xFF14A8EE), isDark) { onNavigate("advanced_store") }

        SettingsMenuItem("إعدادات أخرى", "المظهر، التصفير، والمزيد.", Icons.Default.Tune, Color(0xFFEF4444), isDark) { onNavigate("other") }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun SettingsMenuItem(title: String, subtitle: String, icon: ImageVector, iconTint: Color, isDark: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(85.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)),
        shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, AppTheme.cardBorder(isDark)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(45.dp).clip(RoundedCornerShape(10.dp)).background(iconTint.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark), fontSize = 16.sp)
                Text(subtitle, color = AppTheme.subText(isDark), fontSize = 11.sp)
            }
        }
    }
}

// ==========================================
// 🚀 جديد: شاشة إعدادات المتجر المتقدمة
// ==========================================
@Composable
fun AdvancedStoreSettingsScreen(isDark: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)

    var allowNegativeInventory by remember { mutableStateOf(prefs.getBoolean("allow_negative_inventory", false)) }
    var expiryDays by remember { mutableStateOf(prefs.getInt("expiry_alert_days", 0).toString()) }
    var selectedTemplate by remember { mutableStateOf(prefs.getString("invoice_template", "classic") ?: "classic") }

    val bluePrimary = Color(0xFF14A8EE)
    val grayBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)

    Column(
        modifier = Modifier
        .fillMaxSize()
        .background(AppTheme.bg(isDark))
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // 1️⃣ قسم المخزون السالب
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
            Text("السماح بالبيع بمخزون سالب", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
            Text("يسمح أو يمنع البيع عند عدم كفاية المخزون.", fontSize = 12.sp, color = AppTheme.subText(isDark))

            Spacer(modifier = Modifier.height(16.dp))

            // أزرار OUI / NON
            Row(modifier = Modifier.fillMaxWidth().height(50.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    modifier = Modifier.weight(1f).fillMaxHeight().clickable { allowNegativeInventory = false },
                    shape = RoundedCornerShape(8.dp),
                    color = if (!allowNegativeInventory) grayBg else Color.Transparent,
                    border = BorderStroke(1.dp, if (!allowNegativeInventory) Color.Transparent else AppTheme.cardBorder(isDark))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("NON", fontWeight = FontWeight.Bold, color = if (!allowNegativeInventory) AppTheme.text(isDark) else AppTheme.subText(isDark))
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f).fillMaxHeight().clickable { allowNegativeInventory = true },
                    shape = RoundedCornerShape(8.dp),
                    color = if (allowNegativeInventory) bluePrimary else Color.Transparent,
                    border = BorderStroke(1.dp, if (allowNegativeInventory) Color.Transparent else AppTheme.cardBorder(isDark))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("OUI", fontWeight = FontWeight.Bold, color = if (allowNegativeInventory) Color.White else AppTheme.subText(isDark))
                    }
                }
            }
        }

        // 2️⃣ حقل إدخال أيام الصلاحية
        OutlinedTextField(
            value = expiryDays,
            onValueChange = { expiryDays = it },
            label = { Text("تنبيه أيام الصلاحية", fontSize = 12.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = bluePrimary,
                unfocusedBorderColor = AppTheme.cardBorder(isDark),
                focusedTextColor = AppTheme.text(isDark),
                unfocusedTextColor = AppTheme.text(isDark)
            )
        )

        // 3️⃣ قسم نماذج الفاتورة
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
            Text("نموذج الفاتورة", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
            Spacer(modifier = Modifier.height(16.dp))

            InvoiceTemplateCard(
                title = "عصري", desc = "شريط أزرق ومربعات للزبون والشركة.", isSelected = selectedTemplate == "modern", isDark = isDark,
                onClick = { selectedTemplate = "modern" }, blueColor = bluePrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            InvoiceTemplateCard(
                title = "كلاسيكي", desc = "بسيط بالأبيض والأسود وقريب من الوثائق.", isSelected = selectedTemplate == "classic", isDark = isDark,
                onClick = { selectedTemplate = "classic" }, blueColor = bluePrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            InvoiceTemplateCard(
                title = "مختصر", desc = "أكثر كثافة للفواتير ذات العديد من الأسطر.", isSelected = selectedTemplate == "compact", isDark = isDark,
                onClick = { selectedTemplate = "compact" }, blueColor = bluePrimary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 4️⃣ أزرار الحفظ والرجوع
        Row(modifier = Modifier.fillMaxWidth().height(55.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = grayBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Text("رجوع", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Button(
                onClick = {
                    prefs.edit()
                    .putBoolean("allow_negative_inventory", allowNegativeInventory)
                    .putInt("expiry_alert_days", expiryDays.toIntOrNull() ?: 0)
                    .putString("invoice_template", selectedTemplate)
                    .apply()

                    Toast.makeText(context, "تم حفظ الإعدادات بنجاح! 💾", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = bluePrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Text("حفظ 💾", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun InvoiceTemplateCard(title: String, desc: String, isSelected: Boolean, isDark: Boolean, onClick: () -> Unit, blueColor: Color) {
    val bgColor = if (isSelected) Color.Transparent else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
    val borderColor = if (isSelected) blueColor else Color.Transparent
    val titleColor = if (isSelected) blueColor else AppTheme.text(isDark)

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = titleColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(desc, fontSize = 12.sp, color = AppTheme.subText(isDark))
        }
    }
}

// ==========================================
// 2️⃣ شاشة معلومات التذكرة
// ==========================================
@Composable
fun TicketInfoSubScreen(isDark: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)

    var welcomeMsg by remember { mutableStateOf(prefs.getString("ticket_welcome", "مرحباً بكم في متجرنا") ?: "") }
    var footerMsg by remember { mutableStateOf(prefs.getString("ticket_footer", "شكراً لزيارتكم") ?: "") }
    var socialLinks by remember { mutableStateOf(prefs.getString("ticket_social", "") ?: "") }

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.bg(isDark)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppTheme.text(isDark)) }
            Text("معلومات التذكرة 🎫", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
        }

        Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = welcomeMsg, onValueChange = { welcomeMsg = it }, label = { Text("رسالة الترحيب") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = getSettingsFieldColors(isDark))
                OutlinedTextField(value = footerMsg, onValueChange = { footerMsg = it }, label = { Text("تذييل التذكرة") }, modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(10.dp), colors = getSettingsFieldColors(isDark))
                OutlinedTextField(value = socialLinks, onValueChange = { socialLinks = it }, label = { Text("شبكات التواصل الاجتماعي") }, modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(10.dp), colors = getSettingsFieldColors(isDark))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryAppButton(text = "حفظ 💾", modifier = Modifier.weight(1f), containerColor = Color(0xFF0EA5E9), contentColor = Color.White) {
                        prefs.edit().putString("ticket_welcome", welcomeMsg).putString("ticket_footer", footerMsg).putString("ticket_social", socialLinks).apply()
                        Toast.makeText(context, "تم الحفظ", Toast.LENGTH_SHORT).show(); onBack()
                    }
                    SecondaryAppButton(text = "رجوع", isDark = isDark, modifier = Modifier.weight(1f)) { onBack() }
                }
            }
        }
    }
}
// ==========================================
// 3️⃣ إعدادات أخرى (محدثة مع زر تسجيل الخروج الفعّال 100%)
// ==========================================
@Composable
fun OtherSettingsSubScreen(
    isDark: Boolean,
    inventoryManager: InventoryManager,
    onThemeChange: (Boolean) -> Unit,
    onDataChanged: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)

    var showResetTransactionsDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.bg(isDark)).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppTheme.text(isDark)) }
            Text("إعدادات أخرى 🎛️", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
        }

        Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // المظهر العام
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("المظهر العام", fontWeight = FontWeight.Bold, color = AppTheme.text(isDark)); Text("تغيير ألوان التطبيق", fontSize = 11.sp, color = AppTheme.subText(isDark)) }
                    SecondaryAppButton(text = if (isDark) "نهاري ☀️" else "ليلي 🌙", isDark = isDark) { onThemeChange(!isDark) }
                }

                HorizontalDivider(color = AppTheme.inputBg(isDark), modifier = Modifier.padding(vertical = 8.dp))

                // إدارة البيانات والتصفير
                Text("إدارة البيانات ⚠️", fontWeight = FontWeight.Bold, color = AppTheme.ColorDanger)
                Button(
                    onClick = { showResetTransactionsDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.ColorDanger),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تصفير بيانات المبيعات والمشتريات كاملة", color = Color.White, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = AppTheme.inputBg(isDark), modifier = Modifier.padding(vertical = 8.dp))

                // 🚀 زر تسجيل الخروج
                Text("الحساب 👤", fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, AppTheme.ColorDanger),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = AppTheme.ColorDanger)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسجيل الخروج من الحساب", color = AppTheme.ColorDanger, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // 🔴 نافذة تأكيد تصفير البيانات
    if (showResetTransactionsDialog) {
        AlertDialog(
            onDismissRequest = { showResetTransactionsDialog = false },
            containerColor = AppTheme.card(isDark),
            shape = RoundedCornerShape(16.dp),
            title = { Text("تأكيد التصفير ⚠️", fontWeight = FontWeight.Bold, color = AppTheme.ColorDanger) },
            text = { Text("هل أنت متأكد أنك تريد تصفير جميع المبيعات والمصروفات والأرشيف بالكامل؟\n(لن يتم حذف المنتجات أو العملاء).", color = AppTheme.text(isDark), fontSize = 14.sp, lineHeight = 22.sp) },
                        confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        
                        // 1. تفريغ بيانات الجلسة بالكامل (أضفنا عدة مفاتيح لتغطية أي احتمال)
                        prefs.edit()
                            .putBoolean("is_logged_in", false)
                            .putBoolean("isLoggedIn", false)
                            .putString("logged_in_user", "")
                            .putString("active_role", "")
                            .commit()
                            
                        Toast.makeText(context, "تم تسجيل الخروج بنجاح! 🔒", Toast.LENGTH_SHORT).show()
                        
                        // 2. إجبار التطبيق على هدم الواجهة الحالية وإعادة بنائها من الصفر 
                        // هذا سيجبر MainActivity على قراءة حالة تسجيل الدخول من جديد وإظهار شاشة القفل
                        val activity = context as? android.app.Activity
                        activity?.recreate()
                        
                        // كخيار بديل وقوي في حال لم يستجب Compose لـ recreate
                        val intent = android.content.Intent(context, MainActivity::class.java)
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        context.startActivity(intent)
                        activity?.finishAffinity()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.ColorDanger)
                ) { Text("نعم، خروج", color = Color.White, fontWeight = FontWeight.Bold) }
            },

            dismissButton = { TextButton(onClick = { showResetTransactionsDialog = false }) { Text("إلغاء", color = AppTheme.subText(isDark)) } }
        )
    }

    // 🔴 نافذة تأكيد تسجيل الخروج (بالتحديث النهائي والجذري)
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = AppTheme.card(isDark),
            shape = RoundedCornerShape(16.dp),
            title = { Text("تسجيل الخروج 🚪", fontWeight = FontWeight.Bold, color = AppTheme.text(isDark)) },
            text = { Text("هل أنت متأكد أنك تريد تسجيل الخروج والعودة لشاشة الدخول؟", color = AppTheme.subText(isDark), fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false

                        // 1. مسح حالة تسجيل الدخول من الذاكرة بحفظ فوري
                        prefs.edit()
                        .putBoolean("is_logged_in", false)
                        .putBoolean("is_pin_enabled", true)
                        .putString("logged_in_user", "")
                        .putString("active_role", "")
                        .commit()

                        Toast.makeText(context, "تم تسجيل الخروج بنجاح! 🔒", Toast.LENGTH_SHORT).show()

                        // 2. الانتقال إلى الشاشة الرئيسية بمسار نظيف تماماً ومسح كل الواجهات السابقة
                        val intent = android.content.Intent(context, MainActivity::class.java).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        context.startActivity(intent)

                        // 3. إغلاق الـ Activity الحالية لضمان عدم بقاء أي Compose State
                        (context as? android.app.Activity)?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.ColorDanger)
                ) { Text("نعم، خروج", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("إلغاء", color = AppTheme.subText(isDark)) } }
        )
    }
}

// ==========================================
// 4️⃣ شاشة النسخ الاحتياطي
// ==========================================
@Composable
fun BackupSubScreen(inventoryManager: InventoryManager, isDark: Boolean, onDataChanged: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { try { val json = inventoryManager.exportToJson(); context.contentResolver.openOutputStream(it)?.use { out -> out.write(json.toByteArray()) }; Toast.makeText(context, "تم الحفظ بنجاح!", Toast.LENGTH_LONG).show() } catch (e: Exception) { Toast.makeText(context, "فشل الحفظ!", Toast.LENGTH_SHORT).show() } }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { try { val jsonStr = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() }; if (jsonStr != null && inventoryManager.importFromJson(jsonStr)) { onDataChanged(); Toast.makeText(context, "تم الاستعادة", Toast.LENGTH_LONG).show() } else { Toast.makeText(context, "ملف غير صالح!", Toast.LENGTH_SHORT).show() } } catch (e: Exception) { Toast.makeText(context, "فشل الاستعادة!", Toast.LENGTH_SHORT).show() } }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.bg(isDark)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppTheme.text(isDark)) }
            Text("النسخ الاحتياطي 💾", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
        }
        Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("نسخ احتياطي واستعادة", color = AppTheme.GoldPrimary, fontWeight = FontWeight.Bold)
                PrimaryAppButton(text = "حفظ نسخة احتياطية الآن (تصدير)", modifier = Modifier.fillMaxWidth(), containerColor = Color(0xFF0EA5E9), contentColor = Color.White) { backupLauncher.launch("Kasebo_Backup.json") }
                SecondaryAppButton(text = "استعادة بيانات من ملف (استيراد)", isDark = isDark, modifier = Modifier.fillMaxWidth()) { restoreLauncher.launch(arrayOf("application/json", "*/*")) }
            }
        }
    }
}

// ==========================================
// 5️⃣ شاشة معلومات الشركة
// ==========================================
@Composable
fun CompanyInfoSubScreen(isDark: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val companyManager = remember { CompanyInfoManager(context) }
    val savedInfo = remember { companyManager.getCompanyInfo() }

    var name by remember { mutableStateOf(savedInfo.name) }
    var activity by remember { mutableStateOf(savedInfo.activity) }
    var address by remember { mutableStateOf(savedInfo.address) }
    var phoneFax by remember { mutableStateOf(savedInfo.phoneFax) }
    var rcNumber by remember { mutableStateOf(savedInfo.rcNumber) }
    var nifNumber by remember { mutableStateOf(savedInfo.nifNumber) }
    var articleNumber by remember { mutableStateOf(savedInfo.articleNumber) }
    var logoBase64 by remember { mutableStateOf(savedInfo.logoBase64) }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> uri?.let { logoBase64 = uriToBase64(context, it); Toast.makeText(context, "تم إرفاق الشعار", Toast.LENGTH_SHORT).show() } }

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.bg(isDark)).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppTheme.text(isDark)) }
            Text("معلومات المؤسسة 🏢", color = AppTheme.text(isDark), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), border = BorderStroke(1.dp, AppTheme.cardBorder(isDark)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم المؤسسة") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = getSettingsFieldColors(isDark))
                OutlinedTextField(value = activity, onValueChange = { activity = it }, label = { Text("نشاط المؤسسة") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = getSettingsFieldColors(isDark))
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("العنوان") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = getSettingsFieldColors(isDark))
                OutlinedTextField(value = phoneFax, onValueChange = { phoneFax = it }, label = { Text("الهاتف") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = getSettingsFieldColors(isDark))
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), border = BorderStroke(1.dp, AppTheme.cardBorder(isDark)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = rcNumber, onValueChange = { rcNumber = it }, label = { Text("رقم السجل التجاري") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = getSettingsFieldColors(isDark))
                OutlinedTextField(value = nifNumber, onValueChange = { nifNumber = it }, label = { Text("الرقم الضريبي") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = getSettingsFieldColors(isDark))
                OutlinedTextField(value = articleNumber, onValueChange = { articleNumber = it }, label = { Text("رقم المادة") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = getSettingsFieldColors(isDark))
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), border = BorderStroke(1.dp, AppTheme.cardBorder(isDark)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("شعار المؤسسة", color = AppTheme.GoldPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                Box(modifier = Modifier.size(120.dp).clip(RoundedCornerShape(12.dp)).background(AppTheme.inputBg(isDark)).clickable { imagePickerLauncher.launch("image/*") }, contentAlignment = Alignment.Center) {
                    if (logoBase64.isNotBlank()) Text("تم الشعار ✓", color = AppTheme.ColorSuccess) else Icon(Icons.Default.Image, null, tint = AppTheme.subText(isDark), modifier = Modifier.size(40.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { logoBase64 = "" }, colors = ButtonDefaults.buttonColors(containerColor = AppTheme.ColorDanger)) { Text("حذف") }
                    Button(onClick = { imagePickerLauncher.launch("image/*") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))) { Text("صورة") }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryAppButton(text = "حفظ", modifier = Modifier.weight(1f), containerColor = Color(0xFF0EA5E9), contentColor = Color.White) {
                companyManager.saveCompanyInfo(CompanyInfo(name, activity, address, phoneFax, rcNumber, nifNumber, articleNumber, logoBase64))
                Toast.makeText(context, "تم الحفظ", Toast.LENGTH_SHORT).show(); onBack()
            }
            SecondaryAppButton(text = "رجوع", isDark = isDark, modifier = Modifier.weight(1f)) { onBack() }
        }
        Spacer(Modifier.height(40.dp))
    }
}

// ==========================================
// 6️⃣ شاشة إدارة الفئات والوحدات
// ==========================================
@Composable
fun ListManagerSubScreen(title: String, subtitle: String, desc: String, inputLabel: String, prefsKey: String, defaultItems: String, isDark: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)

    var items by remember { mutableStateOf(prefs.getString(prefsKey, defaultItems)?.split(",")?.filter { it.isNotBlank() } ?: emptyList()) }
    var inputValue by remember { mutableStateOf("") }

    fun saveItems(newList: List<String>) { items = newList; prefs.edit().putString(prefsKey, newList.joinToString(",")).apply() }

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.bg(isDark)).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppTheme.text(isDark)) }
            Text(title, color = AppTheme.text(isDark), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Build, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(8.dp))
                Text(subtitle, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark), fontSize = 16.sp)
                Text(desc, color = AppTheme.subText(isDark), fontSize = 12.sp)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = inputValue, onValueChange = { inputValue = it }, label = { Text(inputLabel) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = getSettingsFieldColors(isDark))
                Button(onClick = { if (inputValue.isNotBlank() && !items.contains(inputValue)) { saveItems(items + inputValue); inputValue = ""; Toast.makeText(context, "تمت الإضافة", Toast.LENGTH_SHORT).show() } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))) { Text("إضافة 💾") }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("القائمة", fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
                    Surface(color = Color(0xFF8B5CF6).copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)) { Text("المجموع ${items.size}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color(0xFF8B5CF6), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(16.dp))
                items.forEachIndexed { index, item ->
                    Card(colors = CardDefaults.cardColors(containerColor = AppTheme.bg(isDark)), border = BorderStroke(1.dp, AppTheme.cardBorder(isDark)), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(item, color = AppTheme.text(isDark), fontWeight = FontWeight.Bold); Text("#${index + 1}", color = AppTheme.subText(isDark)) }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { saveItems(items.filter { it != item }) }, modifier = Modifier.weight(1f).height(35.dp), colors = ButtonDefaults.buttonColors(containerColor = AppTheme.ColorDanger.copy(alpha = 0.2f)), contentPadding = PaddingValues(0.dp)) { Icon(Icons.Default.Delete, null, tint = AppTheme.ColorDanger, modifier = Modifier.size(16.dp)) }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

// ==========================================
// 7️⃣ شاشة كلمات المرور (فعالة 100%)
// ==========================================
@Composable
fun PasswordsSubScreen(isDark: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)

    var usersStr by remember { mutableStateOf(prefs.getString("app_users", "{\"admin\":\"admin\"}") ?: "{\"admin\":\"admin\"}") }

    val usersMap = remember(usersStr) {
        val map = mutableMapOf<String, String>()
        try { val json = JSONObject(usersStr); json.keys().forEach { map[it] = json.getString(it) } } catch (e: Exception) {}
        map
    }

    var selectedUser by remember { mutableStateOf(usersMap.keys.firstOrNull() ?: "") }
    var currentPwd by remember { mutableStateOf("") }
    var newPwd by remember { mutableStateOf("") }
    var confirmPwd by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.bg(isDark)).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppTheme.text(isDark)) }
            Text("كلمة المرور 🔑", color = AppTheme.text(isDark), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Card(colors = CardDefaults.cardColors(containerColor = AppTheme.ColorDanger.copy(alpha = 0.1f)), border = BorderStroke(1.dp, AppTheme.ColorDanger.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Warning, null, tint = AppTheme.ColorDanger); Spacer(Modifier.width(8.dp)); Text("مهم", color = AppTheme.ColorDanger, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(4.dp))
                Text("إذا كانت كلمة مرور المدير admin، يرجى تغييرها لحماية حسابك.", color = AppTheme.ColorDanger, fontSize = 12.sp)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SafeDropdownSelector("المستخدم", usersMap.keys.toList(), selectedUser, { selectedUser = it }, isDark)
                OutlinedTextField(value = currentPwd, onValueChange = { currentPwd = it }, label = { Text("كلمة المرور الحالية") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = getSettingsFieldColors(isDark))
                OutlinedTextField(value = newPwd, onValueChange = { newPwd = it }, label = { Text("كلمة المرور الجديدة") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = getSettingsFieldColors(isDark))
                OutlinedTextField(value = confirmPwd, onValueChange = { confirmPwd = it }, label = { Text("تأكيد كلمة المرور") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = getSettingsFieldColors(isDark))

                Button(
                    onClick = {
                        if(newPwd == confirmPwd && newPwd.isNotBlank()) {
                            if (usersMap[selectedUser] == currentPwd) {
                                val json = JSONObject(usersStr)
                                json.put(selectedUser, newPwd)
                                prefs.edit().putString("app_users", json.toString()).apply()
                                usersStr = json.toString()
                                currentPwd = ""; newPwd = ""; confirmPwd = ""
                                Toast.makeText(context, "تم التغيير بنجاح", Toast.LENGTH_SHORT).show()
                            } else { Toast.makeText(context, "كلمة المرور الحالية خاطئة", Toast.LENGTH_SHORT).show() }
                        } else { Toast.makeText(context, "تأكد من تطابق كلمة المرور الجديدة", Toast.LENGTH_SHORT).show() }
                    },
                    modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                ) { Text("حفظ التغييرات 💾") }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("قائمة المستخدمين", fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
                    Surface(color = Color(0xFF3B82F6).copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)) { Text("المجموع ${usersMap.size}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color(0xFF3B82F6), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
                usersMap.keys.forEach { user ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Person, null, tint = AppTheme.subText(isDark), modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(user, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark)) }
                        Button(onClick = { selectedUser = user }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))) { Text("تغيير السر") }
                    }
                    HorizontalDivider(color = AppTheme.inputBg(isDark))
                }
            }
        }
    }
}

// ==========================================
// 8️⃣ شاشة المستخدمين (فعالة 100%)
// ==========================================
@Composable
fun UsersSubScreen(isDark: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE)

    var usersStr by remember { mutableStateOf(prefs.getString("app_users", "{\"admin\":\"admin\"}") ?: "{\"admin\":\"admin\"}") }
    var rolesStr by remember { mutableStateOf(prefs.getString("app_roles", "{\"admin\":\"مدير\"}") ?: "{\"admin\":\"مدير\"}") }

    val usersMap = remember(usersStr) { val map = mutableMapOf<String, String>(); try { val json = JSONObject(usersStr); json.keys().forEach { map[it] = json.getString(it) } } catch (e: Exception) {}; map }
    val rolesMap = remember(rolesStr) { val map = mutableMapOf<String, String>(); try { val json = JSONObject(rolesStr); json.keys().forEach { map[it] = json.getString(it) } } catch (e: Exception) {}; map }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var selectedUserForRole by remember { mutableStateOf(usersMap.keys.firstOrNull() ?: "") }
    var selectedRole by remember { mutableStateOf(rolesMap[selectedUserForRole] ?: "كاشير") }

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.bg(isDark)).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppTheme.text(isDark)) }
            Text("المستخدمون والصلاحيات 👥", color = AppTheme.text(isDark), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("إضافة مستخدم ➕", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold)
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("اسم المستخدم") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = getSettingsFieldColors(isDark))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("كلمة المرور") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = getSettingsFieldColors(isDark))

                Button(
                    onClick = {
                        if(username.isNotBlank() && password.isNotBlank()) {
                            val uJson = JSONObject(usersStr); uJson.put(username.trim(), password.trim())
                            val rJson = JSONObject(rolesStr); rJson.put(username.trim(), "كاشير")
                            prefs.edit().putString("app_users", uJson.toString()).putString("app_roles", rJson.toString()).apply()
                            usersStr = uJson.toString(); rolesStr = rJson.toString()
                            username = ""; password = ""
                            Toast.makeText(context, "تمت إضافة المستخدم", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.align(Alignment.End), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                ) { Text("إضافة مستخدم") }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("الحقوق والصلاحيات 🛡️", fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
                Text("تحديد مستوى الوصول والصلاحية للمستخدمين.", fontSize = 12.sp, color = AppTheme.subText(isDark))

                SafeDropdownSelector("المستخدم المراد تهيئته", usersMap.keys.toList(), selectedUserForRole, { selectedUserForRole = it; selectedRole = rolesMap[it] ?: "كاشير" }, isDark)
                SafeDropdownSelector("الصلاحية والمستوى", listOf("مدير", "كاشير"), selectedRole, { selectedRole = it }, isDark)

                Button(
                    onClick = {
                        val rJson = JSONObject(rolesStr)
                        rJson.put(selectedUserForRole, selectedRole)
                        prefs.edit().putString("app_roles", rJson.toString()).apply()
                        rolesStr = rJson.toString()
                        Toast.makeText(context, "تم تحديث الصلاحيات", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                ) { Text("حفظ الصلاحيات 💾") }
            }
        }
    }
}

// ==========================================
// 9️⃣ شاشة تدقيق الجرد
// ==========================================
@Composable
fun AuditSubScreen(isDark: Boolean, onBack: () -> Unit) {
    var searchLog by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.bg(isDark)).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppTheme.text(isDark)) }
            Text("تدقيق الجرد 📋", color = AppTheme.text(isDark), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = searchLog, onValueChange = { searchLog = it }, label = { Text("بحث (الجلسة، الكود، المنتج...)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = getSettingsFieldColors(isDark))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) { OutlinedTextField(value = "", onValueChange = {}, label = { Text("تاريخ البداية") }, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = getSettingsFieldColors(isDark)) }
                    Box(modifier = Modifier.weight(1f)) { OutlinedTextField(value = "", onValueChange = {}, label = { Text("تاريخ النهاية") }, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = getSettingsFieldColors(isDark)) }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("السطور", color = AppTheme.subText(isDark)); Text("0", fontWeight = FontWeight.Bold, color = AppTheme.text(isDark), fontSize = 18.sp) }
                HorizontalDivider(color = AppTheme.inputBg(isDark))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("الجلسات", color = AppTheme.subText(isDark)); Text("0", fontWeight = FontWeight.Bold, color = AppTheme.text(isDark), fontSize = 18.sp) }
                HorizontalDivider(color = AppTheme.inputBg(isDark))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("فرق الكمية", color = AppTheme.subText(isDark)); Text("0", fontWeight = FontWeight.Bold, color = AppTheme.text(isDark), fontSize = 18.sp) }
                HorizontalDivider(color = AppTheme.inputBg(isDark))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("قيمة الفرق", color = AppTheme.subText(isDark)); Text("0.00 DA", fontWeight = FontWeight.Black, color = Color(0xFF1E3A8A), fontSize = 20.sp) }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                Text("المستخدم", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("الجلسة", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                Text("التاريخ", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)), modifier = Modifier.fillMaxWidth()) {
            Text("لا توجد سجلات جرد محفوظة حالياً.", color = AppTheme.subText(isDark), modifier = Modifier.padding(24.dp).align(Alignment.CenterHorizontally))
        }
        Spacer(Modifier.height(40.dp))
    }
}
