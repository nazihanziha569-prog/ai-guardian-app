package com.example.ai_guardian.data.model

data class Alert(
    val id: String = "",
    val superviseeId: String = "",
    val superviseurId: String = "",
    val type: String = "",
    val message: String = "",
    val timestamp: Long = 0
)
