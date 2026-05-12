package com.example.ai_guardian.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.example.ai_guardian.MainActivity
import com.example.ai_guardian.utils.AppEvents

import com.example.ai_guardian.utils.KeywordClassifier
import com.example.ai_guardian.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*

class KeywordListenerService : Service() {

    private var recognizer  : SpeechRecognizer? = null
    private lateinit var classifier: KeywordClassifier
    private val scope       = CoroutineScope(Dispatchers.Main)
    private var isListening = false
    private var lastAlertTime = 0L
    private var isRunning   = false

    override fun onCreate() {
        super.onCreate()
        isRunning  = true   // ✅
        classifier = KeywordClassifier(this)
        startForeground(NOTIF_ID, buildNotification())
        startKeywordListening()
    }

    // ════════════════════════════════════════════════════════════════════════
    // Boucle d'écoute
    // ════════════════════════════════════════════════════════════════════════
    private fun startKeywordListening() {
        createRecognizer()
        listenOnce()
    }

    private fun listenOnce() {
        if (isListening) return

        // ✅ تحقق إذا Google Speech متاح
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            android.util.Log.e("KeywordService", "Speech recognition not available!")
            restartAfterDelay(10_000)
            return
        }

        isListening = true

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-TN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-TN")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
            // ✅ هذا مهم جداً للـ background
            putExtra("android.speech.extra.DICTATION_MODE", true)
        }

        try {
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            android.util.Log.e("KeywordService", "startListening failed: ${e.message}")
            isListening = false
            restartAfterDelay(2000)
        }
    }

    private fun createRecognizer() {
        try {
            recognizer?.destroy()
            recognizer = null
        } catch (e: Exception) { }

        // ✅ delay على الـ main thread بدون Thread.sleep
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {

                override fun onReadyForSpeech(p0: Bundle?) {
                    android.util.Log.d("KeywordService", "✅ Ready to listen")
                }

                override fun onResults(bundle: Bundle?) {
                    isListening = false
                    val matches = bundle
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?: return restartAfterDelay(1000)

                    android.util.Log.d("KeywordService", "Heard: $matches")

                    for (text in matches) {
                        val result = classifier.classify(text)
                        android.util.Log.d("KeywordService",
                            "→ ${result.intent} (${"%.0f".format(result.confidence * 100)}%) : $text")
                        when (result.intent) {
                            KeywordClassifier.Intent.EMERGENCY -> {
                                handleEmergency(text, result.confidence)
                                return
                            }
                            KeywordClassifier.Intent.DISTRESS -> {
                                handleDistress(text, result.confidence)
                                return
                            }
                            KeywordClassifier.Intent.NORMAL -> {}
                        }
                    }
                    restartAfterDelay(500)
                }

                override fun onError(errorCode: Int) {
                    isListening = false
                    android.util.Log.d("KeywordService", "onError: $errorCode")
                    when (errorCode) {
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                            // ✅ destroy complètement et recréer
                            try { recognizer?.destroy(); recognizer = null } catch (e: Exception) {}
                            restartAfterDelay(3000)
                        }
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            android.util.Log.e("KeywordService", "❌ Permission RECORD_AUDIO manquante!")
                            // لا تعيد المحاولة
                        }
                        else -> restartAfterDelay(
                            when (errorCode) {
                                SpeechRecognizer.ERROR_NETWORK         -> 5000L
                                SpeechRecognizer.ERROR_NO_MATCH        -> 300L
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT  -> 300L
                                else                                   -> 1000L
                            }
                        )
                    }
                }

                override fun onBeginningOfSpeech()          {}
                override fun onRmsChanged(v: Float)         {}
                override fun onBufferReceived(b: ByteArray?){}
                override fun onEndOfSpeech()                {}
                override fun onPartialResults(b: Bundle?)   {}
                override fun onEvent(t: Int, b: Bundle?)    {}
            })
        }
    }

    private fun restartAfterDelay(delayMs: Long = 800L) {
        if (!isRunning) return  // ✅ لا تعيد إذا الـ service وقف
        scope.launch {
            delay(delayMs)
            if (isRunning) {
                createRecognizer()
                listenOnce()
            }
        }
    }



    // ════════════════════════════════════════════════════════════════════════
    // Urgence → Firebase + ouvrir chatbot
    // ════════════════════════════════════════════════════════════════════════
    private fun handleEmergency(text: String, confidence: Float) {
        val now = System.currentTimeMillis()
        if (now - lastAlertTime < 30_000) return restartAfterDelay()
        lastAlertTime = now

        // ✅ وقف الاستماع قبل ما يفتح الشات
        recognizer?.stopListening()
        isListening = false

        openApp()
        AppEvents.emit(AppEvents.Event.OpenChatbotEmergency(text))
        sendFirebaseAlert(text, "danger")

        // ✅ رجع يسمع بعد 30 ثانية
        scope.launch {
            delay(30_000)
            createRecognizer()
            listenOnce()
        }
    }

    private fun handleDistress(text: String, confidence: Float) {
        val now = System.currentTimeMillis()
        if (now - lastAlertTime < 30_000) return restartAfterDelay()
        lastAlertTime = now

        // ✅ نفس الشيء
        recognizer?.stopListening()
        isListening = false

        openApp()
        AppEvents.emit(AppEvents.Event.OpenChatbotEmergency(text))

        scope.launch {
            delay(30_000)
            createRecognizer()
            listenOnce()
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Ouvrir MainActivity
    // ════════════════════════════════════════════════════════════════════════
    private fun openApp() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    // Firebase Alert
    // ════════════════════════════════════════════════════════════════════════
    private fun sendFirebaseAlert(text: String, type: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("Associations")
            .whereEqualTo("superviseeId", uid)
            .get()
            .addOnSuccessListener { result ->
                val superviseurId = result.documents
                    .firstOrNull()?.getString("superviseurId") ?: ""

                FirebaseFirestore.getInstance()
                    .collection("Alerts")
                    .add(hashMapOf(
                        "superviseeId"  to uid,
                        "superviseurId" to superviseurId,
                        "type"          to type,
                        "message"       to "🎤 Mot-clé vocal détecté : \"$text\"",
                        "timestamp"     to System.currentTimeMillis(),
                        "status"        to "unconfirmed"
                    ))
            }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Notification foreground
    // ════════════════════════════════════════════════════════════════════════
    private fun buildNotification(): Notification {
        val channelId = "keyword_listener"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel(
                channelId,
                "Écoute mots-clés",
                NotificationManager.IMPORTANCE_MIN
            ).also {
                getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(it)
            }
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("AI Guardian")
            .setContentText("🎤 Écoute active...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false   // ✅
        recognizer?.destroy()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIF_ID = 55

        fun start(context: Context) {
            val intent = Intent(context, KeywordListenerService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else
                context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, KeywordListenerService::class.java))
        }
    }
}