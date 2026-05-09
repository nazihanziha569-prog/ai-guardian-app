package com.example.ai_guardian.ai

class AiInputBuilder {

    fun build(
        fall     : Boolean,
        inactive : Boolean,
        movement : Float,
        sound    : Float
    ): FloatArray {
        return floatArrayOf(
            if (fall) 1f else 0f,       // index 0 — lu par AiDecisionEngine
            if (inactive) 1f else 0f,   // index 1
            movement / 10f,             // index 2 — normalisé
            sound    / 100f             // index 3 — non utilisé pour l'instant
        )
    }
}