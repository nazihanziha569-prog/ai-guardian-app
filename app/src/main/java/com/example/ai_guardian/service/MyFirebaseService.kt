package com.example.ai_guardian.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.ai_guardian.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {

        val type = message.data["type"]

        when (type) {
            // ✅ FCM réveille le service keyword
            "start_listening" -> {
                android.util.Log.d("FCM", "🎤 FCM → Starting keyword listener")
                KeywordListenerService.start(this)
            }
            "stop_listening" -> {
                android.util.Log.d("FCM", "🔇 FCM → Stopping keyword listener")
                KeywordListenerService.stop(this)
            }
            // ✅ Notification normale (alertes)
            else -> {
                val title = message.data["title"] ?: "Alert"
                val body  = message.data["body"]  ?: ""
                showNotification(title, body)
            }
        }
    }

    // ✅ Sauvegarder le token FCM quand il change
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("Users")
            .document(uid)
            .update("fcmToken", token)
        android.util.Log.d("FCM", "✅ Token updated: $token")
    }

    private fun showNotification(title: String, message: String) {
        val manager   = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "alerts_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.logo)
            .build()

        manager.notify(1, notification)
    }
}