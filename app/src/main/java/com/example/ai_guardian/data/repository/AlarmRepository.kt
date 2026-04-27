package com.example.ai_guardian.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.ai_guardian.receiver.AlarmReceiver

class AlarmRepository(private val context: Context) {

    fun scheduleAlarm(time: Long, message: String) {

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 🔥 Android 12+ permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                return // أو تنجم توري user message
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("message", message)
        }

        val requestCode = (time % Int.MAX_VALUE).toInt()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            time,
            pendingIntent
        )
    }
}