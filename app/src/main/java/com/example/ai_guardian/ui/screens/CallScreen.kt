package com.example.ai_guardian.ui.screens

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.ai_guardian.viewmodel.CallViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import livekit.org.webrtc.EglBase
import livekit.org.webrtc.SurfaceViewRenderer
import livekit.org.webrtc.VideoTrack

@Composable
fun CallScreen(
    navController   : NavController,
    callId          : String,
    role            : String,   // "caller" أو "callee"
    participantName : String,
    callType        : String,   // "audio" أو "video"
    callVM          : CallViewModel,
    eglBase         : EglBase
) {
    val context      = LocalContext.current
    val db           = FirebaseFirestore.getInstance()
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    var localTrack   by remember { mutableStateOf<VideoTrack?>(null) }
    var remoteTrack  by remember { mutableStateOf<VideoTrack?>(null) }
    var isMuted      by remember { mutableStateOf(false) }
    var isSpeaker    by remember { mutableStateOf(false) }
    var isCamOn      by remember { mutableStateOf(true) }
    var elapsedSec   by remember { mutableStateOf(0) }

    // ✅ ربط الـ video callbacks
    LaunchedEffect(Unit) {
        audioManager.mode             = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = false

        callVM.attachVideoCallbacks(
            onLocalVideo  = { localTrack  = it },
            onRemoteVideo = { remoteTrack = it }
        )
    }

    // ✅ Chrono
    LaunchedEffect(Unit) {
        while (true) { delay(1000L); elapsedSec++ }
    }

    // ✅ اسمع إذا الطرف الثاني أنهى
    LaunchedEffect(callId) {
        db.collection("calls").document(callId).addSnapshotListener { snap, _ ->
            val status = snap?.getString("status") ?: return@addSnapshotListener
            if (status == "ended" || status == "rejected") {
                callVM.endCall(callId)
                safeNav(navController)
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

    if (callType == "video") {
        // ─── VIDEO UI ───────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E))) {

            // Remote video (plein écran)
            if (remoteTrack != null) {
                AndroidView(
                    factory = { ctx ->
                        SurfaceViewRenderer(ctx).apply {
                            init(eglBase.eglBaseContext, null)
                            setMirror(false)
                            remoteTrack?.addSink(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(12.dp))
                        Text("Connexion…", color = Color.White)
                    }
                }
            }

            // Nom + chrono
            Column(
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                    .background(Color(0x80000000), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(participantName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(formatTime(elapsedSec), color = Color(0xFF34C759), fontSize = 12.sp)
            }

            // Local video (coin)
            if (localTrack != null) {
                AndroidView(
                    factory = { ctx ->
                        SurfaceViewRenderer(ctx).apply {
                            init(eglBase.eglBaseContext, null)
                            setMirror(true)
                            localTrack?.addSink(this)
                        }
                    },
                    modifier = Modifier.size(100.dp, 140.dp)
                        .align(Alignment.TopEnd).padding(12.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            }

            // Boutons
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundBtn(if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    if (isMuted) Color.Gray else Color.White, Color(0x80000000)) {
                    isMuted = !isMuted; callVM.toggleMic(!isMuted)
                }
                RoundBtn(Icons.Default.CallEnd, Color.White, Color.Red, size = 64.dp) {
                    db.collection("calls").document(callId).update("status", "ended")
                    callVM.endCall(callId)
                    safeNav(navController)
                }
                RoundBtn(if (isCamOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    if (isCamOn) Color.White else Color.Gray, Color(0x80000000)) {
                    isCamOn = !isCamOn; callVM.toggleCamera(isCamOn)
                }
                RoundBtn(Icons.Default.Cameraswitch, Color.White, Color(0x80000000)) {
                    callVM.switchCamera()
                }
            }
        }
    } else {
        // ─── AUDIO UI ───────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1C1C1E)),
            contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize().padding(32.dp)) {
                Spacer(Modifier.height(100.dp))
                Box(modifier = Modifier.size(100.dp).clip(CircleShape)
                    .background(Color(0xFF1976D2)), contentAlignment = Alignment.Center) {
                    Text(participantName.take(1).uppercase(), fontSize = 38.sp,
                        fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(20.dp))
                Text(participantName, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text(formatTime(elapsedSec), fontSize = 16.sp, color = Color(0xFF34C759))
                Spacer(Modifier.weight(1f))

                // Haut-parleur + Micro
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    // Haut-parleur
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(60.dp).clip(CircleShape)
                            .background(if (isSpeaker) Color.White else Color(0xFF2C2C2E)),
                            contentAlignment = Alignment.Center) {
                            IconButton(onClick = {
                                isSpeaker = !isSpeaker
                                audioManager.isSpeakerphoneOn = isSpeaker
                                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                            }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null,
                                    tint = if (isSpeaker) Color.Black else Color.White,
                                    modifier = Modifier.size(26.dp))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(if (isSpeaker) "HP ON" else "Haut-parleur",
                            color = Color.White.copy(0.7f), fontSize = 12.sp)
                    }
                    // Micro
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(60.dp).clip(CircleShape)
                            .background(if (isMuted) Color.White else Color(0xFF2C2C2E)),
                            contentAlignment = Alignment.Center) {
                            IconButton(onClick = {
                                isMuted = !isMuted
                                callVM.toggleMic(!isMuted)
                                audioManager.isMicrophoneMute = isMuted
                            }) {
                                Icon(if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (isMuted) Color.Black else Color.White,
                                    modifier = Modifier.size(26.dp))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(if (isMuted) "Micro OFF" else "Micro",
                            color = Color.White.copy(0.7f), fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(40.dp))

                // Raccrocher
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(72.dp).clip(CircleShape)
                        .background(Color(0xFFFF3B30)), contentAlignment = Alignment.Center) {
                        IconButton(onClick = {
                            db.collection("calls").document(callId).update("status", "ended")
                            callVM.endCall(callId)
                            safeNav(navController)
                        }) {
                            Icon(Icons.Default.CallEnd, tint = Color.White,
                                contentDescription = null, modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Terminer", color = Color.White.copy(0.7f), fontSize = 13.sp)
                }
                Spacer(Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun RoundBtn(icon: ImageVector, color: Color, bgColor: Color,
                     size: androidx.compose.ui.unit.Dp = 52.dp, onClick: () -> Unit) {
    Box(modifier = Modifier.size(size).clip(CircleShape).background(bgColor),
        contentAlignment = Alignment.Center) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = null, tint = color,
                modifier = Modifier.size(size * 0.5f))
        }
    }
}

private fun safeNav(navController: NavController) {
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