package com.example.bookstoremanager

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // يتم استدعاؤها عند إنشاء أو تحديث توكن الجهاز للتاجر
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // يمكنك هنا حفظ التوكن في SharedPreferences أو إرساله لـ Firebase ليتم استهدافه بالرسائل
    }

    // يتم استدعاؤها فور وصول إشعار سحابي (سواء التطبيق في الخلفية أو مغلق كلياً)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // استخلاص عنوان ومحتوى الإشعار القادم من السحابة أو البيانات المرسلة
        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "🛍️ طلب زبون جديد!"

        val body = remoteMessage.notification?.body 
            ?: remoteMessage.data["body"] 
            ?: "لقد تلقيت طلباً جديداً في متجرك، اضغط للمزيد من التفاصيل."

        // إطلاق الإشعار فوراً للمستخدم عبر المساعد الصوتي والاهتزاز
        NotificationHelper.showNotification(applicationContext, title, body)
    }
}
