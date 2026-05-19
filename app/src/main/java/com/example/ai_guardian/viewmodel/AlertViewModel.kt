package com.example.ai_guardian.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.example.ai_guardian.data.model.Alert
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AlertViewModel(private val context: android.content.Context? = null) : ViewModel() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    var alerts by mutableStateOf<List<Alert>>(emptyList())
        private set

    fun sendAlert(type: String, message: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("Associations")
            .whereEqualTo("superviseeId", currentUserId)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    onError("Pas de superviseur ❌")
                    return@addOnSuccessListener
                }

                val superviseurId = result.documents.first().getString("superviseurId") ?: ""

                db.collection("Users").document(currentUserId).get()
                    .addOnSuccessListener { userDoc ->
                        val nom = userDoc.getString("nom") ?: "Unknown"

                        val alert = hashMapOf(
                            "superviseeId"   to currentUserId,
                            "superviseeName" to nom,
                            "superviseurId"  to superviseurId,
                            "type"           to type,
                            "message"        to message,
                            "timestamp"      to System.currentTimeMillis()
                        )

                        db.collection("Alerts").add(alert)
                            .addOnSuccessListener {
                                onSuccess()
                                db.collection("Users").document(superviseurId).get()
                                    .addOnSuccessListener { supDoc ->
                                        val token = supDoc.getString("fcmToken")
                                            ?: return@addOnSuccessListener
                                        context?.let { ctx ->
                                            sendFcmV1(
                                                token   = token,
                                                title   = if (type == "danger") "🚨 Danger !" else "⚠️ Besoin d'aide",
                                                body    = "$nom : $message",
                                                type    = type,
                                                context = ctx
                                            )
                                        }
                                    }
                            }
                            .addOnFailureListener { onError(it.message ?: "Erreur") }
                    }
            }
    }

    fun listenAlerts() {
        val currentUserId = auth.currentUser?.uid ?: return
        db.collection("Alerts")
            .whereEqualTo("superviseurId", currentUserId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    alerts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Alert::class.java)?.copy(id = doc.id)
                    }
                }
            }
    }

    fun listenMyAlerts() {
        val currentUserId = auth.currentUser?.uid ?: return
        db.collection("Alerts")
            .whereEqualTo("superviseeId", currentUserId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    alerts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Alert::class.java)?.copy(id = doc.id)
                    }
                }
            }
    }
}

// ✅ خارج الكلاس تماماً
fun sendFcmV1(token: String, title: String, body: String, type: String, context: android.content.Context) {
    Thread {
        try {
            val stream      = context.assets.open("service-account.json")
            val credentials = com.google.auth.oauth2.GoogleCredentials
                .fromStream(stream)
                .createScoped("https://www.googleapis.com/auth/firebase.messaging")
            credentials.refreshIfExpired()
            val accessToken = credentials.accessToken.tokenValue

            val projectId = "ai-guardian-89b08"
            val url  = java.net.URL("https://fcm.googleapis.com/v1/projects/$projectId/messages:send")
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type",  "application/json")
                setRequestProperty("Authorization", "Bearer $accessToken")
                doOutput       = true
                connectTimeout = 10_000
                readTimeout    = 10_000
            }

            val bodyJson = org.json.JSONObject().apply {
                put("message", org.json.JSONObject().apply {
                    put("token", token)
                    put("notification", org.json.JSONObject().apply {
                        put("title", title)
                        put("body",  body)
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

            conn.outputStream.use { it.write(bodyJson.toString().toByteArray()) }
            val responseCode = conn.responseCode
            val response     = if (responseCode == 200)
                conn.inputStream.bufferedReader().readText()
            else
                conn.errorStream.bufferedReader().readText()
            android.util.Log.d("FCM_V1", "Response $responseCode: $response")

        } catch (e: Exception) {
            android.util.Log.e("FCM_V1", "❌ ${e.message}")
        }
    }.start()
}