package com.example.ai_guardian.repository

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AiRepository {

    private val db = FirebaseFirestore.getInstance()

    // ─────────────────────────────
    // GET CURRENT USER ID
    // ─────────────────────────────
    private fun getUid(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    // ─────────────────────────────
    // FALL ALERT
    // ─────────────────────────────
    fun sendFallAlert(
        message: String = "⚠️ Chute détectée automatiquement",
        onComplete: (Boolean) -> Unit = {}
    ) {

        val uid = getUid() ?: return

        db.collection("Associations")
            .whereEqualTo("superviseeId", uid)
            .get()
            .addOnSuccessListener { result ->

                val superviseurId =
                    result.documents.firstOrNull()
                        ?.getString("superviseurId") ?: ""

                val alert = hashMapOf(
                    "superviseeId" to uid,
                    "superviseeName" to "Utilisateur",
                    "superviseurId" to superviseurId,
                    "type" to "danger",
                    "message" to message,
                    "timestamp" to System.currentTimeMillis(),
                    "status" to "unconfirmed"
                )

                db.collection("Alerts")
                    .add(alert)
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
    }

    // ─────────────────────────────
    // INACTIVITY ALERT
    // ─────────────────────────────
    fun sendInactivityAlert(
        message: String = "⚠️ Inactivité prolongée détectée",
        onComplete: (Boolean) -> Unit = {}
    ) {

        val uid = getUid() ?: return

        db.collection("Associations")
            .whereEqualTo("superviseeId", uid)
            .get()
            .addOnSuccessListener { result ->

                val superviseurId =
                    result.documents.firstOrNull()
                        ?.getString("superviseurId") ?: ""

                val alert = hashMapOf(
                    "superviseeId" to uid,
                    "superviseeName" to "Utilisateur",
                    "superviseurId" to superviseurId,
                    "type" to "warning",
                    "message" to message,
                    "timestamp" to System.currentTimeMillis(),
                    "status" to "unconfirmed"
                )

                db.collection("Alerts")
                    .add(alert)
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
    }

    // ─────────────────────────────
    // GET SUPERVISEUR PHONE (CALL)
    // ─────────────────────────────
    fun getSuperviseurPhone(
        onResult: (String?) -> Unit
    ) {

        val uid = getUid() ?: return onResult(null)

        db.collection("Associations")
            .whereEqualTo("superviseeId", uid)
            .get()
            .addOnSuccessListener { result ->

                val superviseurId =
                    result.documents.firstOrNull()
                        ?.getString("superviseurId")

                if (superviseurId == null) {
                    onResult(null)
                    return@addOnSuccessListener
                }

                db.collection("Users")
                    .document(superviseurId)
                    .get()
                    .addOnSuccessListener { doc ->

                        val phone = doc.getString("phone")
                        onResult(phone)
                    }
                    .addOnFailureListener {
                        onResult(null)
                    }
            }
    }
    fun sendFallNotification(context: Context) {

        val intent =
            android.content.Intent(
                context,
                com.example.ai_guardian.service.AlertService::class.java
            )

        intent.putExtra("type", "fall")

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            context.startForegroundService(intent)

        } else {

            context.startService(intent)
        }
    }
}