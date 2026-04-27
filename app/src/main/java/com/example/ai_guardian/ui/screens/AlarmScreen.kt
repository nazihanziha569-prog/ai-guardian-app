package com.example.ai_guardian.ui.screens

import android.media.MediaPlayer
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
import com.example.ai_guardian.R

@Composable
fun AlarmScreen(
    message: String,
    onStop: () -> Unit
) {

    val context = LocalContext.current

    val mediaPlayer = remember {
        MediaPlayer.create(context, R.raw.alarm_sound)
    }

    DisposableEffect(Unit) {

        mediaPlayer.isLooping = true
        mediaPlayer.start()

        onDispose {
            mediaPlayer.stop()
            mediaPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFD32F2F)),
        contentAlignment = Alignment.Center
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text(
                text = "🚨 ALARM",
                fontSize = 40.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = message,
                fontSize = 20.sp,
                color = Color.White
            )

            Spacer(Modifier.height(30.dp))

            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Red
                )
            ) {
                Text("STOP")
            }
        }
    }
}