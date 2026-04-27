package com.example.ai_guardian.data.repository

import com.google.firebase.firestore.FirebaseFirestore

class CallRepository {

    private val db = FirebaseFirestore.getInstance()

    fun createCall(
        from: String,
        to: String,
        onSuccess: (String) -> Unit
    ) {

        val call = hashMapOf(
            "from" to from,
            "to" to to,
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("calls")
            .add(call)
            .addOnSuccessListener { docRef ->

                val callId = docRef.id   // 🔥 important

                onSuccess(callId)
            }
    }
}