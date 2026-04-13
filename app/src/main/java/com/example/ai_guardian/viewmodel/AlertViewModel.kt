package com.example.ai_guardian.viewmodel

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.lifecycle.ViewModel

class AlertViewModel : ViewModel() {

    fun sendAlert(
        type: String, // normal / danger
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            onError("User non connecté ❌")
            return
        }

        val alert = hashMapOf(
            "userId" to userId,
            "type" to type,
            "timestamp" to System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection("Alerts")
            .add(alert)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                onError(it.message ?: "Erreur")
            }
    }
}