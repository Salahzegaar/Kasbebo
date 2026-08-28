package com.example.bookstoremanager.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.bookstoremanager.*
import com.example.bookstoremanager.data.*

@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isDark: Boolean
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = AppTheme.subText(isDark)) },
        leadingIcon = { Icon(Icons.Default.Lock, null, tint = AppTheme.subText(isDark)) },
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Text(text = if (passwordVisible) "👁️" else "🙈", fontSize = 16.sp)
            }
        },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AppTheme.inputBg(isDark),
            unfocusedContainerColor = AppTheme.inputBg(isDark),
            focusedTextColor = AppTheme.text(isDark),
            unfocusedTextColor = AppTheme.text(isDark),
            focusedBorderColor = AppTheme.GoldPrimary,
            unfocusedBorderColor = Color.Transparent
        )
    )
}

@Composable
fun LoginAuthScreen(
    isDark: Boolean,
    onLoginSuccess: (String) -> Unit,
    onCustomerModeOpen: (String) -> Unit
) {
    val context = LocalContext.current

    // تعريف مدراء المصادقة والمخزون
    val authManager = remember { AuthManager(context) }
    val inventoryManager = remember { InventoryManager(context) }

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var showCustomerSearchDialog by remember { mutableStateOf(false) }
    var searchMerchantCodeInput by remember { mutableStateOf("") }
    var isSearchingStore by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isSignUpMode) "إنشاء متجر جديد 🚀" else "تسجيل الدخول إلى كاسبو 🔑",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.text(isDark)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isSignUpMode) "أدخل بريدك وكلمة السر لتسجيل حسابك وتوليد كود المتجر تلقائياً:" else "أدخل بريدك الإلكتروني وكلمة السر للدخول إلى متجرك:",
            color = AppTheme.subText(isDark),
            fontSize = 13.sp
        )

        Spacer(Modifier.height(20.dp))

        PremiumTextField(
            value = emailInput,
            onValueChange = { emailInput = it.trim() },
            label = "البريد الإلكتروني (Email)",
            isDark = isDark,
            icon = Icons.Default.Person
        )

        Spacer(Modifier.height(12.dp))

        PasswordTextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            label = "كلمة السر (6 أرقام/أحرف على الأقل)",
            isDark = isDark
        )

        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppTheme.GoldPrimary)
            }
        } else {
            PrimaryAppButton(
                text = if (isSignUpMode) "إنشاء الحساب وتوليد الكود ✨" else "تسجيل الدخول والمزامنة ☁️",
                modifier = Modifier.fillMaxWidth()
            ) {
                val cleanEmail = emailInput.trim()
                val cleanPassword = passwordInput.trim()

                if (cleanEmail.isBlank() || cleanPassword.length < 6) {
                    Toast.makeText(context, "يرجى كتابة البريد الإلكتروني وكلمة سر لا تقل عن 6 أحرف!", Toast.LENGTH_LONG).show()
                    return@PrimaryAppButton
                }

                isLoading = true

                if (isSignUpMode) {
                    authManager.registerNewStore(
                        email = cleanEmail,
                        pass = cleanPassword,
                        onSuccess = { generatedStoreCode ->
                            isLoading = false
                            onLoginSuccess(generatedStoreCode)
                        },
                        onFailure = { errMsg ->
                            isLoading = false
                            Toast.makeText(context, "فشل إنشاء الحساب: $errMsg", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    authManager.loginStore(
                        email = cleanEmail,
                        pass = cleanPassword,
                        inventoryManager = inventoryManager,
                        onSuccess = { storeCode ->
                            isLoading = false
                            Toast.makeText(context, "أهلاً بك مجدداً! تم الدخول بنجاح ✅", Toast.LENGTH_SHORT).show()
                            onLoginSuccess(storeCode)
                        },
                        onFailure = { err ->
                            isLoading = false
                            Toast.makeText(context, "فشل تسجيل الدخول: تأكد من الإيميل وكلمة السر", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            TextButton(
                onClick = { isSignUpMode = !isSignUpMode },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isSignUpMode) "لديك حساب بالفعل؟ سجل الدخول هنا" else "ليس لديك متجر؟ اضغط هنا لإنشاء متجر جديد",
                    color = AppTheme.GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            HorizontalDivider(color = AppTheme.inputBg(isDark), modifier = Modifier.padding(vertical = 8.dp))

            SecondaryAppButton(
                text = "تصفح كزبون (البحث عن تاجر) 🛒",
                isDark = isDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                showCustomerSearchDialog = true
            }
        }
    }

    if (showCustomerSearchDialog) {
        AlertDialog(
            onDismissRequest = { showCustomerSearchDialog = false },
            containerColor = AppTheme.card(isDark),
            title = {
                Text("البحث عن متجر تاجر 🛍️", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("أدخل كود المتجر الخاص بالتاجر للتصفح والتسوق منه مباشرة:", color = AppTheme.subText(isDark), fontSize = 12.sp)
                    PremiumTextField(
                        value = searchMerchantCodeInput,
                        onValueChange = { searchMerchantCodeInput = it.trim().uppercase() },
                        label = "كود التاجر (مثل: KSB-A9X-4829)",
                        isDark = isDark,
                        icon = Icons.Default.Search
                    )
                }
            },
            confirmButton = {
                if (isSearchingStore) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppTheme.GoldPrimary)
                    }
                } else {
                    PrimaryAppButton(text = "دخول المتجر 🚀", modifier = Modifier.fillMaxWidth()) {
                        val code = searchMerchantCodeInput.trim()
                        if (code.isNotBlank()) {
                            isSearchingStore = true
                            authManager.searchMerchantStore(
                                code,
                                onFound = {
                                    isSearchingStore = false
                                    showCustomerSearchDialog = false
                                    onCustomerModeOpen(code)
                                },
                                onNotFound = {
                                    isSearchingStore = false
                                    Toast.makeText(context, "لم يتم العثور على متجر بهذا الكود!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            Toast.makeText(context, "الرجاء كتابة كود التاجر أولاً!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomerSearchDialog = false }) {
                    Text("إلغاء", color = AppTheme.subText(isDark))
                }
            }
        )
    }
}
