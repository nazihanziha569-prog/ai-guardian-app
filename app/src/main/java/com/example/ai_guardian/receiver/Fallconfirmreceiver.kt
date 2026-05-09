package com.example.ai_guardian.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ai_guardian.service.AiService


/**
 * Reçoit le broadcast quand l'utilisateur appuie sur "Je vais bien"
 * dans la notification de confirmation de chute.
 */
class FallConfirmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val serviceIntent = Intent(context, AiService::class.java).apply {
            action = ACTION_USER_OK
        }

        context.startService(serviceIntent)
    }

    companion object {
        const val ACTION_USER_OK =
            "com.example.ai_guardian.USER_OK"
    }
}