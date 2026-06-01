package com.example.ai_guardian.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ai_guardian.R
import com.example.ai_guardian.service.CallListenerService
import com.example.ai_guardian.viewmodel.AuthViewModel
import com.example.ai_guardian.viewmodel.CallViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import livekit.org.webrtc.EglBase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel,
    navController: NavController,
    callVM       : CallViewModel,   // ✅ reçu de MainActivity — NE PAS recréer ici
    eglBase      : EglBase,         // ✅ idem
    onQrClick    : () -> Unit,
    onLogoutClick: () -> Unit,
) {
    var selectedScreen by remember { mutableStateOf("home") }
    var isDarkMode     by remember { mutableStateOf(false) }
    var listeningActive by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    // ✅ Écouter appels entrants vers ce superviseur
    LaunchedEffect(Unit) {

        CallListenerService.stop(context)


        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect



    }

    DisposableEffect(Unit) {
        onDispose {
            CallListenerService.start(context)
        }
    }

    MaterialTheme(colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(id = R.drawable.logo), contentDescription = "Logo", modifier = Modifier.size(45.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("AI Guardian", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    actions = {
                        IconButton(onClick = {
                            listeningActive = !listeningActive
                            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@IconButton

                            FirebaseFirestore.getInstance()
                                .collection("Associations")
                                .whereEqualTo("superviseurId", uid)
                                .get()
                                .addOnSuccessListener { result ->
                                    val surveilleeId = result.documents
                                        .firstOrNull()?.getString("superviseeId") ?: return@addOnSuccessListener
                                    sendCommandViaFirestore(
                                        surveilleeUid = surveilleeId,
                                        type = if (listeningActive) "start_listening" else "stop_listening"
                                    )
                                }

                        }) {
                            Icon(
                                imageVector = if (listeningActive) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = "Toggle listening",
                                tint = if (listeningActive) Color(0xFF43A047) else Color(0xFF9E9E9E)
                            )
                        }
                        IconButton(onClick = {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid
                            if (uid != null) FirebaseFirestore.getInstance().collection("Users").document(uid)
                                .update(mapOf("isOnline" to false, "lastSeen" to System.currentTimeMillis()))
                            FirebaseAuth.getInstance().signOut()
                            onLogoutClick()
                        }) { Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color(0xFF1976D2)) }
                    }
                )
            },
            bottomBar = { BottomNavBar(selected = selectedScreen, onItemSelected = { selectedScreen = it }) },
            floatingActionButton = {
                FloatingActionButton(onClick = onQrClick, containerColor = Color(0xFF1976D2)) {
                    Icon(Icons.Default.QrCode, contentDescription = "QR", tint = Color.White)
                }
            },
            containerColor = Color(0xFFF5F7FA)
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                when (selectedScreen) {
                    "home"     -> HomeContent(authViewModel = authViewModel, navController = navController, callVM = callVM, eglBase = eglBase)
                    "alerts"   -> AlertsScreen(isSupervisor = true)
                    "history"  -> HistoryScreen()
                    "settings" -> SettingsScreen(isDarkMode = isDarkMode, onToggleDarkMode = { isDarkMode = it }, onProfileClick = { navController.navigate("profile") })
                }
            }
        }
    }
}


fun sendCommandViaFirestore(surveilleeUid: String, type: String) {
    FirebaseFirestore.getInstance()
        .collection("FCMCommands")
        .add(hashMapOf(
            "token_uid"  to surveilleeUid,
            "type"       to type,
            "timestamp"  to System.currentTimeMillis()
        ))
}

@Composable
fun BottomNavBar(selected: String, onItemSelected: (String) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(selected == "home",     { onItemSelected("home") },     icon = { Text("🏠") }, label = { Text("Home") })
        NavigationBarItem(selected == "alerts",   { onItemSelected("alerts") },   icon = { Text("🚨") }, label = { Text("Alerts") })
        NavigationBarItem(selected == "history",  { onItemSelected("history") },  icon = { Text("📜") }, label = { Text("History") })
        NavigationBarItem(selected == "settings", { onItemSelected("settings") }, icon = { Text("⚙️") }, label = { Text("Settings") })
    }
}