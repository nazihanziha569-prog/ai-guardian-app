package com.example.ai_guardian.data.model

data class ChatMessage(
    val content  : String,
    val isUser   : Boolean,
    val timestamp: Long = System.currentTimeMillis()
)