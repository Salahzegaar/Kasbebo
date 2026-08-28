package com.example.bookstoremanager

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AppTheme {
    // 🌟 الألوان الأساسية الفاخرة (التصميم الجديد)
    val PremiumGold = Color(0xFFD4AF37)
    val PremiumGoldDark = Color(0xFFB5952F)
    val PremiumGoldLight = Color(0xFFF3D568)

    // تدرج ذهبي للأزرار الفاخرة
    val GoldGradient = Brush.horizontalGradient(
        colors = listOf(PremiumGoldLight, PremiumGoldDark)
    )

    // 🌟 جسر التوافق (هذا الجزء سيحل جميع الأخطاء دفعة واحدة)
    val GoldPrimary = PremiumGold
    val GoldPrimaryText = PremiumGoldDark
    val ColorDanger = Color(0xFFF43F5E) // أحمر ناعم وفاخر
    val ColorSuccess = Color(0xFF10B981) // أخضر ناعم وفاخر

    // 🎨 دوال الوضع الليلي والنهاري (Theme Engine)
    fun bg(isDark: Boolean) = if (isDark) Color(0xFF0B0F19) else Color(0xFFF8FAFC)
    fun card(isDark: Boolean) = if (isDark) Color(0xFF131B2E) else Color(0xFFFFFFFF)
    fun inputBg(isDark: Boolean) = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)

    fun text(isDark: Boolean) = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    fun subText(isDark: Boolean) = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    fun cardBorder(isDark: Boolean) = if (isDark) Color(0xFFFFFFFF).copy(alpha = 0.05f) else Color(0xFFE2E8F0)
}
