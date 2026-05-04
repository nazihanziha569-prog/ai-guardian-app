package com.example.ai_guardian.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OutgoingCallButton(
    icon    : ImageVector,
    label   : String,
    active  : Boolean,
    enabled : Boolean = true,
    onClick : () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    when {
                        !enabled -> Color(0xFF3A3A3C)
                        active   -> Color.White
                        else     -> Color(0xFF3A3A3C)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onClick, enabled = enabled) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint     = if (active) Color.Black else Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            color    = Color.White.copy(alpha = if (enabled) 0.7f else 0.3f),
            fontSize = 12.sp
        )
    }
}