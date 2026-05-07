package com.example.ai_guardian.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ai_guardian.service.FallDetectionService

/**
 * Reçoit le broadcast quand l'utilisateur appuie sur "Je vais bien"
 * dans la notification de confirmation de chute.
 */
class FallConfirmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == FallDetectionService.ACTION_USER_OK) {
            FallDetectionService.userConfirmedOk = true
        }
    }
}