package com.example.ai_guardian.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.ai_guardian.MainActivity
import com.example.ai_guardian.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"] ?: ""

        when (type) {
            "start_listening" -> KeywordListenerService.start(this)
            "stop_listening"  -> KeywordListenerService.stop(this)

            "incoming_call" -> {
                val callId     = message.data["callId"]     ?: return
                val from       = message.data["from"]       ?: return
                val callerName = message.data["callerName"] ?: "Appel entrant"
                showIncomingCallNotification(from, callId, callerName)
            }

            // ✅ Alertes + Rappels
            else -> {
                val title = message.notification?.title
                    ?: message.data["title"]
                    ?: "AI Guardian"
                val body = message.notification?.body
                    ?: message.data["body"]
                    ?: ""
                if (title.isNotBlank()) showNotification(title, body)
            }
        }
    }

    private fun showIncomingCallNotification(from: String, callId: String, callerName: String) {

        // ── Full-screen intent → يفتح MainActivity ──
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("incoming_from",    from)
            putExtra("incoming_call_id", callId)
            putExtra("caller_name",      callerName)
        }
        val fullScreenPI = PendingIntent.getActivity(
            this, callId.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ── Accept / Reject receivers ──
        val acceptIntent = Intent(this, com.example.ai_guardian.receiver.CallActionReceiver::class.java).apply {
            action = "ACTION_ACCEPT"
            putExtra("call_id", callId)
            putExtra("from",    from)
        }
        val rejectIntent = Intent(this, com.example.ai_guardian.receiver.CallActionReceiver::class.java).apply {
            action = "ACTION_REJECT"
            putExtra("call_id", callId)
            putExtra("from",    from)
        }
        val acceptPI = PendingIntent.getBroadcast(
            this, callId.hashCode() + 1, acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val rejectPI = PendingIntent.getBroadcast(
            this, callId.hashCode() + 2, rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "incoming_call_channel", "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "incoming_call_channel")
            .setContentTitle("📞 Appel entrant")
            .setContentText(callerName)
            .setSmallIcon(R.drawable.logo)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPI, true)   // ← يفتح فوق lock screen
            .addAction(R.drawable.logo, "✅ Répondre", acceptPI)
            .addAction(R.drawable.logo, "❌ Rejeter",  rejectPI)
            .setAutoCancel(false)
            .setOngoing(true)
            .setTimeoutAfter(60_000)
            .build()

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(callId.hashCode(), notification)
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