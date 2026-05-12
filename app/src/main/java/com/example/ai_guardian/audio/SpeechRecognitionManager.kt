// audio/SpeechRecognitionManager.kt
package com.example.ai_guardian.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.ai_guardian.viewmodel.ChatViewModel
import java.util.Locale

class SpeechRecognitionManager(
    private val context: Context,
    private val onResult : (String) -> Unit,
    private val onError  : (String) -> Unit,
    private val onStateChange: (SpeechState) -> Unit
) {
    enum class SpeechState { IDLE, LISTENING, PROCESSING }

    private var recognizer: SpeechRecognizer? = null

    init { createRecognizer() }

    private fun createRecognizer() {
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p: Bundle?)   { onStateChange(SpeechState.LISTENING) }
                override fun onBeginningOfSpeech()          {}
                override fun onRmsChanged(v: Float)         {}
                override fun onBufferReceived(b: ByteArray?){}
                override fun onEndOfSpeech()                { onStateChange(SpeechState.PROCESSING) }
                override fun onPartialResults(b: Bundle?)   {}
                override fun onEvent(t: Int, b: Bundle?)    {}

                override fun onResults(bundle: Bundle?) {
                    val matches = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    onStateChange(SpeechState.IDLE)
                    if (text.isNotBlank()) onResult(text)
                    else onError("لم أفهم، حاول مرة أخرى")
                }

                override fun onError(errorCode: Int) {
                    onStateChange(SpeechState.IDLE)
                    val msg = when (errorCode) {
                        SpeechRecognizer.ERROR_NO_MATCH       -> "لم أفهم، حاول مرة أخرى"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "لم يتم اكتشاف صوت"
                        SpeechRecognizer.ERROR_NETWORK        -> "مشكلة في الاتصال"
                        else                                  -> "خطأ في التعرف على الصوت"
                    }
                    onError(msg)
                }
            })
        }
    }

    fun startListening(locale: Locale = Locale("ar", "TN")) {
        if (ChatViewModel.keywordServiceActive) return
        // إعادة إنشاء المعرِّف لأن Android لا يسمح بإعادة الاستخدام المتكرر
        createRecognizer()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        recognizer?.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }
}