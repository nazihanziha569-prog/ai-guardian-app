package com.example.ai_guardian.ui.screens

import android.content.Context
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
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
import com.example.ai_guardian.R
import com.example.ai_guardian.viewmodel.CallViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import livekit.org.webrtc.EglBase

@Composable
fun IncomingCallScreen(
    navController: NavController,
    fromUser     : String,           // uid de l'appelant
    callId       : String,
    callVM       : CallViewModel,
    eglBase      : EglBase,
    onAccept     : () -> Unit = {},
    onReject     : () -> Unit = {}
) {
    val context = LocalContext.current
    val db      = FirebaseFirestore.getInstance()

    var callerName    by remember { mutableStateOf("…") }
    var callerInitial by remember { mutableStateOf("?") }
    var callType      by remember { mutableStateOf("video") }
    var isAccepting   by remember { mutableStateOf(false) }
    var timeLeft      by remember { mutableStateOf(60) }
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as android.app.NotificationManager

    LaunchedEffect(callId) {
        notificationManager.cancel(callId.hashCode())
    }


    // Résoudre nom + callType
    LaunchedEffect(fromUser, callId) {
        db.collection("Users").document(fromUser).get().addOnSuccessListener { doc ->
            val nom       = doc.getString("nom") ?: fromUser
            callerName    = nom
            callerInitial = nom.take(1).uppercase()
        }
        db.collection("calls").document(callId).get().addOnSuccessListener { doc ->
            callType = doc.getString("callType") ?: "video"
        }
    }

    // Sonnerie
    val ringtone = remember {
        MediaPlayer.create(context, R.raw.ringtone)?.apply { isLooping = true; start() }
    }
    DisposableEffect(Unit) {
        onDispose { runCatching { ringtone?.stop(); ringtone?.release() } }
    }

    // ✅ Écouter Firestore : si appelant raccroche ou timeout → sortir
    LaunchedEffect(callId) {
        db.collection("calls").document(callId).addSnapshotListener { snap, _ ->
            val status = snap?.getString("status") ?: return@addSnapshotListener
            if (status == "ended" || status == "missed") {
                runCatching { ringtone?.stop() }
                navController.popBackStack()
            }
        }
    }

    // ✅ Countdown 60s → missed si pas répondu
    LaunchedEffect(callId) {
        while (timeLeft > 0 && !isAccepting) {
            delay(1000L)
            timeLeft--
        }
        if (timeLeft == 0 && !isAccepting) {
            runCatching { ringtone?.stop() }
            db.collection("calls").document(callId).update("status", "missed")
            safeBackFromCall(navController)
        }
    }

    // Animation
    val pulse = rememberInfiniteTransition(label = "p").animateFloat(
        1f, 1.1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "ps"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A1035), Color(0xFF0F0F14)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Spacer(Modifier.height(40.dp))

            // Type d'appel
            Text(
                text = if (callType == "video") "📹 Appel vidéo entrant" else "📞 Appel audio entrant",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.5f)
            )

            // Avatar pulsant
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(pulse.value)
                    .clip(CircleShape)
                    .background(Color(0xFF1976D2)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    callerInitial,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(callerName, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)

            // Countdown
            Text(
                text = "⏱ ${timeLeft}s",
                fontSize = 15.sp,
                color = if (timeLeft <= 10) Color(0xFFFF3B30) else Color.White.copy(alpha = 0.45f)
            )

            Spacer(Modifier.height(50.dp))

            // Boutons Rejeter + Accepter
            Row(
                horizontalArrangement = Arrangement.spacedBy(60.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // ❌ Rejeter
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape)
                            .background(Color(0xFFFF3B30)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = {
                            runCatching { ringtone?.stop() }
                            // ✅ rejected → les DEUX sortent via listener
                            db.collection("calls").document(callId).update("status", "rejected")
                            onReject()
                            // popBackStack déclenché par listener Firestore sur "rejected"
                        }) {
                            Icon(
                                Icons.Default.CallEnd,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Rejeter", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }

                // ✅ Accepter → ActiveCallScreen
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape)
                            .background(if (isAccepting) Color.Gray else Color(0xFF34C759)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isAccepting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(30.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = {
                                if (isAccepting) return@IconButton
                                isAccepting = true
                                runCatching { ringtone?.stop() }
                                db.collection("calls").document(callId)
                                    .update("status", "accepted")
                                    .addOnSuccessListener {

                                // ✅ Naviguer vers ActiveCallScreen (appelé)
                                navController.navigate(
                                    "active_call/$callId/${
                                        java.net.URLEncoder.encode(callerName, "UTF-8")
                                    }/false/$callType"
                                ) {
                                    // Remplacer IncomingCallScreen pour éviter d'y revenir avec Back
                                    popUpTo("incoming_call/$fromUser/$callId") { inclusive = true }
                                }
                                onAccept()}
                            }) {
                                Icon(
                                    Icons.Default.Call,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (isAccepting) "Connexion…" else "Accepter",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            }
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

