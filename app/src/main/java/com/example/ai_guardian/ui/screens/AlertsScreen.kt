package com.example.ai_guardian.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ai_guardian.viewmodel.AlertViewModel

@Composable
fun AlertsScreen() {

    val viewModel = remember { AlertViewModel() }

    // 🔥 realtime listener
    LaunchedEffect(true) {
        viewModel.listenAlerts()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text("Alerts", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(10.dp))

        viewModel.alerts.forEach { alert ->

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Text("Type: ${alert.type}")

                    Text(
                        text = alert.message,
                        color = if (alert.type == "danger") Color.Red else Color.Black
                    )

                    Text("From: ${alert.superviseeId}")
                }
            }
        }
    }
}