package com.example.ai_guardian.repository

import com.example.ai_guardian.BuildConfig
import com.example.ai_guardian.data.model.ChatMessage
import com.example.ai_guardian.data.model.Config
import com.example.ai_guardian.data.model.Rappel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class ChatRepository {

    private val db = FirebaseFirestore.getInstance()

    // ════════════════════════════════════════════════════════════════════════
    // APIs — بالترتيب : Claude → Gemini → Groq
    // ════════════════════════════════════════════════════════════════════════
    private val apis = listOf(
        ::callClaude,
        ::callGemini,
        ::callGroq
    )

    fun listenRappels(onUpdate: (List<Rappel>) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("Rappels")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull {
                        it.toObject(Rappel::class.java)?.copy(id = it.id)
                    }
                    onUpdate(list)
                }
            }
    }

    // ════════════════════════════════════════════════════════════════════════
    // sendMessage — يجرب API بعد API
    // ════════════════════════════════════════════════════════════════════════
    suspend fun sendMessage(
        userMessage: String,
        userName   : String,
        config     : Config?,
        rappels    : List<Rappel>,
        history    : List<ChatMessage>
    ): Result<String> = withContext(Dispatchers.IO) {

        val systemPrompt = buildSystemPrompt(userName, config, rappels)
        var lastError: Exception? = null

        for (api in apis) {
            try {
                val result = api(userMessage, systemPrompt, history)
                if (result.isSuccess) {
                    android.util.Log.d("ChatRepo", "✅ ${api.name} succeeded")
                    return@withContext result
                }
            } catch (e: Exception) {
                android.util.Log.w("ChatRepo", "⚠️ ${api.name} failed: ${e.message}")
                lastError = e
            }
        }

        Result.failure(lastError ?: Exception("All APIs failed"))
    }

    // ════════════════════════════════════════════════════════════════════════
    // API 1 — Claude (Anthropic)
    // ════════════════════════════════════════════════════════════════════════
    private fun callClaude(
        userMessage : String,
        systemPrompt: String,
        history     : List<ChatMessage>
    ): Result<String> {
        val apiKey = BuildConfig.ANTHROPIC_KEY
        if (apiKey.isBlank()) return Result.failure(Exception("No Claude key"))

        val messagesArray = JSONArray()
        history.takeLast(10).forEach { msg ->
            messagesArray.put(JSONObject().apply {
                put("role",    if (msg.isUser) "user" else "assistant")
                put("content", msg.content)
            })
        }
        messagesArray.put(JSONObject().apply {
            put("role",    "user")
            put("content", userMessage)
        })

        val body = JSONObject().apply {
            put("model",      "claude-haiku-4-5-20251001")
            put("max_tokens", 400)
            put("system",     systemPrompt)
            put("messages",   messagesArray)
        }

        val conn = openConnection(
            url     = "https://api.anthropic.com/v1/messages",
            headers = mapOf(
                "Content-Type"      to "application/json",
                "anthropic-version" to "2023-06-01",
                "x-api-key"         to apiKey
            ),
            body = body.toString()
        )

        if (conn.responseCode != 200) {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: ""
            android.util.Log.w("ChatRepo", "Claude error: $err")
            return Result.failure(Exception("Claude HTTP ${conn.responseCode}"))
        }

        val text = JSONObject(conn.inputStream.bufferedReader().readText())
            .getJSONArray("content")
            .getJSONObject(0)
            .getString("text")

        return Result.success(text.trim())
    }

    // ════════════════════════════════════════════════════════════════════════
    // API 2 — Gemini (Google) — مجاني
    // ════════════════════════════════════════════════════════════════════════
    private fun callGemini(
        userMessage : String,
        systemPrompt: String,
        history     : List<ChatMessage>
    ): Result<String> {
        val apiKey = BuildConfig.GEMINI_KEY
        if (apiKey.isBlank()) return Result.failure(Exception("No Gemini key"))

        // Gemini يستخدم prompt موحد مع history
        val fullPrompt = buildString {
            append(systemPrompt)
            append("\n\n")
            history.takeLast(10).forEach { msg ->
                append(if (msg.isUser) "المستخدم: " else "المساعد: ")
                append(msg.content)
                append("\n")
            }
            append("المستخدم: $userMessage\nالمساعد:")
        }

        val body = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().apply {
                    put("parts", JSONArray().put(
                        JSONObject().apply { put("text", fullPrompt) }
                    ))
                }
            ))
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", 400)
                put("temperature",     0.7)
            })
        }

        val conn = openConnection(
            url     = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey",
            headers = mapOf("Content-Type" to "application/json"),
            body    = body.toString()
        )

        if (conn.responseCode != 200) {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: ""
            android.util.Log.w("ChatRepo", "Gemini error: $err")
            return Result.failure(Exception("Gemini HTTP ${conn.responseCode}"))
        }

        val text = JSONObject(conn.inputStream.bufferedReader().readText())
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")

        return Result.success(text.trim())
    }

    // ════════════════════════════════════════════════════════════════════════
    // API 3 — Groq (LLaMA) — مجاني
    // ════════════════════════════════════════════════════════════════════════
    private fun callGroq(
        userMessage : String,
        systemPrompt: String,
        history     : List<ChatMessage>
    ): Result<String> {
        val apiKey = BuildConfig.GROQ_KEY
        if (apiKey.isBlank()) return Result.failure(Exception("No Groq key"))

        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role",    "system")
                put("content", systemPrompt)
            })
            history.takeLast(10).forEach { msg ->
                put(JSONObject().apply {
                    put("role",    if (msg.isUser) "user" else "assistant")
                    put("content", msg.content)
                })
            }
            put(JSONObject().apply {
                put("role",    "user")
                put("content", userMessage)
            })
        }

        val body = JSONObject().apply {
            put("model",      "llama-3.1-8b-instant")
            put("max_tokens", 400)
            put("messages",   messagesArray)
        }

        val conn = openConnection(
            url     = "https://api.groq.com/openai/v1/chat/completions",
            headers = mapOf(
                "Content-Type"  to "application/json",
                "Authorization" to "Bearer $apiKey"
            ),
            body = body.toString()
        )

        if (conn.responseCode != 200) {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: ""
            android.util.Log.w("ChatRepo", "Groq error: $err")
            return Result.failure(Exception("Groq HTTP ${conn.responseCode}"))
        }

        val text = JSONObject(conn.inputStream.bufferedReader().readText())
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")

        return Result.success(text.trim())
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helper — فتح connection
    // ════════════════════════════════════════════════════════════════════════
    private fun openConnection(
        url    : String,
        headers: Map<String, String>,
        body   : String
    ): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
            doOutput       = true
            connectTimeout = 20_000
            readTimeout    = 40_000
            outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // System Prompt
    // ════════════════════════════════════════════════════════════════════════
    private fun buildSystemPrompt(
        userName: String,
        config  : Config?,
        rappels : List<Rappel>
    ): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = sdf.format(Date())
        return buildString {
            append("أنت مساعد صحي ذكي اسمه AI Guardian، ")
            append("متخصص في مساعدة كبار السن والأشخاص ذوي الاحتياجات الخاصة.\n\n")
            append("=== معلومات المستخدم ===\n")
            append("الاسم: $userName\n")
            config?.let {
                if (it.age > 0)              append("العمر: ${it.age} سنة\n")
                if (it.maladies.isNotBlank()) append("الأمراض: ${it.maladies}\n")
                append("وقت الاستيقاظ: ${it.heureReveil}\n")
                append("وقت النوم: ${it.heureSommeil}\n")
            }
            append("\n=== مواعيد الـ Rappels ===\n")
            if (rappels.isEmpty()) append("لا يوجد rappels.\n")
            else rappels.forEach { r ->
                append("• ${r.message} — ${sdf.format(Date(r.time))}\n")
            }
            append("\nالوقت الحالي: $now\n")
            append("\n=== تعليمات ===\n")
            append("- تكلم بالعربية أو الدارجة التونسية\n")
            append("- كن مختصراً ومطمئناً\n")
            append("- إذا سأل عن rappels → أجبه بالمعلومات الحقيقية\n")
            append("- إذا ذكر أعراض خطيرة → اقترح الاتصال بالمشرف\n")
            append("- إذا قال 'عوني' أو 'طحت' → اقترح زر SOS\n")
            append("- لا تعطي تشخيص طبي دقيق\n")
        }
    }
}