package com.example.ai_guardian.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_guardian.data.model.Alert
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlertCard(alert: Alert) {

    val type = alert.type.ifBlank { "info" }
    val message = alert.message.ifBlank { "No message" }

    val color = when (type.lowercase()) {
        "danger" -> Color(0xFFF44336)
        "warning" -> Color(0xFFFF9800)
        "info" -> Color(0xFF1976D2)
        else -> Color(0xFF9E9E9E)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {

        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            //nom
            Text(
                text = "👤 ${alert.superviseeName}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            Spacer(modifier = Modifier.height(6.dp))

            // 🔖 TYPE BADGE
            Box(
                modifier = Modifier
                    .background(
                        color.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = type.uppercase(),
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }


            Spacer(modifier = Modifier.height(10.dp))

            // 📝 MESSAGE
            Text(
                text = message,
                fontSize = 15.sp,
                color = Color.Black,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ⏱ TIME
            Text(
                text = "⏱ ${formatTimestamp(alert.timestamp)}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "Unknown time"

    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}