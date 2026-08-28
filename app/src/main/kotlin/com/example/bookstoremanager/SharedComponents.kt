package com.example.bookstoremanager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import androidx.compose.foundation.clickable

// 🇩🇿 مشاركة الفاتورة للواتساب مع معالجة الرقم الجزائري
fun shareInvoiceToWhatsApp(context: Context, invoiceText: String, phoneNumber: String = "") {
    try {
        var cleanPhone = phoneNumber.replace("+", "").replace(" ", "").replace("-", "").trim()
        if (cleanPhone.startsWith("0") && cleanPhone.length == 10) { cleanPhone = "213" + cleanPhone.substring(1) }
        val url = if (cleanPhone.isNotBlank()) "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(invoiceText)}" else "https://api.whatsapp.com/send?text=${Uri.encode(invoiceText)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "تطبيق الواتساب غير مثبت على جهازك!", Toast.LENGTH_SHORT).show()
    }
}

// ==========================================
// 📷 ماسح الباركود الحي (CameraX + ML Kit)
// ==========================================
@Composable
fun BarcodeScannerDialog(onBarcodeScanned: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth().height(480.dp).padding(16.dp),
        title = { Text("📷 قم بتوجيه الكاميرا نحو الباركود", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Box(
                modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        val executor = ContextCompat.getMainExecutor(ctx)

                        cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val barcodeScanner = BarcodeScanning.getClient()
                                val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                                imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                        barcodeScanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            for (barcode in barcodes) {
                                                barcode.rawValue?.let { code ->
                                                    cameraProvider.unbindAll()
                                                    onBarcodeScanned(code)
                                                    return@addOnSuccessListener
                                                }
                                            }
                                        }
                                        .addOnCompleteListener { imageProxy.close() }
                                    } else {
                                        imageProxy.close()
                                    }
                                }

                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }, executor)

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Red.copy(alpha = 0.5f)).align(Alignment.Center))
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء وإغلاق", color = AppTheme.ColorDanger, fontWeight = FontWeight.Bold) } }
    )
}
// ==========================================
// 📊 مكونات الإحصائيات والأزرار المصغرة
// ==========================================
@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
                Text(title, fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
            }
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
        .size(32.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(color.copy(alpha = 0.12f))
        .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
    }
}
