package com.example.ai_guardian.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_guardian.data.model.Call
import com.example.ai_guardian.utils.formatTime
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

@Composable
fun CallCard(call: Call, currentUid: String?) {

    val isOutgoing    = call.from == currentUid
    val accentColor   = Color(0xFF4CAF50)
    val directionIcon = if (isOutgoing) "📤" else "📥"
    val directionText = if (isOutgoing) "Appel sortant" else "Appel entrant"

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(call.id) {
        while (true) {
            delay(1000L)
            now = System.currentTimeMillis()
        }
    }

    val displayStatus = when {
        call.status == "accepted"                              -> "accepted"
        call.status == "rejected"                             -> "rejected"
        call.status == "pending" && (now - call.timestamp) <= 60_000L -> "pending"
        else                                                   -> "missed"
    }

    val statusColor = when (displayStatus) {
        "accepted" -> Color(0xFF4CAF50)
        "rejected" -> Color(0xFFF44336)
        "missed"   -> Color(0xFF9E9E9E)
        else       -> Color(0xFFFF9800)
    }

    val statusLabel = when (displayStatus) {
        "accepted" -> "✅ Accepté"
        "rejected" -> "❌ Refusé"
        "missed"   -> "📵 Manqué"
        else       -> "⏳ En attente"
    }
    var fromName by remember { mutableStateOf(call.from) }
    var toName   by remember { mutableStateOf(call.to) }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(call.id) {
        db.collection("Users").document(call.from).get().addOnSuccessListener {
            fromName = it.getString("nom") ?: call.from
        }

        db.collection("Users").document(call.to).get().addOnSuccessListener {
            toName = it.getString("nom") ?: call.to
        }
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(90.dp)
                    .background(accentColor, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text       = "$directionIcon $directionText",
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color      = accentColor
                        )
                    }
                    Text(
                        text       = statusLabel,
                        fontSize   = 12.sp,
                        color      = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text       = if (isOutgoing) "📞 Vers : $toName" else "📞 De : $fromName",
                    fontSize   = 13.sp,
                    color      = Color(0xFF333333),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text     = "⏱ ${formatTime(call.timestamp)}",
                    fontSize = 12.sp,
                    color    = Color.Gray
                )
            }
        }
    }
}