package com.example.ai_guardian.data.model
@com.google.firebase.firestore.IgnoreExtraProperties

data class Alert(
    val id: String = "",
    val superviseeName: String = "",
    val superviseeId: String = "",
    val superviseurId: String = "",
    val type: String = "",
    val message: String = "",
    val timestamp: Long = 0
)
