package com.example.ai_guardian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.ai_guardian.data.model.Alert
import com.example.ai_guardian.data.model.User
import com.example.ai_guardian.ui.components.AlertCard
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.*


@Composable
fun DetailsScreen(userId: String) {

    var user by remember { mutableStateOf<User?>(null) }
    var alerts by remember { mutableStateOf<List<Alert>>(emptyList()) }

    val scrollState = rememberScrollState()

    LaunchedEffect(userId) {

        val db = FirebaseFirestore.getInstance()

        db.collection("Users")
            .document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    user = snapshot.toObject(User::class.java)
                }
            }

        db.collection("Alerts")
            .whereEqualTo("superviseeId", userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    alerts = snapshot.documents.mapNotNull {
                        it.toObject(Alert::class.java)
                    }
                }
            }
    }
    println("USER ONLINE STATUS = ${user?.isOnline}")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {

        // 👤 USER CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {

            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier
                        .size(85.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE3F2FD)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user?.nom?.take(1) ?: "?",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = user?.nom ?: "",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text("📞 ${user?.phone ?: ""}", color = Color.Gray)
                Text("📧 ${user?.email ?: ""}", color = Color.Gray)

                Spacer(modifier = Modifier.height(12.dp))
                // 🔥 MAP HERE
                user?.let {
                    if (it.latitude != null && it.longitude != null) {
                        UserLocationMap(
                            latitude = it.latitude,
                            longitude = it.longitude
                        )
                    }
                }

                // STATUS
                val isOnline = user?.let {
                    it.isOnline || (System.currentTimeMillis() - it.lastSeen < 10000)
                } ?: false
                Box(
                    modifier = Modifier
                        .background(
                            if (isOnline)
                                Color(0xFFE8F5E9)
                            else
                                Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isOnline) "🟢 Online" else "🔴 Offline",
                        color = if (isOnline)
                            Color(0xFF2E7D32)
                        else
                            Color(0xFFC62828),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "🚨 Alerts",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2)
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (alerts.isEmpty()) {
            Text("No alerts found ✅", color = Color.Gray)
        } else {
            alerts.forEach { alert ->
                AlertCard(alert)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
@Composable
fun UserLocationMap(latitude: Double, longitude: Double) {

    val position = LatLng(latitude, longitude)

    val cameraPositionState = rememberCameraPositionState {
        this.position = CameraPosition.fromLatLngZoom(position, 15f)
    }

    GoogleMap(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            state = MarkerState(position = position),
            title = "Position"
        )
    }
}

