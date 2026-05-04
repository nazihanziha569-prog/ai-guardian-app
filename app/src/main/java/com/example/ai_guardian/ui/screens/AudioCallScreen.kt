package com.example.ai_guardian.ui.screens

import android.content.Context
import android.media.AudioManager
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ai_guardian.data.repository.WebRTCRepository
import com.example.ai_guardian.viewmodel.CallViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import livekit.org.webrtc.EglBase
import com.example.ai_guardian.ui.components.OutgoingCallButton

@Composable
fun AudioCallScreen(
    navController   : NavController,
    callId          : String,
    participantName : String,
    callVM          : CallViewModel = remember { CallViewModel() }
) {
    val context      = LocalContext.current
    val db           = FirebaseFirestore.getInstance()
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    var elapsedSeconds by remember { mutableStateOf(0) }
    var isMuted        by remember { mutableStateOf(false) }
    var isSpeaker      by remember { mutableStateOf(false) }
    val eglBase = remember { EglBase.create() }

    val webRTC = remember { WebRTCRepository(context) }

    LaunchedEffect(Unit) {
        webRTC.init(eglBase)
        callVM.webRTC = webRTC
    }

    // ✅ Timer
    LaunchedEffect(Unit) {
        audioManager.mode             = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = false
        while (true) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    // ✅ Écouter si l'autre raccroche
    LaunchedEffect(callId) {
        db.collection("calls").document(callId)
            .addSnapshotListener { snap, _ ->
                val status = snap?.getString("status") ?: return@addSnapshotListener
                if (status == "ended" || status == "rejected") {
                    callVM.endCall(callId)
                    safeBackFromCall(navController)
                }
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = AudioManager.MODE_NORMAL
        }
    }

    fun formatTime(s: Int) = "%02d:%02d".format(s / 60, s % 60)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E)),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp)
        ) {
            Spacer(Modifier.height(100.dp))

            // Avatar
            Box(
                modifier = Modifier.size(100.dp).clip(CircleShape)
                    .background(Color(0xFF1976D2)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    participantName.take(1).uppercase(),
                    fontSize   = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(participantName, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(formatTime(elapsedSeconds), fontSize = 16.sp, color = Color.White.copy(alpha = 0.6f))

            Spacer(Modifier.weight(1f))

            // Boutons
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Haut-parleur
                OutgoingCallButton(
                    icon    = Icons.Default.VolumeUp,
                    label   = "Haut-parleur",
                    active  = isSpeaker,
                    enabled = true
                ) {
                    isSpeaker = !isSpeaker
                    audioManager.isSpeakerphoneOn = isSpeaker
                }

                // Micro
                OutgoingCallButton(
                    icon    = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    label   = if (isMuted) "Activé" else "Muet",
                    active  = isMuted,
                    enabled = true
                ) {
                    isMuted = !isMuted
                    callVM.toggleMic(!isMuted)
                    audioManager.isMicrophoneMute = isMuted
                }
            }

            Spacer(Modifier.height(40.dp))

            // Raccrocher
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape)
                        .background(Color(0xFFFF3B30)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = {
                        audioManager.isSpeakerphoneOn = false
                        audioManager.mode = AudioManager.MODE_NORMAL
                        db.collection("calls").document(callId).update("status", "ended")
                        callVM.endCall(callId)
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.CallEnd, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Terminer", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            }

            Spacer(Modifier.height(60.dp))
        }
    }
}
private fun safeBackFromCall(navController: NavController) {
    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    if (uid == null) {
        navController.navigate("login") { popUpTo(0) { inclusive = true } }
        return
    }
    com.google.firebase.firestore.FirebaseFirestore.getInstance()
        .collection("Users").document(uid).get()
        .addOnSuccessListener { doc ->
            val dest = when (doc.getString("role") ?: "") {
                "superviseur" -> "dashboard"
                "surveille"   -> "dashboard_surveille"
                else          -> "welcome"
            }
            navController.navigate(dest) {
                popUpTo(0) { inclusive = true }
            }
        }
}