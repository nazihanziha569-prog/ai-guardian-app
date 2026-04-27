package com.example.ai_guardian.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.ai_guardian.R

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        var mediaPlayer: android.media.MediaPlayer? = null
    }

    override fun onReceive(context: Context, intent: Intent) {

        val message = intent.getStringExtra("message") ?: "Alarm!"

        // 🔥 START SOUND LOOP
        if (mediaPlayer == null) {
            mediaPlayer = android.media.MediaPlayer.create(context, R.raw.alarm_sound)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }

        // 🔴 STOP ACTION
        val stopIntent = Intent(context, StopAlarmReceiver::class.java)
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "alarm_channel")
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("🚨 AI Guardian Alarm")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .addAction(0, "STOP", stopPendingIntent) // 🔥 BUTTON
            .build()

        NotificationManagerCompat.from(context)
            .notify(1001, notification)
    }
}