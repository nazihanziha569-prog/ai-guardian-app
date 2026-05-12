// viewmodel/ChatViewModel.kt
package com.example.ai_guardian.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai_guardian.audio.ChatTtsManager
import com.example.ai_guardian.audio.SpeechRecognitionManager
import com.example.ai_guardian.data.model.ChatMessage
import com.example.ai_guardian.data.model.Config
import com.example.ai_guardian.data.model.Rappel
import com.example.ai_guardian.repository.ChatRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository = ChatRepository()
) : ViewModel() {

    // ── Messages ──────────────────────────────────────────────────────────
    private val _messages   = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isLoading  = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking = _isSpeaking.asStateFlow()

    private val _rappels    = MutableStateFlow<List<Rappel>>(emptyList())
    val rappels = _rappels.asStateFlow()

    // ── Speech ────────────────────────────────────────────────────────────
    private val _speechState = MutableStateFlow(SpeechRecognitionManager.SpeechState.IDLE)
    val speechState = _speechState.asStateFlow()

    private val _voiceMode   = MutableStateFlow(false)
    val voiceMode = _voiceMode.asStateFlow()

    // ── Managers ──────────────────────────────────────────────────────────
    private var ttsManager   : ChatTtsManager?          = null
    private var speechManager: SpeechRecognitionManager? = null

    // ── Config courante (pour sendMessageAndThenListen) ───────────────────
    private var currentConfig  : Config? = null
    private var currentUserName: String  = "Utilisateur"

    init {
        repository.listenRappels { _rappels.value = it }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Init TTS
    // ════════════════════════════════════════════════════════════════════════
    fun initTts(context: Context) {
        if (ttsManager != null) return
        ttsManager = ChatTtsManager(context).apply {
            onSpeakingChanged = { speaking -> _isSpeaking.value = speaking }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Init Speech Recognition
    // ════════════════════════════════════════════════════════════════════════
    fun initSpeech(context: Context) {
        if (speechManager != null) return
        speechManager = SpeechRecognitionManager(
            context      = context,
            onResult     = { text -> sendMessageAndThenListen(text) },
            onError      = { msg  ->
                addBotMessage("⚠️ $msg")
                if (_voiceMode.value) {
                    viewModelScope.launch {
                        delay(1500)
                        startListening()
                    }
                }
            },
            onStateChange = { state -> _speechState.value = state }
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    // Micro controls
    // ════════════════════════════════════════════════════════════════════════
    fun startListening() {
        viewModelScope.launch {
            delay(800)  // ✅ انتظر حتى KeywordService يوقف
            speechManager?.startListening()
        }
    }

    fun stopListening() {
        speechManager?.stopListening()
        _speechState.value = SpeechRecognitionManager.SpeechState.IDLE
    }

    fun toggleVoiceMode(context: Context) {
        initTts(context)
        initSpeech(context)
        _voiceMode.value = !_voiceMode.value
        if (_voiceMode.value) {
            ttsManager?.speak("وضع المحادثة الصوتية مفعّل، تكلم!")
            viewModelScope.launch {
                delay(1800)
                startListening()
            }
        } else {
            stopListening()
            ttsManager?.stop()
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Message de bienvenue
    // ════════════════════════════════════════════════════════════════════════
    fun sendWelcome(userName: String) {
        if (_messages.value.isNotEmpty()) return
        currentUserName = userName
        val welcome = "مرحبا $userName! أنا مساعدك الذكي. كيفاش تحس اليوم؟"
        addBotMessage(welcome)
        ttsManager?.speak(welcome)
    }

    // ════════════════════════════════════════════════════════════════════════
    // Envoyer message (depuis texte)
    // ════════════════════════════════════════════════════════════════════════
    fun sendMessage(text: String, userName: String, config: Config?) {
        currentUserName = userName
        currentConfig   = config
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isLoading.value) return
        addUserMessage(trimmed)                                      // ✅ مرة وحدة
        sendMessageAndThenListen(trimmed, addToHistory = false)      // ✅ لا يضيف مرة ثانية
    }

    // ════════════════════════════════════════════════════════════════════════
    // Envoi interne + réécoute si mode vocal
    // ════════════════════════════════════════════════════════════════════════
    private fun sendMessageAndThenListen(text: String, addToHistory: Boolean = true) {
        if (_isLoading.value) return
        if (addToHistory) addUserMessage(text)

        viewModelScope.launch {
            _isLoading.value = true

            val result = repository.sendMessage(
                userMessage = text,
                userName    = currentUserName,
                config      = currentConfig,
                rappels     = _rappels.value,
                history     = _messages.value
            )

            result
                .onSuccess { reply ->
                    addBotMessage(reply)
                    ttsManager?.speak(reply)

                    if (_voiceMode.value) {
                        // Estimer durée TTS : ~80ms par caractère
                        val estimatedMs = (reply.length * 80L).coerceIn(2000L, 15000L)
                        delay(estimatedMs)
                        if (_voiceMode.value) startListening()
                    }
                }
                .onFailure {
                    val err = "عذراً، فما مشكلة في الاتصال. حاول مرة أخرى."
                    addBotMessage(err)
                    ttsManager?.speak(err)
                    if (_voiceMode.value) {
                        delay(3000)
                        startListening()
                    }
                }

            _isLoading.value = false
        }
    }

    fun sendWelcomeEmergency(detectedText: String = "") {
        val msg = if (detectedText.isNotBlank())
            "🚨 سمعتك تقول \"$detectedText\" — أنا هنا معك. كيفاش تحس؟ هل تحتاج أتصل بالمشرف؟"
        else
            "🚨 سمعتك تطلب مساعدة! أنا هنا — كيفاش تحس؟"

        addBotMessage(msg)
        ttsManager?.speak(msg)
    }


    // ════════════════════════════════════════════════════════════════════════
    // TTS stop
    // ════════════════════════════════════════════════════════════════════════
    fun stopSpeaking() {
        ttsManager?.stop()
    }

    // ════════════════════════════════════════════════════════════════════════
    // Cleanup
    // ════════════════════════════════════════════════════════════════════════
    override fun onCleared() {
        super.onCleared()
        ttsManager?.shutdown()
        speechManager?.destroy()
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private fun addUserMessage(text: String) {
        _messages.value = _messages.value + ChatMessage(content = text, isUser = true)
    }

    private fun addBotMessage(text: String) {
        _messages.value = _messages.value + ChatMessage(content = text, isUser = false)
    }
    companion object {
        // ✅ true = KeywordService شغال، false = الشات يقدر يسمع
        var keywordServiceActive = false
    }
}