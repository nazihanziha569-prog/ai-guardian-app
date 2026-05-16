package com.example.ai_guardian.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ai_guardian.service.AiService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ServiceRestartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // ✅ تحقق من الـ role قبل إعادة التشغيل
        FirebaseFirestore.getInstance()
            .collection("Users").document(uid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: return@addOnSuccessListener
                if (role == "surveille") {
                    val serviceIntent = Intent(context, AiService::class.java)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            }
    }
}