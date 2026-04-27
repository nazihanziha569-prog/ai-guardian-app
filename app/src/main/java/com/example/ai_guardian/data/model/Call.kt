package com.example.ai_guardian.data.model

data class Call(
    val from: String = "",
    val to: String = "",
    val status: String = "pending",
    val timestamp: Long = 0L
)