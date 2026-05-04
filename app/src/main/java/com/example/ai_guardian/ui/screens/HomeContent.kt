package com.example.ai_guardian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import coil.compose.AsyncImage
import com.example.ai_guardian.data.model.User
import com.example.ai_guardian.viewmodel.AuthViewModel
import com.example.ai_guardian.viewmodel.CallViewModel
import com.google.firebase.auth.FirebaseAuth
import livekit.org.webrtc.EglBase

@Composable
fun HomeContent(
    authViewModel: AuthViewModel,
    navController: NavController,
    callVM       : CallViewModel,   // ✅ reçu du DashboardScreen
    eglBase      : EglBase          // ✅ reçu du DashboardScreen
) {
    var searchQuery   by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val list = authViewModel.surveilles.filter {
        it.nom.contains(searchQuery, ignoreCase = true)
    }

    LaunchedEffect(Unit) {
        authViewModel.loadSurveilles()
        authViewModel.loadAlertsCount()
    }

    Column {

        // Header
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Personnes surveillées", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))

            if (isSearchVisible) {
                TextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder   = { Text("Rechercher...") },
                    modifier      = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                Text("✖", fontSize = 18.sp, modifier = Modifier.clickable { searchQuery = ""; isSearchVisible = false }.padding(8.dp))
            } else {
                Text("🔍", fontSize = 22.sp, modifier = Modifier.clickable { isSearchVisible = true }.padding(8.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        if (list.isEmpty()) {
            Text("Aucune personne liée ❌")
        } else {
            list.forEach { user ->
                val count = authViewModel.alertsCount[user.uid] ?: 0
                UserCard(
                    user       = user,
                    alertCount = count,
                    onClick    = { navController.navigate("details/${user.uid}") },
                    // ✅ Bouton appel dans la carte
                    onCall     = {
                        val fromId = FirebaseAuth.getInstance().currentUser?.uid ?: return@UserCard

                        // 1. Créer le doc dans Firestore
                        callVM.sendCall(fromId, user.uid) { callId ->

                            // 2. Démarrer WebRTC + créer offer

                            callVM.startCall(
                                context       = context,
                                callId        = callId,
                                eglBase       = eglBase,
                                onLocalVideo  = {},
                                onRemoteVideo = {},
                                // ✅ 3. Naviguer SEULEMENT quand offer est écrit dans Firestore
                                onOfferReady = {
                                    navController.navigate("outgoing_call/$callId/${user.uid}/video")
                                }
                            )
                        }
                    }
                )
            }
        }
    }
}

fun isUserOnline(user: User): Boolean {
    val now = System.currentTimeMillis()
    return (now - user.lastSeen) < 15000
}

@Composable
fun UserCard(
    user      : User,
    alertCount: Int,
    onClick   : () -> Unit,
    onCall    : () -> Unit = {}
) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() },
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Box {
            Row(
                modifier          = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                if (user.imageUrl.isNotEmpty()) {
                    AsyncImage(model = user.imageUrl, contentDescription = null, modifier = Modifier.size(55.dp).clip(CircleShape))
                } else {
                    Box(
                        modifier = Modifier.size(55.dp).clip(CircleShape).background(Color(0xFFE3F2FD)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(user.nom.take(1), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(user.nom, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text("📞 ${user.phone}", fontSize = 13.sp, color = Color.Gray)
                }

                // Statut online
                Column(horizontalAlignment = Alignment.End) {
                    val online = isUserOnline(user)
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (online) Color(0xFF4CAF50) else Color(0xFFF44336)))
                    Spacer(Modifier.height(4.dp))
                    Text(if (online) "🟢" else "🔴", fontSize = 11.sp)
                }
            }



            // Badge alerte
            if (alertCount > 0) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        .background(Color(0xFF1976D2), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("$alertCount", color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}