package com.example.ai_guardian.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.*

class ChatTtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false

    var onSpeakingChanged: ((Boolean) -> Unit)? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("ar")
                isReady = true

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?)  {
                        onSpeakingChanged?.invoke(true)
                    }
                    override fun onDone(utteranceId: String?)   {
                        onSpeakingChanged?.invoke(false)
                    }
                    override fun onError(utteranceId: String?)  {
                        onSpeakingChanged?.invoke(false)
                    }
                })
            }
        }
    }

    fun speak(text: String) {
        if (!isReady) return
        // nettoyer le texte — enlever emojis et markdown pour TTS
        val clean = text
            .replace(Regex("[\\p{So}\\p{Cn}]"), "")  // emojis
            .replace(Regex("[*_`#]"), "")              // markdown
            .trim()
        if (clean.isBlank()) return
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "chat_tts")
    }

    fun stop() {
        tts?.stop()
        onSpeakingChanged?.invoke(false)
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}