package com.example.ai_guardian.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.example.ai_guardian.receiver.AlertReceiver
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import java.util.*

class AlertService : Service(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private var ttsReady = false

    private lateinit var scope: CoroutineScope

    private var surveilleeName = "Utilisateur"

    companion object {
        // gardé pour compatibilité avec AlertReceiver existant
        @Volatile var userConfirmed = false

        private const val CHANNEL_ID       = "alert_channel"
        private const val NOTIF_FOREGROUND = 2222
        private const val NOTIF_CONFIRM_ID = 1002
    }

    // ════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ════════════════════════════════════════════════════════════════════════
    override fun onCreate() {
        super.onCreate()

        createChannel()
        scope = CoroutineScope(Dispatchers.Main)
        tts   = TextToSpeech(this, this)

        startForeground(
            NOTIF_FOREGROUND,
            buildSilentNotification("AI Guardian", "Surveillance active...")
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        surveilleeName = intent?.getStringExtra("surveillee_name") ?: "Utilisateur"

        when (intent?.getStringExtra("type")) {
            "fall"       -> scope.launch { handleFall() }
            "inactivity" -> scope.launch { handleInactivity() }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        if (::tts.isInitialized) tts.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ════════════════════════════════════════════════════════════════════════
    // TTS
    // ════════════════════════════════════════════════════════════════════════
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.FRENCH
            ttsReady = true
        }
    }

    private fun speakOut(text: String) {
        if (!ttsReady) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "alert_tts")
    }

    // ════════════════════════════════════════════════════════════════════════
    // FALL — même flow que FallDetectionService
    // ════════════════════════════════════════════════════════════════════════
    private suspend fun handleFall() {
        val userOk = askUserIfOk(title = "⚠️ Chute détectée !")

        if (!userOk) {
            sendAlertToFirebase(
                type    = "danger",
                message = "⚠️ Chute détectée automatiquement"
            )
            delay(2_000)
            callSuperviseur()
        }

        stopSelf()
    }

    // ════════════════════════════════════════════════════════════════════════
    // INACTIVITY — même flow que FallDetectionService
    // ════════════════════════════════════════════════════════════════════════
    private suspend fun handleInactivity() {
        val userOk = askUserIfOk(title = "⚠️ Inactivité prolongée !")

        if (!userOk) {
            sendAlertToFirebase(
                type    = "warning",
                message = "⚠️ Inactivité prolongée détectée automatiquement"
            )
            delay(2_000)
            callSuperviseur()
        }

        stopSelf()
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3 tentatives de confirmation — même logique que FallDetectionService
    // ════════════════════════════════════════════════════════════════════════
    private suspend fun askUserIfOk(title: String): Boolean {
        userConfirmed = false

        repeat(3) { attempt ->
            speakOut(
                "$surveilleeName, êtes-vous OK ? " +
                        "Appuyez sur le bouton si vous allez bien."
            )
            showConfirmationNotification(attempt + 1, title)

            // attend 10 secondes (100 × 100ms) en vérifiant la confirmation
            repeat(100) {
                if (userConfirmed) {
                    cancelConfirmationNotification()
                    return true
                }
                delay(100)
            }
        }

        cancelConfirmationNotification()
        return false
    }

    // ════════════════════════════════════════════════════════════════════════
    // Notification interactive — bouton "Je vais bien"
    // ════════════════════════════════════════════════════════════════════════
    private fun showConfirmationNotification(attempt: Int, title: String) {
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(this, AlertReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("$title ($attempt/3)")
            .setContentText("Êtes-vous OK ? Appuyez si vous allez bien.")
            .addAction(
                android.R.drawable.ic_menu_send,
                "✅ Je vais bien",
                pendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_CONFIRM_ID, notification)
    }

    private fun cancelConfirmationNotification() {
        getSystemService(NotificationManager::class.java)
            .cancel(NOTIF_CONFIRM_ID)
    }

    // ════════════════════════════════════════════════════════════════════════
    // Firebase Alert (chute ou inactivité)
    // ════════════════════════════════════════════════════════════════════════
    private fun sendAlertToFirebase(type: String, message: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("Associations")
            .whereEqualTo("superviseeId", uid)
            .get()
            .addOnSuccessListener { result ->
                val superviseurId =
                    result.documents.firstOrNull()?.getString("superviseurId") ?: ""

                FirebaseFirestore.getInstance()
                    .collection("Alerts")
                    .add(hashMapOf(
                        "superviseeId"   to uid,
                        "superviseeName" to surveilleeName,
                        "superviseurId"  to superviseurId,
                        "type"           to type,
                        "message"        to message,
                        "timestamp"      to System.currentTimeMillis(),
                        "status"         to "unconfirmed"
                    ))
            }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Appel superviseur
    // ════════════════════════════════════════════════════════════════════════
    private fun callSuperviseur() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        speakOut("Appel au superviseur en cours.")

        FirebaseFirestore.getInstance()
            .collection("Associations")
            .whereEqualTo("superviseeId", uid)
            .get()
            .addOnSuccessListener { result ->
                val superviseurId = result.documents
                    .firstOrNull()?.getString("superviseurId")
                    ?: return@addOnSuccessListener

                FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(superviseurId)
                    .get()
                    .addOnSuccessListener { doc ->
                        val phone = doc.getString("phone") ?: return@addOnSuccessListener
                        val callIntent = android.content.Intent(
                            android.content.Intent.ACTION_CALL
                        ).apply {
                            data  = android.net.Uri.parse("tel:$phone")
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(callIntent)
                    }
            }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════════════════
    private fun buildSilentNotification(title: String, text: String): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AI Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description     = "Alertes chute et inactivité"
                enableVibration(true)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}