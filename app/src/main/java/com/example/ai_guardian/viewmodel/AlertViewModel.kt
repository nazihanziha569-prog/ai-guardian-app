package com.example.ai_guardian.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.example.ai_guardian.data.model.Alert
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AlertViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    var alerts by mutableStateOf<List<Alert>>(emptyList())
        private set

    fun sendAlert(
        type: String,
        message: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid

        if (currentUserId == null) {
            onError("User non connecté")
            return
        }

        db.collection("Associations")
            .whereEqualTo("superviseeId", currentUserId)
            .get()
            .addOnSuccessListener { result ->

                if (result.isEmpty) {
                    onError("Pas de superviseur ❌")
                    return@addOnSuccessListener
                }

                val superviseurId = result.documents.first()
                    .getString("superviseurId") ?: ""

                db.collection("Users")
                    .document(currentUserId)
                    .get()
                    .addOnSuccessListener { userDoc ->

                        val nom = userDoc.getString("nom") ?: "Unknown"

                        val alert = hashMapOf(
                            "superviseeId" to currentUserId,
                            "superviseeName" to nom,
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