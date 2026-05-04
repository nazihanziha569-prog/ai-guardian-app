package com.example.ai_guardian.data.model
@com.google.firebase.firestore.IgnoreExtraProperties
data class Call(
    val id        : String = "",
    val from      : String = "",
    val to        : String = "",
    val status    : String = "pending",
    val timestamp : Long   = 0L,
    val roomName  : String = "",
    val callType  : String = "video"  // ✅ "video" ou "audio"
)

