package com.example.ai_guardian.data.datasource

import com.example.ai_guardian.data.model.Rappel
import com.example.ai_guardian.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseDataSource {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()


    suspend fun registerUser(user: User, password: String): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(user.email, password).await()
            val uid = result.user?.uid ?: ""
            db.collection("Users").document(uid).set(user.copy(uid = uid)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: ""
            val doc = db.collection("Users").document(uid).get().await()
            val user = doc.toObject(User::class.java) ?: User()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // 🔥 ADD Rappel
    suspend fun addRappel(rappel: Rappel) {
        db.collection("Rappels")
            .document(rappel.id)
            .set(rappel)
            .await()
    }

    // 🔥 LISTEN Rappels
    fun listenRappels(
        userId: String,
        onChange: (List<Rappel>) -> Unit
    ) {
        db.collection("Rappels")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->

                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(Rappel::class.java)?.copy(id = it.id)
                } ?: emptyList()

                onChange(list)
            }
    }
}