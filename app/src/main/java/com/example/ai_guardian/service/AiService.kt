package com.example.ai_guardian.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.app.NotificationCompat
import com.example.ai_guardian.MainActivity
import com.example.ai_guardian.ai.AiManager
import com.example.ai_guardian.receiver.ServiceRestartReceiver
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AiService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // ✅ initialisé immédiatement — pas besoin d'attendre Firestore
    private lateinit var aiManager: AiManager

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, createNotification())

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid != null) {
            FirebaseFirestore.getInstance()
                .collection("Users")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    val nom = doc.getString("nom") ?: "Utilisateur"
                    // ✅ crée AiManager avec le vrai nom
                    aiManager = AiManager(this, surveilleeName = nom)
                }
                .addOnFailureListener {
                    // ✅ Firestore échoue → démarre quand même avec nom par défaut
                    if (!::aiManager.isInitialized) {
                        aiManager = AiManager(this)
                    }
                }

            // ✅ démarre immédiatement sans attendre Firestore
            //    le nom sera mis à jour quand Firestore répond
            if (!::aiManager.isInitialized) {
                aiManager = AiManager(this)
            }

        } else {
            aiManager = AiManager(this)
        }

        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!::aiManager.isInitialized) return
        aiManager.processSensor(
            event.values[0],
            event.values[1],
            event.values[2]
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val restartIntent = Intent(applicationContext, AiService::class.java).also {
            it.setPackage(packageName)
        }
        val pendingIntent = PendingIntent.getService(
            this, 1, restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        (getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager).set(
            android.app.AlarmManager.ELAPSED_REALTIME,
            android.os.SystemClock.elapsedRealtime() + 1000,
            pendingIntent
        )
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        sendBroadcast(Intent(this, ServiceRestartReceiver::class.java))
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun createNotification(): Notification {
        val channelId = "ai_guardian_channel"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "AI Guardian Service", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Surveillance active en arrière-plan"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("AI Guardian actif")
            .setContentText("Surveillance en cours...")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val NOTIF_ID = 1
    }
}