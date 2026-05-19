package com.example.ai_guardian.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ai_guardian.service.AlarmService

class StopAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.stopService(Intent(context, AlarmService::class.java))
    }
}