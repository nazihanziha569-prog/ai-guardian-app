package com.example.ai_guardian.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class QRViewModel : ViewModel() {

    var qrResult by mutableStateOf("")
    var isLinked by mutableStateOf(false)

    // ✅ وقت يعمل scan
    fun onQrScanned(data: String) {
        qrResult = data
    }

    // 🔥 الربط مع superviseur
    fun linkToSupervisor(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserId == null || qrResult.isEmpty()) {
            onError("QR invalide ❌")
            return
        }

        val db = FirebaseFirestore.getInstance()

        db.collection("Users")
            .document(currentUserId)
            .update("supervisorId", qrResult)
            .addOnSuccessListener {
                isLinked = true
                onSuccess()
            }
            .addOnFailureListener {
                onError(it.message ?: "Erreur Firebase")
            }
    }
}