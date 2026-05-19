package com.example.ai_guardian.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.speech.*
import androidx.core.app.NotificationCompat
import com.example.ai_guardian.MainActivity
import com.example.ai_guardian.utils.AppEvents
import com.example.ai_guardian.utils.KeywordClassifier
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*

class KeywordListenerService : Service() {

    private var recognizer    : SpeechRecognizer? = null
    private lateinit var classifier: KeywordClassifier
    private val scope         = CoroutineScope(Dispatchers.Main)
    private var isRunning     = false
    private var isListening   = false
    private var lastAlertTime = 0L

    override fun onCreate() {
        super.onCreate()
        isRunning  = true
        classifier = KeywordClassifier(this)
        startForeground(NOTIF_ID, buildNotification())
        startListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isListening) startListening()
        return START_STICKY
    }

    // ════════════════════════════════════════════════════════════════════════
    // SpeechRecognizer loop
    // ════════════════════════════════════════════════════════════════════════
    private fun startListening() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) return

        createRecognizer()
        listenOnce()
    }

    private fun listenOnce() {
        if (isListening || !isRunning) return
        isListening = true

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-TN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-TN")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("ar", "fr-FR"))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        }

        try { recognizer?.startListening(intent) }
        catch (e: Exception) {
            isListening = false
            restartAfterDelay(1000)
        }
    }

    private fun createRecognizer() {
        try { recognizer?.destroy(); recognizer = null } catch (e: Exception) {}

        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {

                override fun onResults(bundle: Bundle?) {
                    isListening = false
                    val matches = bundle
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?: return restartAfterDelay(500)

                    android.util.Log.d("KeywordService", "📝 Heard: $matches")

                    for (text in matches) {

                        // ✅ 1 — Keyword matching مباشر (أسرع)
                        val textLower = text.lowercase()
                        val emergencyWords = listOf("عاوني", "ساعدني", "نجدة", "طحت", "مريض", "خطر", "نجدني")
                        val distressWords  = listOf("نحس بدوخة", "تعبت", "مش بخير", "دوخة", "نبكي", "خايف")

                        if (emergencyWords.any { textLower.contains(it) }) {
                            handleDetection(text, isEmergency = true)
                            return
                        }
                        if (distressWords.any { textLower.contains(it) }) {
                            handleDetection(text, isEmergency = false)
                            return
                        }

                        // ✅ 2 — ML model (أدق للجمل الطويلة)
                        val result = classifier.classify(text)
                        android.util.Log.d("KeywordService",
                            "🧠 ${result.intent} (${"%.0f".format(result.confidence*100)}%) → $text")

                        when (result.intent) {
                            KeywordClassifier.Intent.EMERGENCY -> { handleDetection(text, true);  return }
                            KeywordClassifier.Intent.DISTRESS  -> { handleDetection(text, false); return }
                            KeywordClassifier.Intent.NORMAL    -> {}
                        }
                    }
                    restartAfterDelay(300)
                }

                override fun onError(errorCode: Int) {
                    isListening = false
                    android.util.Log.d("KeywordService", "onError: $errorCode")
                    restartAfterDelay(
                        when (errorCode) {
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 2000L
                            SpeechRecognizer.ERROR_NETWORK         -> 5000L
                            else                                   -> 500L
                        }
                    )
                }

                override fun onReadyForSpeech(p0: Bundle?) {
                    android.util.Log.d("KeywordService", "✅ Ready")
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

    private fun restartAfterDelay(delayMs: Long = 500L) {
        if (!isRunning) return
        scope.launch {
            delay(delayMs)
            if (isRunning) {
                createRecognizer()
                listenOnce()
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Detection
    // ════════════════════════════════════════════════════════════════════════
    private fun handleDetection(text: String, isEmergency: Boolean) {
        val now = System.currentTimeMillis()
        if (now - lastAlertTime < 15_000) return
        lastAlertTime = now

        android.util.Log.d("KeywordService",
            if (isEmergency) "🚨 EMERGENCY: $text" else "⚠️ DISTRESS: $text")

        openApp()
        AppEvents.emit(AppEvents.Event.OpenChatbotEmergency(text))

    }

    private fun openApp() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
    }



    private fun buildNotification(): Notification {
        val channelId = "keyword_listener"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel(channelId, "Écoute mots-clés",
                NotificationManager.IMPORTANCE_MIN).also {
                getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(it)
            }
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("AI Guardian")
            .setContentText("🎤 Écoute active...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true).build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
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
            else context.startService(intent)
        }
        fun stop(context: Context) {
            context.stopService(Intent(context, KeywordListenerService::class.java))
        }
    }
}