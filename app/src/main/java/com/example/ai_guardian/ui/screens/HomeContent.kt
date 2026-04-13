package com.example.ai_guardian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ai_guardian.data.model.User
import com.example.ai_guardian.viewmodel.AuthViewModel


@Composable
fun HomeContent(authViewModel: AuthViewModel) {

    val list = authViewModel.surveilles

    LaunchedEffect(Unit) {
        authViewModel.loadSurveilles()
    }

    Column {

        Text("Personnes surveillées", fontSize = 20.sp,
            fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(10.dp))

        if (list.isEmpty()) {
            Text("Aucune personne liée ❌")
        } else {
            list.forEach { user ->
                UserCard(user)
            }
        }
    }
}
@Composable
fun UserCard(user: User) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {

        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 🔥 Avatar
            if (user.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = user.imageUrl,
                    contentDescription = "avatar",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(user.nom.take(1), fontSize = 20.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 🔥 Infos
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.nom,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = user.email,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            // 🔥 Status
            Row(verticalAlignment = Alignment.CenterVertically) {

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (user.isOnline) Color.Green else Color.Red)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = if (user.isOnline) "Online" else "Offline",
                    fontSize = 12.sp
                )
            }
        }
    }
}