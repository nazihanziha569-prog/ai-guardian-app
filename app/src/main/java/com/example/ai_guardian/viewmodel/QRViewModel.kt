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

    fun onQrScanned(data: String) {
        qrResult = data
    }

    fun createAssociation(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val superviseeId = FirebaseAuth.getInstance().currentUser?.uid
        val superviseurId = qrResult

        if (superviseeId == null || superviseurId.isEmpty()) {
            onError("QR invalide ❌")
            return
        }

        val data = hashMapOf(
            "superviseurId" to superviseurId,
            "superviseeId" to superviseeId,
            "createdAt" to System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection("Associations")
            .add(data)
            .addOnSuccessListener {
                isLinked = true
                onSuccess()
            }
            .addOnFailureListener {
                onError(it.message ?: "Erreur Firebase")
            }
    }
}