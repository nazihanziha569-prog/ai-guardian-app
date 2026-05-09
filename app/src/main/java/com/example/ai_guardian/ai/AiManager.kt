package com.example.ai_guardian.ai

import android.content.Context
import android.content.Intent
import com.example.ai_guardian.service.AlertService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.sqrt

class AiManager(
    private val context: Context,
    private val surveilleeName: String = "Utilisateur"
) {

    private val fallEngine       = FallEngine(context.assets)
    private val inactivityEngine = InactivityEngine()
    private val decisionEngine   = AiDecisionEngine()
    private val inputBuilder     = AiInputBuilder()

    private var lastFallAlert     = 0L
    private var lastInactiveAlert = 0L

    init {
        listenToConfig()
    }

    private fun listenToConfig() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("SurveilleConfig")
            .document(uid)
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    inactivityEngine.heureReveil       = doc.getString("heureReveil")                    ?: "07:00"
                    inactivityEngine.heureSommeil      = doc.getString("heureSommeil")                   ?: "22:00"
                    inactivityEngine.thresholdMinutes  = doc.getLong("inactivityThresholdMinutes")?.toInt() ?: 1
                    inactivityEngine.inactivityEnabled = doc.getBoolean("inactivityEnabled")             ?: true

                    android.util.Log.d("AI_DEBUG",
                        "Config → réveil=${inactivityEngine.heureReveil} " +
                                "sommeil=${inactivityEngine.heureSommeil} " +
                                "threshold=${inactivityEngine.thresholdMinutes}min " +
                                "enabled=${inactivityEngine.inactivityEnabled}"
                    )
                }
            }
    }

    fun processSensor(x: Float, y: Float, z: Float) {

        val movement = sqrt(x * x + y * y + z * z)

        // ✅ passe x,y,z — InactivityEngine filtre la gravité lui-même
        inactivityEngine.updateMovement(x, y, z)

        val isFall     = fallEngine.predict(x, y, z)
        val isInactive = inactivityEngine.isInactive()

        val input = inputBuilder.build(
            fall     = isFall,
            inactive = isInactive,
            movement = movement,
            sound    = 0f
        )

        val decision = decisionEngine.decide(input)

        android.util.Log.d("AI_DEBUG",
            "decision=$decision | movement=${"%.2f".format(movement)} " +
                    "| isFall=$isFall | isInactive=$isInactive " +
                    "| sleeping=${inactivityEngine.isSleepingTime()}"
        )

        when (decision) {

            "ALERT" -> {
                val now = System.currentTimeMillis()
                if (now - lastFallAlert > 10_000) {
                    lastFallAlert = now
                    android.util.Log.d("AI_DEBUG", "🚨 FALL ALERT TRIGGERED")
                    launchAlertService("fall")
                }
            }

            "INACTIVE" -> {
                val now = System.currentTimeMillis()
                if (now - lastInactiveAlert > 60_000) {
                    lastInactiveAlert = now
                    android.util.Log.d("AI_DEBUG", "⚠️ INACTIVITY ALERT TRIGGERED")
                    launchAlertService("inactivity")
                }
            }
        }
    }

    private fun launchAlertService(type: String) {
        val intent = Intent(context, AlertService::class.java).apply {
            putExtra("type", type)
            putExtra("surveillee_name", surveilleeName)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}