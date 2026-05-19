package com.example.ai_guardian.service

import android.app.*
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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

    // ─── Speech ──────────────────────────────────────────────────────────
    private var speechResult: String? = null   // null = en attente, "" = silence
    private var speechDone   = false

    companion object {
        @Volatile var userConfirmed = false
        private const val CHANNEL_ID       = "alert_channel"
        private const val NOTIF_FOREGROUND = 2222
        private const val NOTIF_CONFIRM_ID = 1002

        // mots positifs
        private val POSITIVE = listOf(
            "oui", "ça va",  "لا باس", "بخير", "نعم", "ايه",
            "bien", "مليح", "okay", "ok", "oui","صفا","حمد لله"
        )
        // mots négatifs / urgence
        private val NEGATIVE = listOf(
            "non", "aïe", "aie", "aïe", "لا", "نجعني", "عاونني", "يا ربي", "اوجعني",
            "مريض", "طحت", "au secours", "help", "اغيثوني","aaah","aaaa","اااااااا")
    }

    // ════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ════════════════════════════════════════════════════════════════════════
    override fun onCreate() {
        super.onCreate()
        createChannel()
        scope = CoroutineScope(Dispatchers.Main)
        tts   = TextToSpeech(this, this)
        startForeground(NOTIF_FOREGROUND,
            buildSilentNotification("AI Guardian", "Surveillance active..."))
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

    // fil onCreate aw onStartCommand — قبل handleFall/handleInactivity
    private fun stopKeywordService() {
        val intent = Intent(this, KeywordListenerService::class.java)
        stopService(intent)
    }

    private fun restartKeywordService() {
        val intent = Intent(this, KeywordListenerService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private suspend fun speakAndWait(text: String) {
        if (!ttsReady) { delay(500); return }
        var done = false
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle().also {}, "tts_id")
        // attend fin TTS (max 6s)
        repeat(60) {
            if (!tts.isSpeaking) { done = true; return@repeat }
            delay(100)
        }
        delay(200)
    }

    // ════════════════════════════════════════════════════════════════════════
    // Speech Recognition
    // ════════════════════════════════════════════════════════════════════════
    private suspend fun listenForResponse(): String {
        speechResult = null
        speechDone   = false

        val recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-TN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-TN")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                speechResult = matches?.firstOrNull()?.lowercase() ?: ""
                speechDone   = true
                recognizer.destroy()
            }
            override fun onError(error: Int) {
                speechResult = ""
                speechDone   = true
                recognizer.destroy()
            }
            override fun onReadyForSpeech(p: Bundle?)    {}
            override fun onBeginningOfSpeech()           {}
            override fun onRmsChanged(v: Float)          {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech()                 {}
            override fun onPartialResults(p: Bundle?)    {}
            override fun onEvent(t: Int, p: Bundle?)     {}
        })

        recognizer.startListening(intent)

        // attend résultat max 7 secondes
        repeat(70) {
            if (speechDone) return@repeat
            delay(100)
        }
        if (!speechDone) recognizer.destroy()

        return speechResult ?: ""
    }

    // ════════════════════════════════════════════════════════════════════════
    // Flow principal — 3 tentatives (TTS + écoute)
    // ════════════════════════════════════════════════════════════════════════
    private suspend fun askUserIfOk(title: String): Boolean {
        userConfirmed = false

        repeat(3) { attempt ->

            // 1. TTS parle
            speakAndWait(
                "$surveilleeName, êtes-vous OK ? " +
                        "Dites oui si vous allez bien, ou appuyez sur le bouton."
            )
            delay(500)

            // 2. Notification bouton (backup)
            showConfirmationNotification(attempt + 1, title)

            // 3. Écoute sawt
            val heard = listenForResponse()

            android.util.Log.d("ALERT_SPEECH", "Heard: '$heard'")

            when {
                // bouton dosé pendant l'écoute
                userConfirmed -> {
                    cancelConfirmationNotification()
                    return true
                }
                // réponse positive
                POSITIVE.any { heard.contains(it) } -> {
                    cancelConfirmationNotification()
                    speakAndWait("Très bien $surveilleeName, je reste à votre écoute.")
                    return true
                }
                // réponse négative → alerte immédiate
                NEGATIVE.any { heard.contains(it) } -> {
                    cancelConfirmationNotification()
                    speakAndWait("J'appelle votre superviseur immédiatement.")
                    return false
                }
                // silence → prochaine tentative
                else -> { /* continue repeat */ }
            }
        }

        cancelConfirmationNotification()
        return false
    }

    // ════════════════════════════════════════════════════════════════════════
    // FALL
    // ════════════════════════════════════════════════════════════════════════
    private suspend fun handleFall() {
        stopKeywordService()
        val userOk = askUserIfOk("⚠️ Chute détectée !")
        if (!userOk) {
            sendAlertToFirebase("danger", "⚠️ Chute détectée automatiquement")
            delay(2_000)
            callSuperviseur()
        }
        restartKeywordService() // ✅ yarja3
        stopSelf()
    }

    // ════════════════════════════════════════════════════════════════════════
    // INACTIVITY
    // ════════════════════════════════════════════════════════════════════════
    private suspend fun handleInactivity() {
        stopKeywordService()
        val userOk = askUserIfOk("⚠️ Inactivité prolongée !")
        if (!userOk) {
            sendAlertToFirebase("warning", "⚠️ Inactivité prolongée détectée automatiquement")
            delay(2_000)
            callSuperviseur()
        }
        restartKeywordService() // ✅ yarja3
        stopSelf()
    }

    // ════════════════════════════════════════════════════════════════════════
    // Notification bouton
    // ════════════════════════════════════════════════════════════════════════
    private fun showConfirmationNotification(attempt: Int, title: String) {
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0,
            Intent(this, AlertReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        getSystemService(NotificationManager::class.java).notify(
            NOTIF_CONFIRM_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("$title ($attempt/3)")
                .setContentText("Parlez ou appuyez si vous allez bien.")
                .addAction(android.R.drawable.ic_menu_send, "✅ Je vais bien", pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(false)
                .setOngoing(true)
                .build()
        )
    }

    private fun cancelConfirmationNotification() {
        getSystemService(NotificationManager::class.java).cancel(NOTIF_CONFIRM_ID)
    }

    // ════════════════════════════════════════════════════════════════════════
    // Firebase
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

                // ✅ Save alert in Firestore
                FirebaseFirestore.getInstance()
                    .collection("Alerts")
                    .add(
                        hashMapOf(
                            "superviseeId"   to uid,
                            "superviseeName" to surveilleeName,
                            "superviseurId"  to superviseurId,
                            "type"           to type,
                            "message"        to message,
                            "timestamp"      to System.currentTimeMillis(),
                            "status"         to "unconfirmed"
                        )
                    )

                // ✅ Get superviseur FCM token
                FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(superviseurId)
                    .get()
                    .addOnSuccessListener { doc ->

                        val token = doc.getString("fcmToken")
                            ?: return@addOnSuccessListener

                        // ✅ Send FCM Notification
                        sendFcmV1(
                            token = token,

                            title = when(type) {
                                "danger" -> "🚨 SOS - Chute détectée"
                                "warning" -> "⚠️ Inactivité prolongée"
                                else -> "🔔 AI Guardian Alert"
                            },

                            body = when(type) {

                                "danger" ->
                                    "$surveilleeName ne répond pas après une chute détectée."

                                "warning" ->
                                    "$surveilleeName ne répond pas après une longue période d’inactivité."

                                else ->
                                    "$surveilleeName a besoin d’aide."
                            },

                            type = type,
                            context = applicationContext
                        )
                    }
            }
    }


    private fun callSuperviseur() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("Associations").whereEqualTo("superviseeId", uid).get()
            .addOnSuccessListener { result ->
                val superviseurId = result.documents.firstOrNull()
                    ?.getString("superviseurId") ?: return@addOnSuccessListener
                FirebaseFirestore.getInstance()
                    .collection("Users").document(superviseurId).get()
                    .addOnSuccessListener { doc ->
                        val phone = doc.getString("phone") ?: return@addOnSuccessListener
                        startActivity(Intent(Intent.ACTION_CALL).apply {
                            data  = android.net.Uri.parse("tel:$phone")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }
            }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════════════════
    private fun buildSilentNotification(title: String, text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title).setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW).build()
    }

    private fun createChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "AI Alerts", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Alertes chute et inactivité"; enableVibration(true) }
            )
        }
    }
}
fun sendFcmV1(
    token: String,
    title: String,
    body: String,
    type: String,
    context: android.content.Context
) {

    Thread {

        try {

            val stream = context.assets.open("service-account.json")

            val credentials = com.google.auth.oauth2.GoogleCredentials
                .fromStream(stream)
                .createScoped("https://www.googleapis.com/auth/firebase.messaging")

            credentials.refreshIfExpired()

            val accessToken = credentials.accessToken.tokenValue

            val projectId = "ai-guardian-89b08"

            val url = java.net.URL(
                "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"
            )

            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {

                requestMethod = "POST"

                setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                setRequestProperty(
                    "Authorization",
                    "Bearer $accessToken"
                )

                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
            }

            val jsonBody = org.json.JSONObject().apply {

                put("message", org.json.JSONObject().apply {

                    put("token", token)

                    put("notification", org.json.JSONObject().apply {

                        put("title", title)
                        put("body", body)
                    })

                    put("android", org.json.JSONObject().apply {

                        put("priority", "high")
                    })

                    put("data", org.json.JSONObject().apply {

                        put("type", type)
                        put("body", body)
                    })
                })
            }

            conn.outputStream.use {

                it.write(jsonBody.toString().toByteArray())
            }

            val responseCode = conn.responseCode

            val response =
                if (responseCode == 200)
                    conn.inputStream.bufferedReader().readText()
                else
                    conn.errorStream.bufferedReader().readText()

            android.util.Log.d(
                "FCM_V1",
                "Response $responseCode : $response"
            )

        } catch (e: Exception) {

            android.util.Log.e(
                "FCM_V1",
                "❌ ${e.message}"
            )
        }

    }.start()
}