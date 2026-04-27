package com.example.ai_guardian.data.repository

import com.example.ai_guardian.data.model.User
import com.google.firebase.firestore.FirebaseFirestore

class AdminRepository {

    private val db = FirebaseFirestore.getInstance()

    fun observeUsers(onResult: (List<User>) -> Unit) {

        db.collection("Users")
            .addSnapshotListener { snap, error ->

                if (error != null) {
                    println("🔥 ERROR: ${error.message}")
                    return@addSnapshotListener
                }

                val users = snap?.documents?.mapNotNull {
                    try {
                        it.toObject(User::class.java)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                onResult(users)
            }
    }

    fun deleteUser(uid: String) {
        db.collection("Users").document(uid).delete()
    }

    fun toggleBlock(user: User) {
        db.collection("Users").document(user.uid)
            .update("blocked", !user.blocked)
    }
}