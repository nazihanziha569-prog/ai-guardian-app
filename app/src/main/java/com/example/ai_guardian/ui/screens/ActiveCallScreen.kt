package com.example.ai_guardian.ui.screens

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import livekit.org.webrtc.EglBase

@Composable
fun ActiveCallScreen(
    navController   : NavController,
    callId          : String,
    participantName : String,
    isOutgoing      : Boolean,
    callType        : String = "audio",
    callVM          : CallViewModel,
    eglBase         : EglBase
) {
    val context      = LocalContext.current
    val db           = FirebaseFirestore.getInstance()
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    var isAccepting  by remember { mutableStateOf(false) }

    // ✅ اسمع إذا المتصل ألغى
    LaunchedEffect(callId) {
        db.collection("calls").document(callId).addSnapshotListener { snap, _ ->
            val status = snap?.getString("status") ?: return@addSnapshotListener
            if (status == "ended" || status == "rejected" || status == "missed") {
                callVM.endCall(callId)
                safeNavigate(navController)
            }
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
                Text(participantName.take(1).uppercase(), fontSize = 42.sp,
                    fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(20.dp))
            Text(participantName, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text("Appel entrant…", fontSize = 16.sp, color = Color.White.copy(0.6f))
            Spacer(Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(60.dp)) {
                // ❌ Rejeter
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(70.dp).clip(CircleShape)
                        .background(Color(0xFFFF3B30)), contentAlignment = Alignment.Center) {
                        IconButton(onClick = {
                            db.collection("calls").document(callId).update("status", "rejected")
                            safeNavigate(navController)
                        }) {
                            Icon(Icons.Default.CallEnd, tint = Color.White,
                                contentDescription = null, modifier = Modifier.size(30.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Rejeter", color = Color.White.copy(0.7f), fontSize = 13.sp)
                }

                // ✅ Accepter
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(70.dp).clip(CircleShape)
                        .background(if (isAccepting) Color.Gray else Color(0xFF34C759)),
                        contentAlignment = Alignment.Center) {
                        if (isAccepting) {
                            CircularProgressIndicator(color = Color.White,
                                modifier = Modifier.size(30.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = {
                                isAccepting = true
                                // ✅ اقرأ الـ offer وعمل answer
                                db.collection("calls").document(callId).get()
                                    .addOnSuccessListener { doc ->
                                        val offer = doc.getString("offer") ?: ""
                                        if (offer.isBlank()) {
                                            // انتظر الـ offer
                                            db.collection("calls").document(callId)
                                                .addSnapshotListener { snap, _ ->
                                                    val o = snap?.getString("offer") ?: return@addSnapshotListener
                                                    if (o.isNotBlank()) acceptCall(context, callId, o, callType, participantName, callVM, eglBase, navController)
                                                }
                                        } else {
                                            acceptCall(context, callId, offer, callType, participantName, callVM, eglBase, navController)
                                        }
                                    }
                            }) {
                                Icon(Icons.Default.Call, tint = Color.White,
                                    contentDescription = null, modifier = Modifier.size(30.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Accepter", color = Color.White.copy(0.7f), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(70.dp))
        }
    }
}

private fun acceptCall(
    context        : Context,
    callId         : String,
    offerSdp       : String,
    callType       : String,
    participantName: String,
    callVM         : CallViewModel,
    eglBase        : EglBase,
    navController  : NavController
) {
    callVM.answerCall(
        context       = context,
        callId        = callId,
        offerSdp      = offerSdp,
        eglBase       = eglBase,
        onLocalVideo  = {},
        onRemoteVideo = {}
    )
    FirebaseFirestore.getInstance()
        .collection("calls").document(callId)
        .update("status", "accepted")

    val encoded = java.net.URLEncoder.encode(participantName, "UTF-8")
    // ✅ نفس الـ CallScreen للاثنين
    navController.navigate("call_screen/$callId/callee/$encoded/$callType") {
        popUpTo(0) { inclusive = true }
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