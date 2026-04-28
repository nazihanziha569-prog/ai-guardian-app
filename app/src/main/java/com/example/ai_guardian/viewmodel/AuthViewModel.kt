package com.example.ai_guardian.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai_guardian.data.model.User
import com.example.ai_guardian.data.repository.UserRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repo: UserRepository
) : ViewModel() {

    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var name by mutableStateOf("")
    var passwordVisible by mutableStateOf(false)
    var role by mutableStateOf("")
    var age by mutableStateOf("")
    var phone by mutableStateOf("")


    var imageUri by mutableStateOf<android.net.Uri?>(null)

    // 🔥 لازم هنا (داخل الكلاس)
    var surveilles by mutableStateOf<List<User>>(emptyList())

    fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible
    }

    fun login(onSuccess: (String) -> Unit, onError: (String) -> Unit) {

        if (email.isEmpty() || password.isEmpty()) {
            onError("Remplir tous les champs ❌")
            return
        }

        viewModelScope.launch {
            val result = repo.login(email, password)

            result.onSuccess {

                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@onSuccess

                // 🔥 خزن fcmToken
                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->

                    FirebaseFirestore.getInstance()
                        .collection("Users")
                        .document(uid)
                        .update("fcmToken", token)
                }

                FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { doc ->

                        val role = doc.getString("role") ?: ""

                        onSuccess(role)
                    }
            }.onFailure {
                onError(it.message ?: "Erreur")
            }
        }
    }

    fun register(onSuccess: () -> Unit, onError: (String) -> Unit) {

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            onError("Remplir tous les champs ❌")
            return
        }

        if (password != confirmPassword) {
            onError("Les mots de passe ne correspondent pas ❌")
            return
        }

        viewModelScope.launch {

            val user = User(
                nom = name,
                email = email,
                role = role,
                isOnline = false,
                phone = phone
            )

            val result = repo.register(user, password)

            result.onSuccess {
                onSuccess()
            }.onFailure {
                onError(it.message ?: "Erreur")
            }
        }
    }

    // 🔥 نفس الشي: لازم داخل الكلاس
    fun loadSurveilles() {

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        // 🔥 1. نجيب associations متاع supervisor
        db.collection("Associations")
            .whereEqualTo("superviseurId", currentUserId)
            .get()
            .addOnSuccessListener { result ->

                val surveilleIds = result.mapNotNull {
                    it.getString("superviseeId")
                }

                if (surveilleIds.isEmpty()) {
                    surveilles = emptyList()
                    return@addOnSuccessListener
                }

                // 🔥 2. نجيب users بالـ ids
                db.collection("Users")
                    .whereIn("uid", surveilleIds)
                    .get()
                    .addOnSuccessListener { usersResult ->

                        val list = usersResult.mapNotNull {
                            it.toObject(User::class.java)
                        }

                        surveilles = list
                    }
            }
    }

    fun updateName(name: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("Users")
            .document(uid)
            .update("nom", name)
    }
    fun updateEmail(
        newEmail: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val credential = EmailAuthProvider.getCredential(user.email!!, password)

        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->

                if (!reauthTask.isSuccessful) {
                    onError("Wrong password or session expired")
                    return@addOnCompleteListener
                }

                user.updateEmail(newEmail)
                    .addOnCompleteListener { updateTask ->

                        if (!updateTask.isSuccessful) {
                            onError(updateTask.exception?.message ?: "Email update failed")
                            return@addOnCompleteListener
                        }

                        // 🔥 IMPORTANT: update Firestore
                        FirebaseFirestore.getInstance()
                            .collection("Users")
                            .document(user.uid)
                            .update("email", newEmail)

                        // 🔥 logout after success
                        FirebaseAuth.getInstance().signOut()

                        onSuccess()
                    }
            }
    }
    fun reAuth(oldPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) {

        val user = FirebaseAuth.getInstance().currentUser ?: return

        val email = user.email ?: return

        val credential = EmailAuthProvider.getCredential(email, oldPassword)

        user.reauthenticate(credential)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "") }
    }
    fun changePassword(newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) {

        FirebaseAuth.getInstance().currentUser
            ?.updatePassword(newPassword)
            ?.addOnSuccessListener { onSuccess() }
            ?.addOnFailureListener { onError(it.message ?: "") }
    }
    fun resetPassword(
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        FirebaseAuth.getInstance()
            .sendPasswordResetEmail(email)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError(it.message ?: "Erreur")
            }
    }
    var alertsCount by mutableStateOf<Map<String, Int>>(emptyMap())

    fun loadAlertsCount() {

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("Alerts")
            .whereEqualTo("superviseurId", currentUserId)
            .addSnapshotListener { snapshot, _ ->

                if (snapshot != null) {

                    val counts = mutableMapOf<String, Int>()

                    for (doc in snapshot.documents) {

                        val userId = doc.getString("superviseeId") ?: continue

                        counts[userId] = counts.getOrDefault(userId, 0) + 1
                    }

                    alertsCount = counts
                }
            }
    }
}