package com.example.ai_guardian.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ai_guardian.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun DashboardScreen(authViewModel: AuthViewModel,
                    onQrClick: () -> Unit,
                    onLogoutClick: () -> Unit,

                    ) {

    var selectedScreen by remember { mutableStateOf("home") }

    Box(modifier = Modifier.fillMaxSize()) {
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
            },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Icon(Icons.Default. Logout, contentDescription = "Logout", tint = Color.Red)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 90.dp)
                .padding(16.dp)
        ) {
            when (selectedScreen) {
                "home" -> HomeContent(authViewModel)
                "alerts" -> AlertsScreen()
                "settings" -> SettingsScreen()
                "history" -> HistoryScreen()
            }
        }

        BottomNavBar(
            selected = selectedScreen,
            onItemSelected = { selectedScreen = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 35.dp)
        )

        FloatingActionButton(
            onClick = { onQrClick() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp),
            containerColor = Color.White
        ) {
            Icon(Icons.Default.QrCode, contentDescription = "QR", tint = Color.Blue)
        }
    }
}
@Composable
fun BottomNavBar(
    selected: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        Button(onClick = { onItemSelected("home") }) {
            Text("Home")
        }
        Button(onClick = { onItemSelected("alerts") }) {
            Text("Alerts")
        }
        Button(onClick = { onItemSelected("history") }) {
            Text("History")
        }
        Button(onClick = { onItemSelected("settings") }) {
            Text("Settings")
        }
    }
}