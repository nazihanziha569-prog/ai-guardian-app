package com.example.ai_guardian.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.ai_guardian.MainActivity
import com.example.ai_guardian.R
import com.example.ai_guardian.service.AlarmService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val message      = intent.getStringExtra("message") ?: "Rappel"
        val originalTime = intent.getLongExtra("original_time", 0L)

        android.util.Log.d("ALARM_TEST", "✅ AlarmReceiver triggered! msg=$message")

        // ✅ شغّل AlarmService (foreground) — يشتغل حتى على Xiaomi
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("message", message)
            putExtra("original_time", originalTime)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // ✅ إعادة الجدولة ليوم الجاي
        if (originalTime > 0L) scheduleNextDay(context, originalTime, message)
    }

    private fun scheduleNextDay(context: Context, originalTime: Long, message: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) return
        }

        val nextTime = originalTime + AlarmManager.INTERVAL_DAY

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("message", message)
            putExtra("original_time", originalTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (originalTime % Int.MAX_VALUE).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(nextTime, pendingIntent),
            pendingIntent
        )
    }
}