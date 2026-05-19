package com.example.ai_guardian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_guardian.service.AlarmService
import android.content.Intent

@Composable
fun AlarmScreen(message: String, onStop: () -> Unit) {

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFD32F2F)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text("🚨 ALARM", fontSize = 40.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(message, fontSize = 20.sp, color = Color.White)
            Spacer(Modifier.height(30.dp))

            Button(
                onClick = {
                    // ✅ وقّف السيرفيس = يوقف الصوت + يلغي الـ notification
                    context.stopService(Intent(context, AlarmService::class.java))
                    onStop()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Red
                )
            ) {
                Text("STOP", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}