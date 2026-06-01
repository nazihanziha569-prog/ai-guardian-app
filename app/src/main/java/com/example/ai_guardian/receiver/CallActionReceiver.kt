package com.example.ai_guardian.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ai_guardian.GlobalCallState
import com.example.ai_guardian.MainActivity
import com.google.firebase.firestore.FirebaseFirestore

class CallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val callId = intent.getStringExtra("call_id") ?: return
        val from   = intent.getStringExtra("from")    ?: return

        when (intent.action) {
            "ACTION_ACCEPT" -> {
                // إلغاء الـ notification
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as android.app.NotificationManager
                nm.cancel(callId.hashCode())

                // فتح MainActivity → IncomingCallScreen
                GlobalCallState.pendingFrom   = from
                GlobalCallState.pendingCallId = callId

                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(mainIntent)
            }

            "ACTION_REJECT" -> {
                // إلغاء الـ notification
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as android.app.NotificationManager
                nm.cancel(callId.hashCode())

                // رفض الاتصال في Firestore
                FirebaseFirestore.getInstance()
                    .collection("calls").document(callId)
                    .update("status", "rejected")
            }
        }
    }
}