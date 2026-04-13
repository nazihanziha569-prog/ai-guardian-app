package com.example.ai_guardian.data.model


data class User(
    val uid: String = "",
    val nom: String = "",
    val email: String = "",
    val age: String = "",
    val role: String = "",
    val supervisorId: String = "",
    val isOnline: Boolean = false,
    val imageUrl: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)