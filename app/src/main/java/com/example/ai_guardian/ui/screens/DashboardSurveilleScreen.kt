package com.example.ai_guardian.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.ai_guardian.viewmodel.AlertViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun DashboardSurveilleScreen(
    alertViewModel: AlertViewModel,
    onLogoutClick: () -> Unit
) {

    val context = LocalContext.current
    val activity = context as Activity

    LaunchedEffect(Unit) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1
        )

        startLocationUpdates(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        // 🔴 Logout
        IconButton(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                onLogoutClick()
            },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(Icons.Default.Logout, contentDescription = "Logout")
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Envoyer une alerte",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 🟢 Alert normal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                // 🟠 NORMAL ALERT
                FloatingActionButton(
                    onClick = {
                        alertViewModel.sendAlert(
                            type = "normal",
                            message = "",
                            onSuccess = {
                                Toast.makeText(context, "Alerte envoyée ✅", Toast.LENGTH_SHORT).show()
                            },
                            onError = {
                                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    containerColor = Color(0xFFFF9800),
                    modifier = Modifier.size(90.dp)
                ) {
                    Text("🟢", fontSize = 28.sp)
                }

                // 🔴 DANGER ALERT
                FloatingActionButton(
                    onClick = {
                        alertViewModel.sendAlert(
                            type = "danger",
                            message = "",
                            onSuccess = {
                                Toast.makeText(context, "ALERTE DANGER 🚨", Toast.LENGTH_SHORT).show()
                            },
                            onError = {
                                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    containerColor = Color.Red,
                    modifier = Modifier.size(90.dp)
                ) {
                    Text("🚨", fontSize = 28.sp)
                }
            }}}}


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
                    "longitude", location.longitude
                )
        }
    }

    fusedLocationClient.requestLocationUpdates(
        request,
        callback,
        Looper.getMainLooper()
    )
}