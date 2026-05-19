package com.example.ai_guardian.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.ai_guardian.receiver.AlarmReceiver
import java.util.Calendar

class AlarmRepository(private val context: Context) {

    fun scheduleAlarm(time: Long, message: String) {

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                android.util.Log.e("ALARM", "❌ No exact alarm permission!")
                return
            }
        }

        val triggerTime = Calendar.getInstance().apply {
            timeInMillis = time
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("message", message)
            putExtra("original_time", time)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (time % Int.MAX_VALUE).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
            pendingIntent
        )

        android.util.Log.d("ALARM", "✅ Scheduled for: ${java.util.Date(triggerTime)}")
    }

    fun cancelAlarm(time: Long, message: String) {

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("message", message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (time % Int.MAX_VALUE).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }
}