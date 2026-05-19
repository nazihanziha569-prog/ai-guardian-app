package com.example.ai_guardian.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.ai_guardian.MainActivity
import com.example.ai_guardian.R
import com.example.ai_guardian.receiver.StopAlarmReceiver

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val message = intent?.getStringExtra("message") ?: "Rappel"

        // ✅ 1. Foreground notification مع bouton STOP
        startForeground(1001, buildNotification(message))

        // ✅ 2. شغّل الصوت
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()



        return START_NOT_STICKY
    }

    private fun buildNotification(message: String): android.app.Notification {
        val channelId = "alarm_channel"
        val manager   = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel(channelId, "Alarmes", NotificationManager.IMPORTANCE_HIGH)
                .also { manager.createNotificationChannel(it) }
        }

        // ✅ كي تضغط على الـ notification يفتح AlarmScreen
        val openIntent = Intent(this, MainActivity::class.java)
        openIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        openIntent.putExtra("alarm_message", message)
        val openPending = PendingIntent.getActivity(
            this, 1, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ✅ bouton STOP
        val stopIntent = Intent(this, StopAlarmReceiver::class.java)
        val stopPending = PendingIntent.getBroadcast(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("⏰ Rappel médicament")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPending)  // ✅ يفتح AlarmScreen عند الضغط
            .addAction(R.drawable.logo, "STOP", stopPending)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}