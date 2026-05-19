package com.example.ai_guardian.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.ai_guardian.viewmodel.CallViewModel
import livekit.org.webrtc.EglBase
import livekit.org.webrtc.SurfaceViewRenderer
import livekit.org.webrtc.VideoTrack

@Composable
fun VideoCallScreen(
    navController   : NavController,
    callId          : String,
    roomName        : String,       // non-vide si appelant
    participantName : String,
    callVM          : CallViewModel,
    // ✅ FIX: reçoit eglBase de MainActivity — ne crée plus son propre EglBase
    eglBase         : EglBase
) {
    var localTrack  by remember { mutableStateOf<VideoTrack?>(null) }
    var remoteTrack by remember { mutableStateOf<VideoTrack?>(null) }
    var micEnabled  by remember { mutableStateOf(true) }
    var camEnabled  by remember { mutableStateOf(true) }
    var statusText  by remember { mutableStateOf("Connexion en cours...") }
    var permGranted by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val isOffer = roomName.isNotBlank() // appelant = true

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val camOk   = perms[Manifest.permission.CAMERA]       == true
        val audioOk = perms[Manifest.permission.RECORD_AUDIO] == true

        if (camOk && audioOk) {
            permGranted = true // ✅ permissions OK → شغّل الـ call
        } else {
            statusText = "❌ Permissions caméra/micro refusées"
        }
    }

    LaunchedEffect(Unit) {
        permLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }

    LaunchedEffect(permGranted) {
        if (!permGranted) return@LaunchedEffect

        val isCallerSide = roomName == "offer"

        if (isCallerSide) {
            // ✅ المتصل — الـ WebRTC شغّال مسبقاً في OutgoingCallScreen
            // بس نربط الـ video callbacks
            callVM.attachVideoCallbacks(
                onLocalVideo  = { localTrack  = it },
                onRemoteVideo = { remoteTrack = it; statusText = "Connecté ✅" }
            )
        } else {
            // ✅ المستجيب — اقرأ الـ offer واعمل answer
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("calls").document(callId).get()
                .addOnSuccessListener { doc ->
                    val offerSdp = doc.getString("offer") ?: return@addOnSuccessListener
                    callVM.answerCall(
                        context       = context,
                        callId        = callId,
                        offerSdp      = offerSdp,
                        eglBase       = eglBase,
                        onLocalVideo  = { localTrack  = it },
                        onRemoteVideo = { remoteTrack = it; statusText = "Connecté ✅" }
                    )
                }
        }
    }

    // ✅ FIX: NE PAS libérer eglBase ici — il appartient à MainActivity
    DisposableEffect(Unit) {
        onDispose {
            callVM.endCall(callId)
            // eglBase.release() — NON, géré par MainActivity
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E))) {

        // Vidéo distante (plein écran)
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
                    Spacer(Modifier.height(16.dp))
                    Text(statusText, color = Color.White, fontSize = 16.sp)
                }
            }
        }

        // Nom participant
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color(0x80000000), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("📞 $participantName", color = Color.White, fontSize = 14.sp)
        }

        // Vidéo locale (coin)
        if (localTrack != null) {
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        init(eglBase.eglBaseContext, null)
                        setMirror(true)
                        localTrack?.addSink(this)
                    }
                },
                modifier = Modifier
                    .size(120.dp, 160.dp)
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }

        // Boutons contrôle
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CallControlButton(
                icon = if (micEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                color = if (micEnabled) Color.White else Color.Gray,
                bgColor = Color(0x80000000)
            ) { micEnabled = !micEnabled; callVM.toggleMic(micEnabled) }

            CallControlButton(
                icon = Icons.Default.CallEnd, color = Color.White, bgColor = Color.Red, size = 64.dp
            ) { callVM.endCall(callId); navController.popBackStack() }

            CallControlButton(
                icon = if (camEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                color = if (camEnabled) Color.White else Color.Gray,
                bgColor = Color(0x80000000)
            ) { camEnabled = !camEnabled; callVM.toggleCamera(camEnabled) }

            CallControlButton(
                icon = Icons.Default.Cameraswitch, color = Color.White, bgColor = Color(0x80000000)
            ) { callVM.switchCamera() }
        }
    }
}

@Composable
fun CallControlButton(icon: ImageVector, color: Color, bgColor: Color, size: Dp = 52.dp, onClick: () -> Unit) {
    Box(modifier = Modifier.size(size).clip(CircleShape).background(bgColor), contentAlignment = Alignment.Center) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size * 0.5f))
        }
    }
}