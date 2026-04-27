package com.example.ai_guardian.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RatingBar(
    rating: Int,
    onRatingChanged: (Int) -> Unit
) {
    Row(modifier = Modifier.padding(8.dp)) {

        for (i in 1..5) {
            Text(
                text = if (i <= rating) "★" else "☆",
                fontSize = 32.sp,
                color = if (i <= rating) Color(0xFFFFC107) else Color.Gray,
                modifier = Modifier
                    .padding(6.dp)
                    .clickable { onRatingChanged(i) }
            )
        }
    }
}