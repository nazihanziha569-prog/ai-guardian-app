package com.example.ai_guardian.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RatingRepository {

    private val db = FirebaseFirestore.getInstance()

    fun saveRating(rating: Int) {

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val data = hashMapOf(
            "rating" to rating,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("Ratings")
            .document(uid)
            .set(data)
    }
}