package com.example.ai_guardian.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ai_guardian.data.model.Config
import com.example.ai_guardian.service.AiService
import com.example.ai_guardian.service.KeywordListenerService
import com.example.ai_guardian.ui.components.CallTypeDialog
import com.example.ai_guardian.ui.components.SosFloatingButton
import com.example.ai_guardian.utils.AppEvents
import com.example.ai_guardian.viewmodel.AlertViewModel
import com.example.ai_guardian.viewmodel.CallViewModel
import com.example.ai_guardian.viewmodel.ChatViewModel
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import livekit.org.webrtc.EglBase

private val SBlue   = Color(0xFF1976D2)
private val SRed    = Color(0xFFE53935)
private val SOrange = Color(0xFFFB8C00)
private val SGreen  = Color(0xFF43A047)
private val SGray   = Color(0xFF9E9E9E)
private val SBg     = Color(0xFFF5F7FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardSurveilleScreen(
    alertViewModel: AlertViewModel,
    navController : NavController,
    callVM        : CallViewModel,
    eglBase       : EglBase,
    onLogoutClick : () -> Unit
) {
    var selectedScreen by remember { mutableStateOf("home") }
    val context  = LocalContext.current
    val activity = context as Activity

    // ✅ ChatViewModel créé ici — vit aussi longtemps que le Dashboard
    val chatViewModel = remember { ChatViewModel() }

    // ✅ config + userName remontés au niveau Dashboard pour les passer au chatbot
    var config   by remember { mutableStateOf<Config?>(null) }
    var userName by remember { mutableStateOf("Utilisateur") }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

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
                FirebaseFirestore.getInstance()
                    .collection("LocationHistory")
                    .document(uid).collection("points")
                    .add(hashMapOf(
                        "lat"       to loc.latitude,
                        "lng"       to loc.longitude,
                        "timestamp" to System.currentTimeMillis()
                    ))
            }
        }
    }

    LaunchedEffect(Unit) {
        KeywordListenerService.start(context)
        launch {
            AppEvents.events.collect { event ->
                when (event) {
                    is AppEvents.Event.OpenChatbot -> {
                        selectedScreen = "chat"
                    }
                    is AppEvents.Event.OpenChatbotEmergency -> {
                        selectedScreen = "chat"
                        chatViewModel.sendWelcomeEmergency(event.detectedText)
                    }
                }
            }
        }


        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect

        FirebaseFirestore.getInstance()
            .collection("Users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                userName = userDoc.getString("nom") ?: "Utilisateur"

                FirebaseFirestore.getInstance()
                    .collection("Users").document(uid)
                    .update("isOnline", true)

                val aiServiceIntent = Intent(context, AiService::class.java).apply {
                    putExtra("surveillee_name", userName)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(aiServiceIntent)
                } else {
                    context.startService(aiServiceIntent)
                }
            }

        // ✅ Config en temps réel
        FirebaseFirestore.getInstance()
            .collection("SurveilleConfig").document(uid)
            .addSnapshotListener { snap, _ ->
                if (snap != null && snap.exists()) {
                    config = Config(
                        userId         = uid,
                        age            = (snap.getLong("age") ?: 0L).toInt(),
                        maladies       = snap.getString("maladies") ?: "",
                        localisation   = snap.getBoolean("localisation") ?: true,
                        alertesActives = snap.getBoolean("alertesActives") ?: true,
                        heureReveil    = snap.getString("heureReveil") ?: "07:00",
                        heureSommeil   = snap.getString("heureSommeil") ?: "22:00"
                    )
                }
            }

        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CALL_PHONE),
            1
        )
        startLocationUpdates(fusedLocationClient, locationCallback)

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
    // ── وقف/بدء KeywordService حسب الـ tab ───────────────────────────────
    LaunchedEffect(selectedScreen) {
        if (selectedScreen == "chat") {
            KeywordListenerService.stop(context)   // ✅ الشات يسمع
        } else {
            KeywordListenerService.start(context)  // ✅ keyword يسمع
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            KeywordListenerService.stop(context)

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                FirebaseFirestore.getInstance().collection("Users").document(uid)
                    .update("isOnline", false, "lastSeen", System.currentTimeMillis())
            }
            fusedLocationClient.removeLocationUpdates(locationCallback)
            context.stopService(Intent(context, AiService::class.java))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(id = R.drawable.logo),
                            contentDescription = "Logo", modifier = Modifier.size(45.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("AI Guardian", fontSize = 20.sp,
                            fontWeight = FontWeight.Bold, color = SBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
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
            SurveilleBottomNavBar(
                selected       = selectedScreen,
                onItemSelected = { selectedScreen = it }
            )
        },
        // ✅ SOS FAB — visible sur tous les tabs sauf le chatbot
        floatingActionButton = {
            if (selectedScreen != "chat") {
                SosFloatingButton(onClick = { selectedScreen = "chat" })
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = SBg
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedScreen) {
                "home"    -> SurveilleHomeTab(alertViewModel, navController, callVM, eglBase,
                    config = config, userName = userName)
                "alerts"  -> AlertsScreen(isSupervisor = false)
                "history" -> HistoryScreen()

                // ✅ Tab chatbot
                "chat"    -> ChatbotScreen(
                    viewModel  = chatViewModel,
                    config     = config,
                    userName   = userName,
                    onSosClick = {
                        alertViewModel.sendAlert("danger", "Danger 🚨 via SOS chatbot", {}, {})
                    }
                )

                "settings" -> SettingsScreen(
                    isDarkMode       = false,
                    onToggleDarkMode = {},
                    onProfileClick   = { navController.navigate("profile") }
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// SurveilleHomeTab — reçoit config + userName depuis le Dashboard
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun SurveilleHomeTab(
    alertViewModel: AlertViewModel,
    navController : NavController,
    callVM        : CallViewModel,
    eglBase       : EglBase,
    config        : Config?,
    userName      : String
) {
    val context = LocalContext.current
    var superviseurId  by remember { mutableStateOf("") }
    var showCallDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect

        FirebaseFirestore.getInstance()
            .collection("Associations")
            .whereEqualTo("superviseeId", uid).get()
            .addOnSuccessListener { result ->
                superviseurId = result.documents.firstOrNull()?.getString("superviseurId") ?: ""
            }
    }

    if (showCallDialog && superviseurId.isNotEmpty()) {
        CallTypeDialog(
            calleeName = "Superviseur",
            onAudio = {
                showCallDialog = false
                val fromId = FirebaseAuth.getInstance().currentUser?.uid ?: return@CallTypeDialog
                callVM.sendCall(fromId, superviseurId, callType = "audio") { callId ->
                    navController.navigate("outgoing_call/$callId/$superviseurId/audio")
                }
            },
            onVideo = {
                showCallDialog = false
                val fromId = FirebaseAuth.getInstance().currentUser?.uid ?: return@CallTypeDialog
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
        modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp),
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
            BigActionCard("🟡", "Besoin d'aide", "Alerte normale",
                Color(0xFFFFF3E0), SOrange, Modifier.weight(1f)) {
                alertViewModel.sendAlert("normal", "Besoin d'aide", {
                    Toast.makeText(context, "Alerte envoyée ✅", Toast.LENGTH_SHORT).show()
                }, {})
            }
            BigActionCard("🚨", "Danger !", "Alerte critique",
                Color(0xFFFFEBEE), SRed, Modifier.weight(1f)) {
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
                if (superviseurId.isEmpty())
                    Toast.makeText(context, "❌ Aucun superviseur trouvé", Toast.LENGTH_SHORT).show()
                else showCallDialog = true
            }
        }

        SectionTitle("📡 Mon statut")

        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    StatusItem(Icons.Default.LocationOn, "Localisation",
                        if (config?.localisation != false) "Active" else "Désactivée",
                        if (config?.localisation != false) SGreen else SGray)
                    VerticalDivider(modifier = Modifier.height(40.dp))
                    StatusItem(Icons.Default.Sensors, "Capteur chute", "Actif", SGreen)
                    VerticalDivider(modifier = Modifier.height(40.dp))
                    StatusItem(Icons.Default.Notifications, "Alertes",
                        if (config?.alertesActives != false) "Activées" else "Désactivées",
                        if (config?.alertesActives != false) SGreen else SGray)
                }

                if (config != null) {
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.WbSunny, null,
                                tint = Color(0xFFF9A825), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.height(4.dp))
                            Text(config!!.heureReveil, fontSize = 13.sp,
                                fontWeight = FontWeight.Bold, color = Color(0xFFF9A825))
                            Text("Réveil", fontSize = 10.sp, color = Color.Gray)
                        }
                        Icon(Icons.Default.ArrowForward, null,
                            tint = Color.LightGray, modifier = Modifier.size(16.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Bedtime, null,
                                tint = Color(0xFF7B1FA2), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.height(4.dp))
                            Text(config!!.heureSommeil, fontSize = 13.sp,
                                fontWeight = FontWeight.Bold, color = Color(0xFF7B1FA2))
                            Text("Sommeil", fontSize = 10.sp, color = Color.Gray)
                        }
                        if (config!!.maladies.isNotBlank()) {
                            VerticalDivider(modifier = Modifier.height(40.dp))
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier            = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.LocalHospital, null,
                                    tint = SRed, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.height(4.dp))
                                Text(config!!.maladies, fontSize = 10.sp,
                                    color = Color.Gray, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ════════════════════════════════════════════════════════════════════════════
// BottomNavBar — avec tab Assistant
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun SurveilleBottomNavBar(selected: String, onItemSelected: (String) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(
            selected = selected == "home",
            onClick  = { onItemSelected("home") },
            icon     = { Icon(Icons.Default.Home, null) },
            label    = { Text("Accueil") }
        )
        NavigationBarItem(
            selected = selected == "alerts",
            onClick  = { onItemSelected("alerts") },
            icon     = { Icon(Icons.Default.Notifications, null) },
            label    = { Text("Alertes") }
        )
        NavigationBarItem(
            selected = selected == "history",
            onClick  = { onItemSelected("history") },
            icon     = { Icon(Icons.Default.History, null) },
            label    = { Text("Historique") }
        )
        // ✅ Tab chatbot
        NavigationBarItem(
            selected = selected == "chat",
            onClick  = { onItemSelected("chat") },
            icon     = {
                Icon(Icons.Default.Chat, null,
                    tint = if (selected == "chat") SBlue else SGray)
            },
            label    = { Text("Assistant") }
        )
        NavigationBarItem(
            selected = selected == "settings",
            onClick  = { onItemSelected("settings") },
            icon     = { Icon(Icons.Default.Settings, null) },
            label    = { Text("Paramètres") }
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
// UI Components
// ════════════════════════════════════════════════════════════════════════════
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
        Column(modifier = Modifier.padding(18.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally) {
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
        Row(modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {
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
    FloatingActionButton(onClick = onClick, containerColor = color,
        modifier = Modifier.size(85.dp)) {
        Text(text = emoji, fontSize = 26.sp)
    }
}

@SuppressLint("MissingPermission")
fun startLocationUpdates(
    fusedLocationClient: FusedLocationProviderClient,
    callback: LocationCallback
) {
    fusedLocationClient.requestLocationUpdates(
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5 * 60 * 1000L).build(),
        callback,
        Looper.getMainLooper()
    )
}