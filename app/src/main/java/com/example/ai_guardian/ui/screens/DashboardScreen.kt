package com.example.ai_guardian.ui.screens


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ai_guardian.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel,
    onQrClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {

    var selectedScreen by remember { mutableStateOf("home") }

    Scaffold(

        topBar = {
            TopAppBar(
                title = { Text("AI Guardian") },
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
                            tint = Color.Red
                        )
                    }
                }
            )
        },

        bottomBar = {
            BottomNavBar(
                selected = selectedScreen,
                onItemSelected = { selectedScreen = it }
            )
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = onQrClick,
                containerColor = Color.White
            ) {
                Icon(
                    Icons.Default.QrCode,
                    contentDescription = "QR",
                    tint = Color.Blue
                )
            }
        }

    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            when (selectedScreen) {
                "home" -> HomeContent(authViewModel)
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
    NavigationBar {

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