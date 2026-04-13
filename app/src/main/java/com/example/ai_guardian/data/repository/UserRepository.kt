package com.example.ai_guardian.data.repository

import com.example.ai_guardian.data.datasource.FirebaseDataSource
import com.example.ai_guardian.data.model.User

class UserRepository(private val firebase: FirebaseDataSource) {

    suspend fun register(user: User, password: String): Result<Unit> {
        return firebase.registerUser(user, password)
    }

    suspend fun login(email: String, password: String): Result<User> {
        return firebase.loginUser(email, password)
    }
}