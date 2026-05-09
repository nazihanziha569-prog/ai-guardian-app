package com.example.ai_guardian.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ai_guardian.service.AlertService

class AlertReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {

        AlertService.userConfirmed = true

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(1002)
    }
}