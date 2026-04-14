package com.example.ai_guardian.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.example.ai_guardian.data.model.Alert
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AlertViewModel : ViewModel() {

    // 🔥 state باش نستعملوه في UI
    var alerts by mutableStateOf<List<Alert>>(emptyList())

    // ✅ إرسال alert (عندك)
    fun sendAlert(
        type: String,
        message: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserId == null) {
            onError("User non connecté")
            return
        }

        db.collection("Users")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { doc ->

                val superviseurId = doc.getString("supervisorId")

                if (superviseurId == null) {
                    onError("Pas de superviseur ❌")
                    return@addOnSuccessListener
                }

                val alert = hashMapOf(
                    "superviseeId" to currentUserId,
                    "superviseurId" to superviseurId,
                    "type" to type,
                    "message" to message,
                    "timestamp" to System.currentTimeMillis()
                )

                db.collection("Alerts")
                    .add(alert)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener {
                        onError(it.message ?: "Erreur")
                    }
            }
    }

    fun listenAlerts() {

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("Alerts")
            .whereEqualTo("superviseurId", currentUserId)
            .addSnapshotListener { snapshot, _ ->

                if (snapshot != null) {
                    alerts = snapshot.documents.mapNotNull {
                        it.toObject(Alert::class.java)
                    }
                }
            }
    }

}