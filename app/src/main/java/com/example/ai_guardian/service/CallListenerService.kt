package com.example.ai_guardian.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import com.example.ai_guardian.MainActivity
import com.example.ai_guardian.R
import com.example.ai_guardian.receiver.CallActionReceiver
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class CallListenerService : Service() {

    private var listener: ListenerRegistration? = null
    private val shownCalls = mutableSetOf<String>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        startListening()
        return START_STICKY
    }

    private fun startListening() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val startTime = System.currentTimeMillis()

        listener = FirebaseFirestore.getInstance()
            .collection("calls")
            .whereEqualTo("to", uid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documents?.forEach { doc ->
                    val from      = doc.getString("from") ?: return@forEach
                    val callId    = doc.id
                    val timestamp = doc.getLong("timestamp") ?: 0L

                    if (timestamp < startTime) return@forEach
                    if (shownCalls.contains(callId)) return@forEach
                    shownCalls.add(callId)

                    // جيب اسم المتصل ثم أظهر notification
                    FirebaseFirestore.getInstance()
                        .collection("Users").document(from).get()
                        .addOnSuccessListener { doc ->
                            val name = doc.getString("nom") ?: from
                            showCallNotification(from, callId, name)
                        }
                }
            }
    }

    // ← هنا مش في companion object
    private fun showCallNotification(from: String, callId: String, callerName: String) {

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

        val acceptIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = "ACTION_ACCEPT"
            putExtra("call_id", callId)
            putExtra("from", from)
        }
        val rejectIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = "ACTION_REJECT"
            putExtra("call_id", callId)
            putExtra("from", from)
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
            val channel = android.app.NotificationChannel(
                "incoming_call_channel", "Incoming Calls",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableVibration(true)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
                .createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "incoming_call_channel")
            .setContentTitle("📞 Appel entrant")
            .setContentText(callerName)
            .setSmallIcon(R.drawable.logo)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPI, true)
            .addAction(R.drawable.logo, "✅ Répondre", acceptPI)
            .addAction(R.drawable.logo, "❌ Rejeter", rejectPI)
            .setAutoCancel(false)
            .setOngoing(true)
            .setTimeoutAfter(60_000)
            .build()

        (getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
            .notify(callId.hashCode(), notification)
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI Guardian actif")
            .setContentText("En attente d'appels…")
            .setSmallIcon(R.drawable.logo)
            .setSilent(true)
            .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        listener?.remove()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "call_listener_channel"
        const val NOTIF_ID   = 999

        fun start(context: Context) {
            createChannel(context)
            val intent = Intent(context, CallListenerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CallListenerService::class.java))
        }

        private fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID, "Call Listener",
                    NotificationManager.IMPORTANCE_LOW
                )
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .createNotificationChannel(channel)
            }
        }
    }
}