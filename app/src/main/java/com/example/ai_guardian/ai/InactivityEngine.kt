package com.example.ai_guardian.ai

import java.util.Calendar
import kotlin.math.abs
import kotlin.math.sqrt

class InactivityEngine {

    private var lastMovementTime = System.currentTimeMillis()

    var heureReveil       = "07:00"
    var heureSommeil      = "22:00"
    var thresholdMinutes  = 1
    var inactivityEnabled = true

    // ─── Filtre gravité (low-pass filter) ────────────────────────────────
    private var gravX = 0f
    private var gravY = 0f
    private var gravZ = 9.8f
    private val alpha = 0.9f   // facteur lissage — plus proche de 1 = plus lent

    fun updateMovement(x: Float, y: Float, z: Float) {
        // sépare gravité et accélération linéaire
        gravX = alpha * gravX + (1 - alpha) * x
        gravY = alpha * gravY + (1 - alpha) * y
        gravZ = alpha * gravZ + (1 - alpha) * z

        val linearX = x - gravX
        val linearY = y - gravY
        val linearZ = z - gravZ

        // magnitude de l'accélération linéaire (sans gravité)
        val linearMag = sqrt(linearX * linearX + linearY * linearY + linearZ * linearZ)

        // ✅ seuil 0.8 m/s² — mouvement humain réel (pas vibration/bruit)
        if (linearMag > 0.8f) {
            lastMovementTime = System.currentTimeMillis()
        }
    }

    fun isInactive(): Boolean {
        if (!inactivityEnabled) return false
        if (isSleepingTime()) return false
        val now = System.currentTimeMillis()
        return now - lastMovementTime > thresholdMinutes * 60 * 1000L
    }

    fun isSleepingTime(): Boolean {
        val cal     = Calendar.getInstance()
        val current = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val wake    = parseTime(heureReveil)
        val sleep   = parseTime(heureSommeil)
        return if (sleep > wake) {
            current >= sleep || current < wake
        } else {
            current in sleep until wake
        }
    }

    private fun parseTime(time: String): Int {
        val parts = time.split(":")
        return (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
    }
}