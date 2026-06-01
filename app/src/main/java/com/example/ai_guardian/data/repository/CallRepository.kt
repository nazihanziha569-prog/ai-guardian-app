package com.example.ai_guardian.data.repository

import com.example.ai_guardian.data.model.Call
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class CallRepository {

    private val db = FirebaseFirestore.getInstance()

    fun createCall(
        from: String,
        to: String,
        callType: String = "video",
        onSuccess: (String) -> Unit
    ) {

        val call = hashMapOf(
            "from" to from,
            "to" to to,
            "status" to "pending",
            "callType" to callType,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("calls")
            .add(call)
            .addOnSuccessListener { doc ->
                onSuccess(doc.id)
                autoEndCall(doc.id, System.currentTimeMillis())

                // جيب token المستجيب وابعث FCM مباشرة
                db.collection("Users").document(to).get()
                    .addOnSuccessListener { toDoc ->
                        val token = toDoc.getString("fcmToken") ?: return@addOnSuccessListener
                        db.collection("Users").document(from).get()
                            .addOnSuccessListener { fromDoc ->
                                val callerName = fromDoc.getString("nom") ?: "Appel entrant"
                                sendFCMDirect(token, doc.id, from, callerName, callType)
                            }
                    }
            }
    }

    private fun sendFCMDirect(
        token: String, callId: String, from: String,
        callerName: String, callType: String
    ) {
        val json = org.json.JSONObject().apply {
            put("message", org.json.JSONObject().apply {
                put("token", token)
                put("data", org.json.JSONObject().apply {
                    put("type", "incoming_call")
                    put("callId", callId)
                    put("from", from)
                    put("callerName", callerName)
                    put("callType", callType)
                })
                put("android", org.json.JSONObject().apply {
                    put("priority", "high")
                })
            })
        }

        Thread {
            try {
                val url = java.net.URL("https://fcm.googleapis.com/v1/projects/ai-guardian-89b08/messages:send")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer credentials.accessToken.tokenValue")
                conn.doOutput = true
                conn.outputStream.write(json.toString().toByteArray())
                conn.responseCode
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun listenCallsByUid(uid: String, onChange: (List<Call>) -> Unit): ListenerRegistration {
        return db.collection("calls")
            .addSnapshotListener { snapshot, _ ->
                val calls = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Call::class.java)?.copy(id = doc.id)
                }?.filter { it.from == uid || it.to == uid } ?: emptyList()

                onChange(calls)
            }
    }

    fun autoEndCall(callId: String, timestamp: Long) {
        val delay = 60_000L - (System.currentTimeMillis() - timestamp)

        if (delay > 0) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                db.collection("calls").document(callId).get()
                    .addOnSuccessListener { doc ->
                        if (doc.getString("status") == "pending") {
                            db.collection("calls").document(callId)
                                .update("status", "missed")
                        }
                    }
            }, delay)
        }
    }
    fun resolveUserName(uid: String, onResult: (String) -> Unit) {
        FirebaseFirestore.getInstance()
            .collection("Users")
            .document(uid)
            .get()
            .addOnSuccessListener {
                onResult(it.getString("nom") ?: "Unknown")
            }
    }
    fun listenCallsBetween(
        uid1: String,
        uid2: String,
        onChange: (List<Call>) -> Unit
    ): ListenerRegistration {
        return db.collection("calls")
            .addSnapshotListener { snapshot, _ ->
                val calls = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Call::class.java)?.copy(id = doc.id)
                }?.filter {
                    (it.from == uid1 && it.to == uid2) ||
                            (it.from == uid2 && it.to == uid1)
                } ?: emptyList()

                onChange(calls)
            }
    }
}