package com.example.ai_guardian.ui.components


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun CallTypeDialog(
    calleeName : String,
    onAudio    : () -> Unit,
    onVideo    : () -> Unit,
    onDismiss  : () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier            = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1976D2)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        calleeName.take(1).uppercase(),
                        fontSize   = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Appeler $calleeName",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )

                Text(
                    "Choisir le type d'appel",
                    fontSize = 13.sp,
                    color    = Color.White.copy(alpha = 0.5f)
                )

                Spacer(Modifier.height(28.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // ── Audio ──────────────────────────────────────────────
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF43A047)),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = { onAudio(); onDismiss() }) {
                                Icon(
                                    Icons.Default.Call,
                                    contentDescription = null,
                                    tint     = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Audio", color = Color.White, fontSize = 13.sp)
                    }

                    // ── Vidéo ──────────────────────────────────────────────
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1976D2)),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = { onVideo(); onDismiss() }) {
                                Icon(
                                    Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint     = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Vidéo", color = Color.White, fontSize = 13.sp)
                    }
                }

                Spacer(Modifier.height(20.dp))

                TextButton(onClick = onDismiss) {
                    Text("Annuler", color = Color.White.copy(alpha = 0.5f))
                }
            }
        }
    }
}