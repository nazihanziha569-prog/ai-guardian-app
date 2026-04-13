package com.example.ai_guardian.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai_guardian.data.model.User
import com.example.ai_guardian.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
                isOnline = false
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

        FirebaseFirestore.getInstance()
            .collection("Users")
            .whereEqualTo("supervisorId", currentUserId)
            .get()
            .addOnSuccessListener { result ->

                val list = result.mapNotNull { it.toObject(User::class.java) }

                surveilles = list
            }
    }
}