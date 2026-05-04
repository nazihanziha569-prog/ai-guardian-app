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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.example.ai_guardian.R
import com.example.ai_guardian.ui.components.CallTypeDialog
import com.example.ai_guardian.viewmodel.AlertViewModel
import com.example.ai_guardian.viewmodel.CallViewModel
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import livekit.org.webrtc.EglBase
import kotlin.math.sqrt

private val SBlue   = Color(0xFF1976D2)
private val SRed    = Color(0xFFE53935)
private val SOrange = Color(0xFFFB8C00)
private val SGreen  = Color(0xFF43A047)
private val SBg     = Color(0xFFF5F7FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardSurveilleScreen(
    alertViewModel: AlertViewModel,
    navController : NavController,
    // ✅ reçus de MainActivity — même instance que DashboardScreen
    callVM        : CallViewModel,
    eglBase       : EglBase,
    onLogoutClick : () -> Unit
) {
    var selectedScreen by remember { mutableStateOf("home") }

    val context       = LocalContext.current
    val activity      = context as Activity
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    var lastFallTime  by remember { mutableStateOf(0L) }

    val sensorListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = event.values
                val acceleration = sqrt(x * x + y * y + z * z)
                if (acceleration > 25f) {
                    val now = System.currentTimeMillis()
                    if (now - lastFallTime > 10000) {
                        lastFallTime = now
                        alertViewModel.sendAlert(
                            "danger", "⚠️ Chute détectée automatiquement",
                            onSuccess = { Toast.makeText(context, "Chute détectée 🚨", Toast.LENGTH_SHORT).show() },
                            onError   = {}
                        )
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
                FirebaseFirestore.getInstance().collection("Users").document(uid)
                    .update(
                        "latitude",  loc.latitude,
                        "longitude", loc.longitude,
                        "lastSeen",  System.currentTimeMillis(),
                        "isOnline",  true
                    )
            }
        }
    }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect

        FirebaseFirestore.getInstance().collection("Users").document(uid).update("isOnline", true)

        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1
        )
        startLocationUpdates(fusedLocationClient, locationCallback)
        accelerometer?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // ✅ Écouter appels entrants (superviseur appelle le surveillé)
        FirebaseFirestore.getInstance()
            .collection("calls")
            .whereEqualTo("to", uid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documents?.forEach { doc ->
                    val from   = doc.getString("from") ?: ""
                    val callId = doc.id
                    val route  = navController.currentDestination?.route ?: ""
                    if (!route.startsWith("incoming_call") &&
                        !route.startsWith("active_call")   &&
                        !route.startsWith("outgoing_call") &&
                        !route.startsWith("video_call")) {
                        navController.navigate("incoming_call/$from/$callId")
                    }
                }
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                FirebaseFirestore.getInstance().collection("Users").document(uid)
                    .update("isOnline", false, "lastSeen", System.currentTimeMillis())
            }
            fusedLocationClient.removeLocationUpdates(locationCallback)
            sensorManager.unregisterListener(sensorListener)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painterResource(id = R.drawable.logo),
                            contentDescription = "Logo",
                            modifier = Modifier.size(45.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("AI Guardian", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SBlue)
                    }
                },
                colors  = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                actions = {
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        onLogoutClick()
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = SBlue)
                    }
                }
            )
        },
        bottomBar = {
            BottomNavBar(selected = selectedScreen, onItemSelected = { selectedScreen = it })
        },
        containerColor = SBg
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedScreen) {
                "home"     -> SurveilleHomeTab(
                    alertViewModel = alertViewModel,
                    navController  = navController,
                    // ✅ passer callVM + eglBase
                    callVM         = callVM,
                    eglBase        = eglBase
                )
                "alerts"   -> AlertsScreen(isSupervisor = false)
                "history"  -> HistoryScreen()
                "settings" -> SettingsScreen(
                    isDarkMode       = false,
                    onToggleDarkMode = {},
                    onProfileClick   = { navController.navigate("profile") }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SurveilleHomeTab — reçoit callVM + eglBase
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SurveilleHomeTab(
    alertViewModel: AlertViewModel,
    navController : NavController,
    callVM        : CallViewModel,   // ✅ partagé
    eglBase       : EglBase          // ✅ partagé
) {
    val context = LocalContext.current

    var userName       by remember { mutableStateOf("") }
    var superviseurId  by remember { mutableStateOf("") }
    var showCallDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect

        FirebaseFirestore.getInstance().collection("Users").document(uid).get()
            .addOnSuccessListener { doc -> userName = doc.getString("nom") ?: "" }

        FirebaseFirestore.getInstance()
            .collection("Associations")
            .whereEqualTo("superviseeId", uid)
            .get()
            .addOnSuccessListener { result ->
                superviseurId = result.documents.firstOrNull()?.getString("superviseurId") ?: ""
            }
    }

    // ✅ Dialog choix type d'appel
    if (showCallDialog && superviseurId.isNotEmpty()) {
        CallTypeDialog(
            calleeName = "Superviseur",
            onAudio = {
                showCallDialog = false
                val fromId = FirebaseAuth.getInstance().currentUser?.uid ?: return@CallTypeDialog

                // ✅ sendCall → puis startCall → onOfferReady → navigate outgoing
                callVM.sendCall(fromId, superviseurId, callType = "audio") { callId ->
                    navController.navigate("outgoing_call/$callId/$superviseurId/audio")
                }
            },
            onVideo = {
                showCallDialog = false
                val fromId = FirebaseAuth.getInstance().currentUser?.uid ?: return@CallTypeDialog

                // ✅ sendCall → startCall (crée offer WebRTC) → onOfferReady → navigate
                callVM.sendCall(fromId, superviseurId, callType = "video") { callId ->
                    callVM.startCall(
                        context       = context,
                        callId        = callId,
                        eglBase       = eglBase,
                        onLocalVideo  = {},
                        onRemoteVideo = {},
                        onOfferReady  = {
                            navController.navigate("outgoing_call/$callId/$superviseurId/video")
                        }
                    )
                }
            },
            onDismiss = { showCallDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            "Bonjour${if (userName.isNotEmpty()) ", $userName 👋" else " 👋"}",
            fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SBlue
        )
        Text("Que souhaitez-vous faire ?", fontSize = 14.sp, color = Color.Gray)

        SectionTitle("🚨 Alertes d'urgence")

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigActionCard(
                emoji = "🟡", label = "Besoin d'aide", subtitle = "Alerte normale",
                bgColor = Color(0xFFFFF3E0), borderColor = SOrange, modifier = Modifier.weight(1f)
            ) {
                alertViewModel.sendAlert("normal", "Besoin d'aide", {
                    Toast.makeText(context, "Alerte envoyée ✅", Toast.LENGTH_SHORT).show()
                }, {})
            }
            BigActionCard(
                emoji = "🚨", label = "Danger !", subtitle = "Alerte critique",
                bgColor = Color(0xFFFFEBEE), borderColor = SRed, modifier = Modifier.weight(1f)
            ) {
                alertViewModel.sendAlert("danger", "Danger 🚨", {
                    Toast.makeText(context, "Alerte danger envoyée 🚨", Toast.LENGTH_SHORT).show()
                }, {})
            }
        }

        SectionTitle("⚡ Actions rapides")

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallActionCard("⏰", "Rappel", SBlue, Modifier.weight(1f)) {
                navController.navigate("rappel")
            }
            SmallActionCard("📞", "Appeler", SGreen, Modifier.weight(1f)) {
                if (superviseurId.isEmpty()) {
                    Toast.makeText(context, "❌ Aucun superviseur trouvé", Toast.LENGTH_SHORT).show()
                } else {
                    showCallDialog = true
                }
            }
        }

        SectionTitle("📡 Mon statut")

        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusItem(Icons.Default.LocationOn,    "Localisation",  "Active",   SGreen)
                VerticalDivider(modifier = Modifier.height(40.dp))
                StatusItem(Icons.Default.Sensors,       "Capteur chute", "Actif",    SGreen)
                VerticalDivider(modifier = Modifier.height(40.dp))
                StatusItem(Icons.Default.Notifications, "Alertes",       "Activées", SGreen)
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Composants UI
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
}

@Composable
private fun BigActionCard(
    emoji: String, label: String, subtitle: String,
    bgColor: Color, borderColor: Color,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Card(
        onClick   = onClick, modifier = modifier, shape = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(3.dp),
        border    = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 36.sp)
            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun SmallActionCard(
    emoji: String, label: String, color: Color,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Card(
        onClick   = onClick, modifier = modifier, shape = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(2.dp),
        border    = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun StatusItem(icon: ImageVector, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
fun ActionButton(emoji: String, color: Color, onClick: () -> Unit) {
    FloatingActionButton(
        onClick        = onClick,
        containerColor = color,
        modifier       = Modifier.size(85.dp)
    ) {
        Text(text = emoji, fontSize = 26.sp)
    }
}

@SuppressLint("MissingPermission")
fun startLocationUpdates(
    fusedLocationClient: FusedLocationProviderClient,
    callback           : LocationCallback
) {
    fusedLocationClient.requestLocationUpdates(
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build(),
        callback,
        Looper.getMainLooper()
    )
}