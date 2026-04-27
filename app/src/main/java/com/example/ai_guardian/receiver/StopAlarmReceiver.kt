package com.example.ai_guardian.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StopAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        AlarmReceiver.mediaPlayer?.stop()
        AlarmReceiver.mediaPlayer?.release()
        AlarmReceiver.mediaPlayer = null

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        manager.cancel(1001) // remove notification
    }
}