package com.example.ai_guardian.data.model


data class User(
    val uid: String = "",
    val nom: String = "",
    val email: String = "",
    val age: Any? = null,
    val role: String = "",
    val isOnline: Boolean = false,
    var lastSeen: Long = 0L,
    val imageUrl: String = "",
    val phone: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0
)