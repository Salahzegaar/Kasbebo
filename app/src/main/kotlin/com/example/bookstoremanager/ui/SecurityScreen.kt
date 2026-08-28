package com.example.bookstoremanager.ui

import com.example.bookstoremanager.*
import com.example.bookstoremanager.data.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PinLockScreen(savedPin: String, isDark: Boolean, onUnlock: () -> Unit) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
        .fillMaxSize()
        .background(AppTheme.bg(isDark))
        .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Lock, contentDescription = "قفل", tint = AppTheme.GoldPrimary, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("كاسبو مقفل", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
        Text(if (isError) "رمز خاطئ، حاول مجدداً" else "الرجاء إدخال الرمز السري", fontSize = 14.sp, color = if (isError) AppTheme.ColorDanger else AppTheme.subText(isDark))

        Spacer(modifier = Modifier.height(32.dp))

        // النقاط الأربعة التي تمثل الأرقام المدخلة
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            for (i in 0 .. 3) {
                val isFilled = i < enteredPin.length
                Box(
                    modifier = Modifier
                    .size(16.dp)
                    .background(
                        color = if (isError) AppTheme.ColorDanger else if (isFilled) AppTheme.GoldPrimary else AppTheme.inputBg(isDark),
                        shape = CircleShape
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // لوحة الأرقام (NumPad)
        val buttons = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "⌫")
        )

        for (row in buttons) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(bottom = 24.dp)) {
                for (btn in row) {
                    if (btn.isEmpty()) {
                        Spacer(modifier = Modifier.size(72.dp))
                    } else {
                        Box(
                            modifier = Modifier
                            .size(72.dp)
                            .background(AppTheme.card(isDark), CircleShape)
                            .clickable {
                                if (isError) { isError = false; enteredPin = "" }

                                if (btn == "⌫") {
                                    if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                } else {
                                    if (enteredPin.length < 4) {
                                        enteredPin += btn
                                        if (enteredPin.length == 4) {
                                            if (enteredPin == savedPin) {
                                                onUnlock() // فتح القفل إذا كان الرمز صحيحاً
                                            } else {
                                                isError = true
                                                scope.launch {
                                                    delay(400) // الانتظار نصف ثانية لتوضيح الخطأ
                                                    enteredPin = ""
                                                    isError = false
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = btn,
                                fontSize = if (btn == "⌫") 24.sp else 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (btn == "⌫") AppTheme.ColorDanger else AppTheme.text(isDark)
                            )
                        }
                    }
                }
            }
        }
    }
}
