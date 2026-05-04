package com.example.ai_guardian.ui.screens

import android.Manifest
import android.content.Context
import android.media.AudioManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ai_guardian.viewmodel.CallViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import livekit.org.webrtc.EglBase

private val BgTop    = Color(0xFF1A1035)
private val BgBot    = Color(0xFF0F0F14)
private val AccGreen = Color(0xFF34C759)
private val AccRed   = Color(0xFFFF3B30)
private val AccBlue  = Color(0xFF1976D2)
private val W70      = Color.White.copy(0.7f)
private val W40      = Color.White.copy(0.4f)

// ─────────────────────────────────────────────────────────────────────────────
// ActiveCallScreen — unique pour appelant (isOutgoing=true) ET appelé (false)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ActiveCallScreen(
    navController   : NavController,
    callId          : String,
    participantName : String,
    isOutgoing      : Boolean,       // true = appelant, false = appelé
    callType        : String = "audio",
    callVM          : CallViewModel,
    eglBase         : EglBase
) {
    val context      = LocalContext.current
    val db           = FirebaseFirestore.getInstance()
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    var isMuted      by remember { mutableStateOf(false) }
    var isSpeaker    by remember { mutableStateOf(false) }
    var isConnected  by remember { mutableStateOf(false) }
    var isAccepting  by remember { mutableStateOf(false) }
    var elapsedSec   by remember { mutableStateOf(0) }
    var timeLeft     by remember { mutableStateOf(60) }
    var permOk       by remember { mutableStateOf(false) }
    var statusText   by remember { mutableStateOf(
        if (isOutgoing) "Appel en cours…" else "Appel entrant"
    ) }

    // ✅ FIX 3: Demander permissions caméra+micro dès l'entrée dans l'écran
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        permOk = perms[Manifest.permission.CAMERA]       == true &&
                perms[Manifest.permission.RECORD_AUDIO] == true
    }

    LaunchedEffect(Unit) {
        permLauncher.launch(arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ))
    }

    // Audio mode
    DisposableEffect(Unit) {
        audioManager.mode             = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = false
        onDispose {
            audioManager.mode             = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = false
        }
    }

    // ✅ FIX 2: Écouter Firestore — les DEUX reçoivent les changements de status
    LaunchedEffect(callId) {
        db.collection("calls").document(callId)
            .addSnapshotListener { snap, _ ->
                val status = snap?.getString("status") ?: return@addSnapshotListener
                when (status) {
                    // Appelant : l'appelé a accepté → connecté
                    "accepted" -> {
                        if (!isConnected) {
                            isConnected = true
                            statusText  = "Connecté"
                        }
                    }
                    // ✅ FIX 4: les DEUX sortent proprement
                    "rejected" -> {
                        callVM.endCall(callId)
                        safeNavigateBack(navController)
                    }
                    "ended" -> {
                        callVM.endCall(callId)
                        safeNavigateBack(navController)
                    }
                    "missed" -> {
                        callVM.endCall(callId)
                        safeNavigateBack(navController)
                    }
                }
            }
    }

    // Chrono (quand connecté)
    LaunchedEffect(isConnected) {
        if (isConnected) {
            while (true) { delay(1000L); elapsedSec++ }
        }
    }

    // ✅ FIX 1 + Timeout 60s si pas de réponse
    LaunchedEffect(callId) {
        while (timeLeft > 0 && !isConnected) {
            delay(1000L)
            timeLeft--
        }
        if (!isConnected && timeLeft == 0) {
            db.collection("calls").document(callId).update("status", "missed")
            callVM.endCall(callId)
            safeNavigateBack(navController)
        }
    }

    // Pulse animation
    val pulse = rememberInfiniteTransition(label = "pulse").animateFloat(
        1f, if (!isConnected) 1.1f else 1f,
        infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "ps"
    )

    // ─── UI ──────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBot))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp)
        ) {
            Spacer(Modifier.height(80.dp))

            // Type d'appel
            Text(
                if (callType == "video") "📹 Appel vidéo" else "📞 Appel audio",
                fontSize = 14.sp, color = W40
            )

            Spacer(Modifier.height(20.dp))

            // Avatar pulsant
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(pulse.value)
                    .clip(CircleShape)
                    .background(AccBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    participantName.take(1).uppercase(),
                    fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Color.White
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(participantName, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)

            Spacer(Modifier.height(10.dp))

            // Statut / chrono
            when {
                isConnected -> Text(
                    formatCallDuration(elapsedSec),
                    fontSize = 18.sp, color = AccGreen, fontWeight = FontWeight.Medium
                )
                else -> Text(statusText, fontSize = 16.sp, color = W70)
            }

            // Countdown (si pas encore connecté)
            if (!isConnected) {
                Text(
                    "⏱ ${timeLeft}s",
                    fontSize = 13.sp,
                    color = if (timeLeft <= 10) AccRed else W40
                )
            }

            Spacer(Modifier.weight(1f))

            // ─── Boutons haut-parleur + micro ─────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Haut-parleur
                SecondaryCallBtn(
                    icon   = Icons.Default.VolumeUp,
                    label  = if (isSpeaker) "HP ON" else "Haut-parleur",
                    active = isSpeaker
                ) {
                    isSpeaker = !isSpeaker
                    audioManager.isSpeakerphoneOn = isSpeaker
                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                }

                // Micro
                SecondaryCallBtn(
                    icon   = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    label  = if (isMuted) "Micro OFF" else "Micro",
                    active = isMuted
                ) {
                    isMuted = !isMuted
                    callVM.toggleMic(!isMuted)
                    audioManager.isMicrophoneMute = isMuted
                }
            }

            Spacer(Modifier.height(40.dp))

            // ─── Boutons principaux ───────────────────────────────────────────
            if (!isOutgoing && !isConnected) {
                // APPELÉ : Rejeter + Accepter
                Row(
                    horizontalArrangement = Arrangement.spacedBy(60.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // ❌ Rejeter
                    PrimaryCallBtn(
                        icon  = Icons.Default.CallEnd,
                        color = AccRed,
                        label = "Rejeter",
                        size  = 70.dp
                    ) {
                        // ✅ "rejected" → listener Firestore → les DEUX sortent
                        db.collection("calls").document(callId).update("status", "rejected")
                        // Ne pas appeler popBackStack ici — le listener s'en occupe
                    }

                    // ✅ Accepter
                    Box(contentAlignment = Alignment.Center) {
                        if (isAccepting) {
                            Box(
                                modifier = Modifier.size(70.dp).clip(CircleShape).background(Color.Gray),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(30.dp), strokeWidth = 2.dp)
                            }
                        } else {
                            PrimaryCallBtn(
                                icon  = Icons.Default.Call,
                                color = AccGreen,
                                label = "Accepter",
                                size  = 70.dp
                            ) {
                                isAccepting = true

                                // ✅ FIX 2: Lire offer → answerCall → mettre accepted → listener déclenche isConnected
                                db.collection("calls").document(callId).get()
                                    .addOnSuccessListener { doc ->
                                        val offer = doc.getString("offer") ?: ""

                                        fun doAnswer(sdp: String) {
                                            callVM.answerCall(
                                                context       = context,
                                                callId        = callId,
                                                offerSdp      = sdp,
                                                eglBase       = eglBase,
                                                onLocalVideo  = {},
                                                onRemoteVideo = {}
                                            )
                                            // ✅ Mettre accepted → appelant reçoit → isConnected=true des deux côtés
                                            db.collection("calls").document(callId)
                                                .update("status", "accepted")
                                            isAccepting = false
                                        }

                                        if (offer.isNotBlank()) {
                                            doAnswer(offer)
                                        } else {
                                            // Attendre offer si pas encore écrit
                                            db.collection("calls").document(callId)
                                                .addSnapshotListener { snap, _ ->
                                                    val o = snap?.getString("offer") ?: return@addSnapshotListener
                                                    if (o.isNotBlank()) doAnswer(o)
                                                }
                                        }
                                    }
                                    .addOnFailureListener { isAccepting = false }
                            }
                        }
                        // Label sous le bouton
                        if (!isAccepting) {
                            // déjà inclus dans PrimaryCallBtn
                        }
                    }
                }
            } else {
                // APPELANT ou connecté : Raccrocher
                PrimaryCallBtn(
                    icon  = Icons.Default.CallEnd,
                    color = AccRed,
                    label = "Terminer",
                    size  = 72.dp
                ) {
                    audioManager.isSpeakerphoneOn = false
                    audioManager.mode = AudioManager.MODE_NORMAL
                    // ✅ "ended" → listener Firestore → les DEUX sortent
                    db.collection("calls").document(callId).update("status", "ended")
                    callVM.endCall(callId)
                }
            }

            Spacer(Modifier.height(70.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ✅ FIX 4: Navigation sécurisée — évite l'écran blanc
// ─────────────────────────────────────────────────────────────────────────────
private fun safeNavigateBack(navController: NavController) {
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
        .addOnFailureListener {
            navController.navigate("welcome") { popUpTo(0) { inclusive = true } }
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// Composants
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PrimaryCallBtn(
    icon   : ImageVector,
    color  : Color,
    label  : String,
    size   : Dp      = 64.dp,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(size).clip(CircleShape).background(if (enabled) color else Color(0xFF3A3A3C)),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onClick, enabled = enabled) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.45f))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = W70, fontSize = 13.sp)
    }
}

@Composable
private fun SecondaryCallBtn(
    icon   : ImageVector,
    label  : String,
    active : Boolean,
    onClick: () -> Unit
) {
    val bg        by animateColorAsState(if (active) Color.White else Color(0xFF2C2C2E), tween(200), label = "bg")
    val iconColor by animateColorAsState(if (active) Color.Black else Color.White, tween(200), label = "ic")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(60.dp).clip(CircleShape).background(bg),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onClick) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = W70, fontSize = 12.sp)
    }
}

private fun formatCallDuration(sec: Int) = "%02d:%02d".format(sec / 60, sec % 60)