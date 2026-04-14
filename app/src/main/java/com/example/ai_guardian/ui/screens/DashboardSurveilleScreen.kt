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
import androidx.compose.material3.FloatingActionButton

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                // 🟠 NORMAL ALERT
                FloatingActionButton(
                    onClick = {
                        alertViewModel.sendAlert(
                            type = "normal",
                            message = "",
                            onSuccess = {
                                Toast.makeText(context, "Alerte envoyée ✅", Toast.LENGTH_SHORT).show()
                            },
                            onError = {
                                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    containerColor = Color(0xFFFF9800),
                    modifier = Modifier.size(90.dp)
                ) {
                    Text("🟢", fontSize = 28.sp)
                }

                // 🔴 DANGER ALERT
                FloatingActionButton(
                    onClick = {
                        alertViewModel.sendAlert(
                            type = "danger",
                            message = "",
                            onSuccess = {
                                Toast.makeText(context, "ALERTE DANGER 🚨", Toast.LENGTH_SHORT).show()
                            },
                            onError = {
                                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    containerColor = Color.Red,
                    modifier = Modifier.size(90.dp)
                ) {
                    Text("🚨", fontSize = 28.sp)
                }
            }}}}