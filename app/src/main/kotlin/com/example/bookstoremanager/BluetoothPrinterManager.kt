package com.example.bookstoremanager

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.OutputStream
import java.util.UUID

class BluetoothPrinterManager {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    // المعرف القياسي العالمي (UUID) للاتصال التسلسلي عبر البلوتوث (SPP) للطابعات
    private val UUID_SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /**
     * دالة للاتصال بالطابعة عبر اسمها (مثلاً: MTP-II, MPT-2, POS-58)
     */
    @SuppressLint("MissingPermission")
    fun connectToPrinter(printerName: String): Boolean {
        try {
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                return false // البلوتوث مغلق أو غير مدعوم
            }

            // جلب الأجهزة المقترنة بالهاتف
            val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
            val printerDevice = pairedDevices.find { it.name == printerName } ?: return false

            // إنشاء اتصال
            bluetoothSocket = printerDevice.createRfcommSocketToServiceRecord(UUID_SPP)
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * دالة لإرسال نص الفاتورة إلى الطابعة
     */
    fun printText(text: String) {
        try {
            val os = outputStream ?: return

            // 1. تهيئة الطابعة (ESC @)
            os.write(byteArrayOf(0x1B, 0x40))

            // 2. محاذاة النص للوسط (ESC a 1)
            os.write(byteArrayOf(0x1B, 0x61, 0x01))

            // 3. إرسال النص (ندعمه بالـ UTF-8 ليدعم اللغات)
            os.write(text.toByteArray(charset("UTF-8")))

            // 4. دفع الورق للأعلى للقطع (ترك مسافة)
            os.write("\n\n\n\n".toByteArray())

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * دالة لقطع الاتصال بعد الطباعة لتوفير البطارية
     */
    fun disconnect() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
