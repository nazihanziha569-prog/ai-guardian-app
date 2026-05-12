package com.example.ai_guardian.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.text.font.FontWeight

@Composable
fun SosFloatingButton(
    onClick  : () -> Unit,
    modifier : Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.08f,
        animationSpec = infiniteRepeatable(
            animation  = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    FloatingActionButton(
        onClick        = onClick,
        modifier       = modifier.size(60.dp).scale(scale),
        shape          = CircleShape,
        containerColor = Color(0xFFE53935),
        contentColor   = Color.White,
        elevation      = FloatingActionButtonDefaults.elevation(8.dp)
    ) {
        Text("🆘", fontSize = 24.sp)
    }
}