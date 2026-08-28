package com.example.bookstoremanager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

// ==========================================
// 🧩 مكونات التصميم المشتركة
// ==========================================
@Composable
fun PrimaryAppButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    containerColor: Color = AppTheme.GoldPrimary,
    contentColor: Color = AppTheme.GoldPrimaryText,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        shape = RoundedCornerShape(14.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SecondaryAppButton(
    text: String,
    modifier: Modifier = Modifier,
    isDark: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    contentColor: Color = AppTheme.text(isDark),
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.inputBg(isDark), contentColor = contentColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isDark: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isNumber: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = AppTheme.subText(isDark)) },
        leadingIcon = if (icon != null) { { Icon(icon, null, tint = AppTheme.subText(isDark)) } } else null,
        keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
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
fun FilterChipItem(text: String, isSelected: Boolean, isDark: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) AppTheme.GoldPrimary else AppTheme.inputBg(isDark),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = if (isSelected) AppTheme.GoldPrimaryText else AppTheme.subText(isDark),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

// ==========================================
// 🚀 الدوال المساعدة (Helper Functions)
// ==========================================

data class ScannedProductInfo(val name: String, val imageUrl: String)

// 🔍 محرك البحث المزدوج عن الباركود
suspend fun fetchProductInfoByBarcode(barcode: String): ScannedProductInfo? {
    val cleanBarcode = barcode.trim()
    if (cleanBarcode.isBlank()) return null

    return withContext(Dispatchers.IO) {
        try {
            // 1️⃣ البحث أولاً في كتالوج كاسبو السحابي الموحد (Kasebo Global Community Catalog)
            val firebaseResult = suspendCancellableCoroutine<ScannedProductInfo?> { continuation ->
                FirebaseDatabase.getInstance().getReference("Kasebo_Global_Catalog")
                .child(cleanBarcode).get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val name = snapshot.child("name").value?.toString() ?: ""
                        val imageUrl = snapshot.child("imageUrl").value?.toString() ?: ""
                        if (name.isNotBlank()) {
                            continuation.resume(ScannedProductInfo(name, imageUrl))
                            return@addOnSuccessListener
                        }
                    }
                    continuation.resume(null)
                }.addOnFailureListener { continuation.resume(null) }
            }
            if (firebaseResult != null) return@withContext firebaseResult

            // 2️⃣ البحث في قاعدة البيانات العالمية (Open Food Facts)
            val url = URL("https://world.openfoodfacts.org/api/v0/product/$cleanBarcode.json")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 4000; conn.readTimeout = 4000
            conn.setRequestProperty("User-Agent", "KaseboStore - Android")

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                if (json.optInt("status", 0) == 1) {
                    val productObj = json.optJSONObject("product")
                    if (productObj != null) {
                        val prodName = productObj.optString("product_name_ar")
                        .ifBlank { productObj.optString("product_name_fr") }
                        .ifBlank { productObj.optString("product_name", "") }
                        val prodImg = productObj.optString("image_url").ifBlank { productObj.optString("image_front_url", "") }
                        return@withContext ScannedProductInfo(prodName, prodImg)
                    }
                }
            }
            null
        } catch (e: Exception) { null }
    }
}

// 🖼️ تحويل الصور للنصوص (للحفظ السحابي)
fun uriToBase64(context: Context, uri: Uri): String {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        if (bytes != null) android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT) else ""
    } catch (e: Exception) { "" }
}

