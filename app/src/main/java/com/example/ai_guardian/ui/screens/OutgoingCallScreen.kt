package com.example.ai_guardian.ui.screens

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ai_guardian.viewmodel.CallViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import livekit.org.webrtc.EglBase

@Composable
fun OutgoingCallScreen(
    navController: NavController,
    callId       : String,
    toUserId     : String,
    callType     : String = "audio",
    callVM       : CallViewModel,
    eglBase      : EglBase
) {
    val db           = FirebaseFirestore.getInstance()
    val context      = LocalContext.current
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    var calleeName by remember { mutableStateOf("…") }
    var navigated  by remember { mutableStateOf(false) }
    var timeLeft   by remember { mutableStateOf(60) }

    // ✅ 1. اقرأ اسم المستجيب
    LaunchedEffect(toUserId) {
        db.collection("Users").document(toUserId).get().addOnSuccessListener { doc ->
            calleeName = doc.getString("nom") ?: toUserId
        }
    }

    // ✅ 2. شغّل WebRTC فوراً — اعمل offer
    LaunchedEffect(callId) {
        callVM.startCall(
            context       = context,
            callId        = callId,
            eglBase       = eglBase,
            onLocalVideo  = {},
            onRemoteVideo = {},
            onOfferReady  = {}
        )
    }

    // ✅ 3. اسمع الـ status
    LaunchedEffect(callId) {
        db.collection("calls").document(callId).addSnapshotListener { snap, _ ->
            val status = snap?.getString("status") ?: return@addSnapshotListener
            when (status) {
                "accepted" -> if (!navigated) {
                    navigated = true
                    val encoded = java.net.URLEncoder.encode(calleeName, "UTF-8")
                    // ✅ نفس الـ screen للاثنين — CallScreen
                    navController.navigate("call_screen/$callId/caller/$encoded/$callType") {
                        popUpTo("outgoing_call/$callId/$toUserId/$callType") { inclusive = true }
                    }
                }
                "rejected", "missed" -> {
                    callVM.endCall(callId)
                    safeNavigate(navController)
                }
                "ended" -> {
                    callVM.endCall(callId)
                    safeNavigate(navController)
                }
            }
        }
    }

    // ✅ 4. Timeout 60s
    LaunchedEffect(callId) {
        repeat(60) {
            if (navigated) return@LaunchedEffect
            delay(1000L)
            timeLeft--
        }
        if (!navigated) {
            db.collection("calls").document(callId).update("status", "missed")
            callVM.endCall(callId)
            safeNavigate(navController)
        }
    }

    DisposableEffect(Unit) {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        onDispose { audioManager.mode = AudioManager.MODE_NORMAL }
    }

    val pulse = rememberInfiniteTransition(label = "p").animateFloat(
        1f, 1.1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "ps"
    )

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A1035), Color(0xFF0F0F14)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(32.dp)
        ) {
            Spacer(Modifier.height(80.dp))
            Text(if (callType == "video") "📹 Appel vidéo" else "📞 Appel audio",
                fontSize = 14.sp, color = Color.White.copy(0.4f))
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier.size(110.dp).scale(pulse.value)
                    .clip(CircleShape).background(Color(0xFF1976D2)),
                contentAlignment = Alignment.Center
            ) {
                Text(calleeName.take(1).uppercase(), fontSize = 42.sp,
                    fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(20.dp))
            Text(calleeName, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text("Appel en cours…", fontSize = 16.sp, color = Color.White.copy(0.6f))
            Text("⏱ ${timeLeft}s", fontSize = 13.sp,
                color = if (timeLeft <= 10) Color(0xFFFF3B30) else Color.White.copy(0.3f))
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape).background(Color(0xFFFF3B30)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = {
                        db.collection("calls").document(callId).update("status", "rejected")
                        callVM.endCall(callId)
                        safeNavigate(navController)
                    }) {
                        Icon(Icons.Default.CallEnd, tint = Color.White,
                            contentDescription = null, modifier = Modifier.size(32.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Annuler", color = Color.White.copy(0.7f), fontSize = 13.sp)
            }
            Spacer(Modifier.height(70.dp))
        }
    }


}
private fun safeNavigate(navController: NavController) {
    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
    com.google.firebase.firestore.FirebaseFirestore.getInstance()
        .collection("Users").document(uid).get()
        .addOnSuccessListener { doc ->
            val dest = when (doc.getString("role") ?: "") {
                "superviseur" -> "dashboard"
                "surveille"   -> "dashboard_surveille"
                else          -> "welcome"
            }
            navController.navigate(dest) { popUpTo(0) { inclusive = true } }
        }
}