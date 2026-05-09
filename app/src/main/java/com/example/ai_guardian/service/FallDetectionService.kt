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
import org.tensorflow.lite.Interpreter
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

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

    // ─── Inactivité ──────────────────────────────────────────────────────────
    private var lastMovementTime = System.currentTimeMillis()
    private var inactivityAlertSent = false

    // ─── User ────────────────────────────────────────────────────────────────
    private var surveilleeName = "Utilisateur"
    private lateinit var tflite: Interpreter
    private val window = ArrayDeque<FloatArray>()
    private val lock = Any()


    private var heureReveil = "07:00"
    private var heureSommeil = "22:00"
    private var inactivityThresholdMinutes = 10
    private var inactivityEnabled = true




    // ─── Flags ───────────────────────────────────────────────────────────────
    @Volatile
    private var isHandlingFall = false

    override fun onCreate() {
        super.onCreate()

        startForeground(1, createNotification())

        // Accelerometer
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        accelerometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        // TTS
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale.FRENCH)

                ttsReady =
                    result != TextToSpeech.LANG_MISSING_DATA &&
                            result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }

        scope = CoroutineScope(Dispatchers.Main)

        // ─── Vérification inactivité ────────────────────────────────────────
        scope.launch {
            while (true) {

                val now = System.currentTimeMillis()

                // 30 minutes sans mouvement
                if (inactivityEnabled &&
                    !isSleepingTime() &&
                    !inactivityAlertSent &&
                    now - lastMovementTime > inactivityThresholdMinutes * 60 * 1000L) {

                    inactivityAlertSent = true
                    handleInactivityDetected()

                }

                delay(60_000L)
            }
        }
        tflite = Interpreter(loadModelFile())
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        surveilleeName =
            intent?.getStringExtra("surveillee_name")
                ?: "Utilisateur"

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid != null) {

            FirebaseFirestore.getInstance()
                .collection("SurveilleConfig")
                .document(uid)
                .addSnapshotListener { doc, _ ->

                    if (doc != null && doc.exists()) {

                        heureReveil =
                            doc.getString("heureReveil") ?: "07:00"

                        heureSommeil =
                            doc.getString("heureSommeil") ?: "22:00"
                        inactivityThresholdMinutes =
                            doc.getLong("inactivityThresholdMinutes")?.toInt() ?: 10
                        inactivityEnabled =
                            doc.getBoolean("inactivityEnabled") ?: true

                    }
                }
        }

        return START_STICKY


    }

    // ════════════════════════════════════════════════════════════════════════
    // Accelerometer
    // ════════════════════════════════════════════════════════════════════════
    override fun onSensorChanged(event: SensorEvent) {

        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val (x, y, z) = event.values

        val acceleration = sqrt(x * x + y * y + z * z)
        val sample = floatArrayOf(x, y, z)

        window.addLast(sample)

        if (window.size > 50) {
            window.removeFirst()
        }

        // Mouvement détecté
        if (acceleration > 2f) {
            lastMovementTime = System.currentTimeMillis()
            inactivityAlertSent = false
        }

        // Chute détectée
        if (predictFall()) {

            val now = System.currentTimeMillis()

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
    // Chute détectée
    // ════════════════════════════════════════════════════════════════════════
    private suspend fun handleFallDetected() {

        val userResponded =
            askUserIfOk("⚠️ Chute détectée !")

        if (!userResponded) {

            // Alert Firebase
            sendAlertToFirebase()

            // Appel superviseur
            delay(2000)

            callSuperviseur()
        }

        isHandlingFall = false
    }
    private suspend fun handleInactivityDetected() {

        val userResponded =
            askUserIfOk("⚠️ Inactivité prolongée !")

        if (!userResponded) {

            sendInactivityAlert()

            delay(2000)

            callSuperviseur()
        }

        inactivityAlertSent = true
    }

    // ════════════════════════════════════════════════════════════════════════
    // Confirmation utilisateur
    // ════════════════════════════════════════════════════════════════════════
    private suspend fun askUserIfOk(
        title: String
    ): Boolean {

        repeat(3) { attempt ->

            speakOut(
                "$surveilleeName, êtes-vous OK ? " +
                        "Appuyez sur le bouton si vous allez bien."
            )

            showConfirmationNotification(
                attempt + 1,
                title
            )

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
    // Inactivité prolongée
    // ════════════════════════════════════════════════════════════════════════
    private fun sendInactivityAlert() {

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("Associations")
            .whereEqualTo("superviseeId", uid)
            .get()
            .addOnSuccessListener { result ->

                val superviseurId =
                    result.documents.firstOrNull()
                        ?.getString("superviseurId") ?: ""

                val alert = hashMapOf(
                    "superviseeId" to uid,
                    "superviseeName" to surveilleeName,
                    "superviseurId" to superviseurId,
                    "type" to "warning",
                    "message" to
                            "⚠️ Inactivité prolongée détectée automatiquement",
                    "timestamp" to System.currentTimeMillis(),
                    "status" to "unconfirmed"
                )

                FirebaseFirestore.getInstance()
                    .collection("Alerts")
                    .add(alert)
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

                val superviseurId =
                    result.documents.firstOrNull()
                        ?.getString("superviseurId")
                        ?: return@addOnSuccessListener

                FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(superviseurId)
                    .get()
                    .addOnSuccessListener { userDoc ->

                        val phone =
                            userDoc.getString("phone")
                                ?: return@addOnSuccessListener

                        val callIntent =
                            Intent(Intent.ACTION_CALL).apply {
                                data = Uri.parse("tel:$phone")
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

                val superviseurId =
                    result.documents.firstOrNull()
                        ?.getString("superviseurId") ?: ""

                val alert = hashMapOf(
                    "superviseeId" to uid,
                    "superviseeName" to surveilleeName,
                    "superviseurId" to superviseurId,
                    "type" to "danger",
                    "message" to "⚠️ Chute détectée automatiquement",
                    "timestamp" to System.currentTimeMillis(),
                    "status" to "unconfirmed"
                )

                FirebaseFirestore.getInstance()
                    .collection("Alerts")
                    .add(alert)
            }
    }

    // ════════════════════════════════════════════════════════════════════════
    // TTS
    // ════════════════════════════════════════════════════════════════════════
    private fun speakOut(text: String) {

        if (ttsReady) {

            tts.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "fall_confirm"
            )
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Notification confirmation
    // ════════════════════════════════════════════════════════════════════════
    private fun showConfirmationNotification(
        attempt: Int,
        title: String
    ){
        val okIntent =
            Intent(this, FallConfirmReceiver::class.java).apply {
                action = ACTION_USER_OK
            }

        val okPending = PendingIntent.getBroadcast(
            this,
            0,
            okIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notif =
            NotificationCompat.Builder(this, "alarm_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("$title ($attempt/3)")
                .setContentText(
                    "Êtes-vous OK ? Appuyez si vous allez bien."
                )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .addAction(
                    android.R.drawable.ic_menu_call,
                    "✅ Je vais bien",
                    okPending
                )
                .setAutoCancel(false)
                .setOngoing(true)
                .build()

        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_CONFIRM_ID, notif)
    }

    private fun cancelConfirmationNotification() {

        getSystemService(NotificationManager::class.java)
            .cancel(NOTIF_CONFIRM_ID)
    }

    // ════════════════════════════════════════════════════════════════════════
    // Foreground notification
    // ════════════════════════════════════════════════════════════════════════
    private fun createNotification(): Notification {

        val channelId = "ai_guardian_service"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                channelId,
                "AI Guardian Service",
                NotificationManager.IMPORTANCE_LOW
            )

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("AI Guardian actif")
            .setContentText("Détection de chute en cours...")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = assets.openFd("fall_detection.tflite")
        val inputStream = fileDescriptor.createInputStream()
        val fileChannel = inputStream.channel

        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }
    private fun predictFall(): Boolean {

        if (window.size < 50) return false

        val input = Array(1) { Array(50) { FloatArray(3) } }

        val data = window.toList()

        for (i in 0 until 50) {
            input[0][i][0] = data[i][0]
            input[0][i][1] = data[i][1]
            input[0][i][2] = data[i][2]
        }

        val output = Array(1) { FloatArray(1) }

        tflite.run(input, output)

        // ───── AI confidence ─────
        val isHighConfidence = output[0][0] > 0.85f

        // ───── last sensor values (important) ─────
        val last = window.last()

        val x = last[0]
        val y = last[1]
        val z = last[2]

        // ───── physical rule (movement intensity) ─────
        val magnitude = kotlin.math.sqrt(x * x + y * y + z * z)
        val possibleFall = magnitude > 20f

        // ───── final decision ─────
        return isHighConfidence && possibleFall
    }

    private fun isSleepingTime(): Boolean {

        val calendar = Calendar.getInstance()

        val currentHour =
            calendar.get(Calendar.HOUR_OF_DAY)

        val currentMinute =
            calendar.get(Calendar.MINUTE)

        val currentTime =
            currentHour * 60 + currentMinute

        val wake =
            heureReveil.split(":")

        val sleep =
            heureSommeil.split(":")

        val wakeMinutes =
            wake[0].toInt() * 60 + wake[1].toInt()

        val sleepMinutes =
            sleep[0].toInt() * 60 + sleep[1].toInt()

        return if (sleepMinutes > wakeMinutes) {

            currentTime >= sleepMinutes ||
                    currentTime < wakeMinutes

        } else {

            currentTime in sleepMinutes until wakeMinutes
        }
    }

    companion object {

        @Volatile
        var userConfirmedOk = false

        const val ACTION_USER_OK =
            "com.example.ai_guardian.USER_OK"

        const val NOTIF_CONFIRM_ID = 42
    }

    override fun onDestroy() {
        super.onDestroy()

        sensorManager.unregisterListener(this)

        tts.shutdown()

        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}