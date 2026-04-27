package com.example.ai_guardian.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_guardian.ui.components.AlertCard
import com.example.ai_guardian.viewmodel.AlertViewModel

@Composable
fun AlertsScreen(isSupervisor: Boolean) {

    val viewModel = remember { AlertViewModel() }

    LaunchedEffect(Unit) {

        if (isSupervisor) {
            viewModel.listenAlerts()       // 🧑‍💼
        } else {
            viewModel.listenMyAlerts()    // 👤
        }
    }

    val alerts = viewModel.alerts

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        item {
            Text(
                text = "🚨 Alerts",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color(0xFF1976D2)
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (alerts.isEmpty()) {
            item {
                Text("No alerts yet ✅", color = Color.Gray)
            }
        } else {
            items(alerts) { alert ->
                AlertCard(alert)
            }
        }
    }
}