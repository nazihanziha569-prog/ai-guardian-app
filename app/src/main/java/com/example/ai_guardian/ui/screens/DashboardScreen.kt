package com.example.ai_guardian.ui.screens

import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ai_guardian.R
import com.example.ai_guardian.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel,
    navController: NavController,
    onQrClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {

    var selectedScreen by remember { mutableStateOf("home") }
    var isDarkMode by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // 🔥 CALL LISTENER (CORRECT PLACE)
    LaunchedEffect(Unit) {

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect

        FirebaseFirestore.getInstance()
            .collection("calls")
            .whereEqualTo("to", uid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->

                snapshot?.documents?.forEach { doc ->

                    val from = doc.getString("from") ?: ""
                    val callId = doc.id

                    // 🔥 هوني تحط الشرط
                    if (doc.getString("status") == "pending"
                        && navController.currentDestination?.route != "incoming_call/{from}/{callId}"
                    ) {
                        navController.navigate("incoming_call/$from/$callId")
                    }
                }
            }
    }


    MaterialTheme(
        colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()
    ){

    Scaffold(

        // 🌟 LIGHT TOP BAR
        topBar = {
            TopAppBar(

                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Logo",
                            modifier = Modifier.size(45.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "AI Guardian",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2) // blue AI style
                        )

                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                ),

                actions = {

                    IconButton(
                        onClick = {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid

                            if (uid != null) {
                                FirebaseFirestore.getInstance()
                                    .collection("Users")
                                    .document(uid)
                                    .update(
                                        mapOf(
                                            "isOnline" to false,
                                            "lastSeen" to System.currentTimeMillis()
                                        )
                                    )
                            }

                            FirebaseAuth.getInstance().signOut()
                            onLogoutClick()
                        }
                    ) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = Color(0xFF1976D2)
                        )
                    }
                }
            )
        },

        // 📱 Bottom Nav LIGHT
        bottomBar = {
            BottomNavBar(
                selected = selectedScreen,
                onItemSelected = { selectedScreen = it }
            )
        },

        // 📸 QR Button
        floatingActionButton = {
            FloatingActionButton(
                onClick = onQrClick,
                containerColor = Color(0xFF1976D2)
            ) {
                Icon(
                    Icons.Default.QrCode,
                    contentDescription = "QR",
                    tint = Color.White
                )
            }
        },

        containerColor = Color(0xFFF5F7FA) // 🌤 light background

    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {


            when (selectedScreen) {
                "home" -> HomeContent(authViewModel, navController)
                "alerts" -> AlertsScreen(isSupervisor = true)
                "history" -> HistoryScreen()
                "settings" -> SettingsScreen(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { isDarkMode = it }
                )
            }


        }
    }
}}
@Composable
fun BottomNavBar(
    selected: String,
    onItemSelected: (String) -> Unit,
) {
    NavigationBar(
        containerColor = Color.White
    ) {

        NavigationBarItem(
            selected = selected == "home",
            onClick = { onItemSelected("home") },
            icon = { Text("🏠") },
            label = { Text("Home") }
        )

        NavigationBarItem(
            selected = selected == "alerts",
            onClick = { onItemSelected("alerts") },
            icon = { Text("🚨") },
            label = { Text("Alerts") }
        )

        NavigationBarItem(
            selected = selected == "history",
            onClick = { onItemSelected("history") },
            icon = { Text("📜") },
            label = { Text("History") }
        )

        NavigationBarItem(
            selected = selected == "settings",
            onClick = { onItemSelected("settings") },
            icon = { Text("⚙️") },
            label = { Text("Settings") }
        )
    }
}
@Composable
fun IncomingCallScreen(
    navController: NavController,
    fromUser: String,
    callId: String,
    onAccept: () -> Unit = {},
    onReject: () -> Unit = {}
) {

    val context = LocalContext.current

    val ringtone = remember {
        MediaPlayer.create(context, R.raw.ringtone).apply {
            isLooping = true
            start()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ringtone.stop()
            ringtone.release()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text("📞 Incoming Call", fontSize = 28.sp)

            Text("From: $fromUser")

            Spacer(Modifier.height(20.dp))

            Row {

                // ✅ ACCEPT
                Button(onClick = {

                    FirebaseFirestore.getInstance()
                        .collection("calls")
                        .document(callId)
                        .update("status", "accepted")

                    Toast.makeText(context, "Call accepted", Toast.LENGTH_SHORT).show()

                    onAccept()

                }) {
                    Text("Accept")
                }

                Spacer(Modifier.width(10.dp))

                // ❌ REJECT
                Button(onClick = {

                    FirebaseFirestore.getInstance()
                        .collection("calls")
                        .document(callId)
                        .update("status", "rejected")

                    Toast.makeText(context, "Call rejected", Toast.LENGTH_SHORT).show()

                    navController.popBackStack() // ✅ تخدم توّا

                }) {
                    Text("Reject")
                }
            }
        }
    }
}