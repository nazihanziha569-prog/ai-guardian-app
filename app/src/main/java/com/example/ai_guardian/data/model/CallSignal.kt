package com.example.ai_guardian.data.model

data class CallSignal(
    val type      : String = "", // "offer" / "answer" / "candidate"
    val sdp       : String = "",
    val candidate : String = "",
    val sdpMid    : String = "",
    val sdpMLineIndex : Int = 0
)