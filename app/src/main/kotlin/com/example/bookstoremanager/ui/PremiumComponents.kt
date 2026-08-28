package com.example.bookstoremanager.ui

// 🌟 الاستيرادات التي كانت مفقودة لحل الأخطاء
import androidx.compose.ui.graphics.Brush
import com.example.bookstoremanager.AppTheme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==========================================
// 🚀 1. الزر الفاخر (مع تأثير النبض والتدرج)
// ==========================================
@Composable
fun PremiumButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // حركة تصغير ناعمة عند الضغط
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 150), label = ""
    )

    val backgroundBrush = if (isDanger) {
        Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFBE123C)))
    } else {
        AppTheme.GoldGradient
    }

    Box(
        modifier = modifier
        .scale(scale)
        .height(54.dp)
        .shadow(
            elevation = if (isPressed) 2.dp else 8.dp,
            shape = RoundedCornerShape(14.dp),
            spotColor = if (isDanger) Color.Red else AppTheme.PremiumGold
        )
        .clip(RoundedCornerShape(14.dp))
        .background(backgroundBrush)
        .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ==========================================
// 💳 2. البطاقة الفاخرة (تأثير الزجاج والظل الناعم)
// ==========================================
@Composable
fun PremiumCard(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val baseModifier = modifier
    .fillMaxWidth()
    .shadow(
        elevation = if (isDark) 0.dp else 6.dp,
        shape = RoundedCornerShape(16.dp),
        spotColor = Color(0xFF94A3B8).copy(alpha = 0.15f)
    )
    .clip(RoundedCornerShape(16.dp))
    .background(AppTheme.card(isDark))
    .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)

    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppTheme.cardBorder(isDark)),
        modifier = baseModifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

// ==========================================
// 📝 3. حقل الإدخال العصري (Clean TextField)
// ==========================================
@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isNumber: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        leadingIcon = icon?.let { { Icon(it, null, tint = AppTheme.PremiumGold, modifier = Modifier.size(20.dp)) } },
        keyboardOptions = KeyboardOptions(keyboardType = if (isNumber) androidx.compose.ui.text.input.KeyboardType.Number else androidx.compose.ui.text.input.KeyboardType.Text),
        modifier = modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AppTheme.text(isDark),
            unfocusedTextColor = AppTheme.text(isDark),
            focusedContainerColor = AppTheme.inputBg(isDark),
            unfocusedContainerColor = AppTheme.inputBg(isDark).copy(alpha = 0.5f),
            focusedBorderColor = AppTheme.PremiumGold,
            unfocusedBorderColor = Color.Transparent,
            focusedLabelColor = AppTheme.PremiumGold,
            unfocusedLabelColor = AppTheme.subText(isDark)
        )
    )
}
