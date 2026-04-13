package com.example.ai_guardian.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_guardian.viewmodel.AlertViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.ButtonDefaults

@Composable
fun DashboardSurveilleScreen(
    alertViewModel: AlertViewModel,
    onLogoutClick: () -> Unit
) {

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        // 🔴 Logout
        IconButton(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                onLogoutClick()
            },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(Icons.Default.Logout, contentDescription = "Logout")
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Envoyer une alerte",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 🟢 Alert normal
            Button(
                onClick = {
                    alertViewModel.sendAlert(
                        type = "normal",
                        onSuccess = {
                            Toast.makeText(context, "Alerte envoyée ✅", Toast.LENGTH_SHORT).show()
                        },
                        onError = {
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text("Alerte normale 🟢")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 🔴 Alert danger
            Button(
                onClick = {
                    alertViewModel.sendAlert(
                        type = "danger",
                        onSuccess = {
                            Toast.makeText(context, "ALERTE DANGER 🚨", Toast.LENGTH_SHORT).show()
                        },
                        onError = {
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Alerte DANGER 🔴")
            }
        }
    }
}