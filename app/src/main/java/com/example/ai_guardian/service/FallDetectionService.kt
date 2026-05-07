package com.example.ai_guardian.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.IBinder
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.example.ai_guardian.receiver.FallConfirmReceiver
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import java.util.*
import kotlin.math.sqrt

class FallDetectionService : Service(), SensorEventListener {

    // ─── Accelerometer ──────────────────────────────────────────────────────
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastFallTime = 0L

    // ─── TTS ─────────────────────────────────────────────────────────────────
    private lateinit var tts: TextToSpeech
    private var ttsReady = false

    // ─── Coroutines ──────────────────────────────────────────────────────────
    private lateinit var scope: CoroutineScope

    // ─── Flags ───────────────────────────────────────────────────────────────
    @Volatile private var isHandlingFall = false

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())

        // Accelerometer
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // TTS
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale.FRENCH)
                ttsReady = (result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED)
            }
        }

        scope = CoroutineScope(Dispatchers.Main)
    }

    // ════════════════════════════════════════════════════════════════════════
    // Accelerometer — détection simple par seuil
    // ════════════════════════════════════════════════════════════════════════
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val (x, y, z) = event.values
        val acceleration = sqrt(x * x + y * y + z * z)

        if (acceleration > 25f) {
            val now = System.currentTimeMillis()
            // Anti-spam : 30 secondes entre deux détections
            if (!isHandlingFall && now - lastFallTime > 30000) {
                lastFallTime = now
                isHandlingFall = true
                scope.launch {
                    handleFallDetected()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ════════════════════════════════════════════════════════════════════════
    // Logique principale : chute détectée
    // ════════════════════════════════════════════════════════════════════════
    private suspend fun handleFallDetected() {
        val userResponded = askUserIfOk()

        if (!userResponded) {
            // 1. Firebase Alert
            sendAlertToFirebase()
            // 2. Attendre 2s puis appeler le superviseur
            delay(2000)
            callSuperviseur()
        }

        isHandlingFall = false
    }

    // ════════════════════════════════════════════════════════════════════════
    // "Êtes-vous OK ?" × 3 avec 10s d'attente
    // ════════════════════════════════════════════════════════════════════════
    private suspend fun askUserIfOk(): Boolean {
        repeat(3) { attempt ->
            speakOut("Êtes-vous OK ? Appuyez sur le bouton si vous allez bien.")
            showConfirmationNotification(attempt + 1)

            // Attendre 10 secondes
            repeat(100) {
                if (userConfirmedOk) {
                    userConfirmedOk = false
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
    // Appel téléphonique automatique au superviseur
    // ════════════════════════════════════════════════════════════════════════
    private fun callSuperviseur() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        speakOut("Appel au superviseur en cours.")

        FirebaseFirestore.getInstance()
            .collection("Associations")
            .whereEqualTo("superviseeId", uid)
            .get()
            .addOnSuccessListener { result ->
                val superviseurId = result.documents.firstOrNull()
                    ?.getString("superviseurId") ?: return@addOnSuccessListener

                FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(superviseurId)
                    .get()
                    .addOnSuccessListener { userDoc ->
                        val phone = userDoc.getString("phone") ?: return@addOnSuccessListener
                        val callIntent = Intent(Intent.ACTION_CALL).apply {
                            data  = Uri.parse("tel:$phone")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(callIntent)
                    }
            }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Firebase Alert
    // ════════════════════════════════════════════════════════════════════════
    private fun sendAlertToFirebase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("Associations")
            .whereEqualTo("superviseeId", uid)
            .get()
            .addOnSuccessListener { result ->
                val superviseurId = result.documents.firstOrNull()
                    ?.getString("superviseurId") ?: ""

                FirebaseFirestore.getInstance()
                    .collection("Users").document(uid).get()
                    .addOnSuccessListener { userDoc ->
                        val nom = userDoc.getString("nom") ?: "Unknown"

                        val alert = hashMapOf(
                            "superviseeId"   to uid,
                            "superviseeName" to nom,
                            "superviseurId"  to superviseurId,
                            "type"           to "danger",
                            "message"        to "⚠️ Chute détectée automatiquement",
                            "timestamp"      to System.currentTimeMillis(),
                            "status"         to "unconfirmed"
                        )

                        FirebaseFirestore.getInstance()
                            .collection("Alerts")
                            .add(alert)
                    }
            }
    }

    // ════════════════════════════════════════════════════════════════════════
    // TTS
    // ════════════════════════════════════════════════════════════════════════
    private fun speakOut(text: String) {
        if (ttsReady) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "fall_confirm")
    }

    // ════════════════════════════════════════════════════════════════════════
    // Notification confirmation
    // ════════════════════════════════════════════════════════════════════════
    private fun showConfirmationNotification(attempt: Int) {
        val okIntent  = Intent(this, FallConfirmReceiver::class.java).apply {
            action = ACTION_USER_OK
        }
        val okPending = PendingIntent.getBroadcast(
            this, 0, okIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, "alarm_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Chute détectée ! ($attempt/3)")
            .setContentText("Êtes-vous OK ? Appuyez si vous allez bien.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(android.R.drawable.ic_menu_call, "✅ Je vais bien", okPending)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIF_CONFIRM_ID, notif)
    }

    private fun cancelConfirmationNotification() {
        getSystemService(NotificationManager::class.java).cancel(NOTIF_CONFIRM_ID)
    }

    // ════════════════════════════════════════════════════════════════════════
    // Notification foreground
    // ════════════════════════════════════════════════════════════════════════
    private fun createNotification(): Notification {
        val channelId = "ai_guardian_service"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "AI Guardian Service", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("AI Guardian actif")
            .setContentText("Détection de chute en cours...")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        @Volatile var userConfirmedOk  = false
        const val ACTION_USER_OK       = "com.example.ai_guardian.USER_OK"
        const val NOTIF_CONFIRM_ID     = 42
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        tts.shutdown()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}