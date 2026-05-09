package com.example.ai_guardian.ai

class AiDecisionEngine {

    fun decide(input: FloatArray): String {

        val fall     = input[0]   // 1f = chute, 0f = normal
        val inactive = input[1]   // 1f = inactif, 0f = actif
        val movement = input[2]   // movement / 10f

        return when {
            fall     >= 1f   -> "ALERT"      // FallEngine a détecté une chute
            inactive >= 1f   -> "INACTIVE"   // InactivityEngine timeout dépassé
            movement >  1.5f -> "SUSPICIOUS" // mouvement anormal mais pas chute
            else             -> "NORMAL"
        }
    }
}