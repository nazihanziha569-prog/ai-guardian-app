package com.example.ai_guardian.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
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
                                    .update("isOnline", false)
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
                "alerts" -> AlertsScreen()
                "history" -> HistoryScreen()
                "settings" -> SettingsScreen()
            }
        }
    }
}
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