package com.example.ai_guardian.data.model


@com.google.firebase.firestore.IgnoreExtraProperties

data class User(
    val uid: String = "",
    val nom: String = "",
    val email: String = "",
    val age: Any? = null,
    val role: String = "",
    val isOnline: Boolean = false,
    val imageUrl: String = "",
    val phone: String = "",
    val blocked: Boolean = false,
    val supervisorId: String = "",
    val fcmToken: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)