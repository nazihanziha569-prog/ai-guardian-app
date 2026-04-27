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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.ai_guardian.data.model.User
import com.example.ai_guardian.viewmodel.AuthViewModel
import androidx.compose.foundation.clickable
import androidx.compose.material3.TextField
import androidx.compose.runtime.*

@Composable
fun HomeContent(
    authViewModel: AuthViewModel,
    navController: NavController
) {

    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }

    val list = authViewModel.surveilles.filter {
        it.nom.contains(searchQuery, ignoreCase = true)
    }

    LaunchedEffect(Unit) {
        authViewModel.loadSurveilles()
        authViewModel.loadAlertsCount()
    }

    Column {

        // 🔍 HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                "Personnes surveillées",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            if (isSearchVisible) {

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher...") },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "✖",
                    fontSize = 18.sp,
                    modifier = Modifier
                        .clickable {
                            searchQuery = ""
                            isSearchVisible = false
                        }
                        .padding(8.dp)
                )

            } else {

                Text(
                    text = "🔍",
                    fontSize = 22.sp,
                    modifier = Modifier
                        .clickable { isSearchVisible = true }
                        .padding(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // LIST
        if (list.isEmpty()) {
            Text("Aucune personne liée ❌")
        } else {
            list.forEach { user ->
                val count = authViewModel.alertsCount[user.uid] ?: 0

                UserCard(
                    user = user,
                    alertCount = count,
                    onClick = {
                        navController.navigate("details/${user.uid}")
                    }
                )
            }
        }
    }
}
fun isUserOnline(user: User): Boolean {
    val last = user.lastSeen ?: 0L
    val now = System.currentTimeMillis()

    return (now - last) < 10000 // 10 sec rule
}
@Composable
fun UserCard(
    user: User,
    alertCount: Int,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {

        Box {

            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // 👤 AVATAR
                if (user.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = user.imageUrl,
                        contentDescription = "avatar",
                        modifier = Modifier
                            .size(55.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(55.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE3F2FD)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            user.nom.take(1),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // INFO
                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        text = user.nom,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "📞 ${user.phone}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                // STATUS
                Column(horizontalAlignment = Alignment.End) {
                    val online = isUserOnline(user)
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(

                        if (online) Color(0xFF4CAF50)
                        else Color(0xFFF44336)
                            )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (online) "🟢 Online" else "🔴 Offline",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            // 🚨 ALERT BADGE
            if (alertCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            Color(0xFF1976D2),
                            shape = CircleShape
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$alertCount",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}