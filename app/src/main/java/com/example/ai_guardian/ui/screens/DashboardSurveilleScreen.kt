package com.example.ai_guardian.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.example.ai_guardian.R
import com.example.ai_guardian.viewmodel.AlertViewModel
import com.example.ai_guardian.viewmodel.CallViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardSurveilleScreen(
    alertViewModel: AlertViewModel,
    navController: NavController,
    onLogoutClick: () -> Unit
) {

    var selectedScreen by remember { mutableStateOf("home") }

    val context = LocalContext.current
    val activity = context as Activity

    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var lastFallTime by remember { mutableStateOf(0L) }
    val threshold = 25f

    val sensorListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val acceleration = sqrt(x*x + y*y + z*z)

                if (acceleration > threshold) {
                    val currentTime = System.currentTimeMillis()

                    if (currentTime - lastFallTime > 10000) {
                        lastFallTime = currentTime

                        alertViewModel.sendAlert(
                            type = "danger",
                            message = "⚠️ Chute détectée automatiquement",
                            onSuccess = {
                                Toast.makeText(context, "Chute détectée 🚨", Toast.LENGTH_SHORT).show()
                            },
                            onError = {}
                        )
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    // 🔥 START
    LaunchedEffect(Unit) {

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid != null) {
            FirebaseFirestore.getInstance()
                .collection("Users")
                .document(uid)
                .update("isOnline", true)
        }

        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1
        )

        startLocationUpdates(context)

        accelerometer?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    // 🔴 STOP
    DisposableEffect(Unit) {
        onDispose {

            val uid = FirebaseAuth.getInstance().currentUser?.uid

            if (uid != null) {
                FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(uid)
                    .update(
                        "isOnline", false,
                        "lastSeen", System.currentTimeMillis()
                    )
            }

            sensorManager.unregisterListener(sensorListener)
        }
    }

    // 🎨 UI
    Scaffold(

        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Logo",
                            modifier = Modifier.size(50.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "AI Guardian",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2) // blue AI style
                        )

                    }
                },
                actions = {
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        onLogoutClick()
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        },

        bottomBar = {
            BottomNavBar(
                selected = selectedScreen,
                onItemSelected = { selectedScreen = it }
            )
        }

    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            when (selectedScreen) {

                "home" -> {

                    Text(
                        text = "Actions",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    // 🔥 ROW 1
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        ActionButton("🟢", Color(0xFFFF9800)) {
                            alertViewModel.sendAlert("normal", "Besoin d'aide", {}, {})
                        }

                        ActionButton("🚨", Color.Red) {
                            alertViewModel.sendAlert("danger", "Danger 🚨", {}, {})
                        }
                    }

                    Spacer(modifier = Modifier.height(25.dp))

                    // 🔥 ROW 2
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        ActionButton("⏰", Color(0xFF1976D2)) {
                            navController.navigate("rappel")
                        }

                        val callViewModel = remember { CallViewModel() }

                        ActionButton("📞", Color(0xFF4CAF50)) {

                            val fromId = FirebaseAuth.getInstance().currentUser?.uid ?: return@ActionButton

                            FirebaseFirestore.getInstance()
                                .collection("Associations")
                                .whereEqualTo("superviseeId", fromId)
                                .get()
                                .addOnSuccessListener { result ->

                                    if (!result.isEmpty) {

                                        val toId = result.documents[0].getString("superviseurId") ?: return@addOnSuccessListener

                                        callViewModel.sendCall(fromId, toId) {

                                            Toast.makeText(context, "📞 Call sent", Toast.LENGTH_SHORT).show()
                                        }

                                    } else {
                                        Toast.makeText(context, "❌ No supervisor found", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }}
                }

                "alerts" -> AlertsScreen(isSupervisor = false)
                "history" -> HistoryScreen()
                "settings" -> SettingsScreen(
                    isDarkMode = false,
                    onToggleDarkMode = {}
                )
            }
        }
    }
}

@Composable
fun ActionButton(
    emoji: String,
    color: Color,
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = color,
        modifier = Modifier.size(85.dp)
    ) {
        Text(text = emoji, fontSize = 26.sp)
    }
}


@SuppressLint("MissingPermission")
fun startLocationUpdates(context: Context) {

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    val request = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        5000 // كل 5 ثواني
    ).build()

    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {

            val location = result.lastLocation ?: return

            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

            FirebaseFirestore.getInstance()
                .collection("Users")
                .document(uid)
                .update(
                    "latitude", location.latitude,
                    "longitude", location.longitude,
                    "lastSeen", System.currentTimeMillis(), // 🔥 IMPORTANT
                    "isOnline", true
                )
        }
    }

    fusedLocationClient.requestLocationUpdates(
        request,
        callback,
        Looper.getMainLooper()
    )
}