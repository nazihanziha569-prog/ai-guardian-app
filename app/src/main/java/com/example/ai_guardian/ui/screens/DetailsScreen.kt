package com.example.ai_guardian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ai_guardian.data.model.Alert
import com.example.ai_guardian.data.model.Call
import com.example.ai_guardian.data.model.Config
import com.example.ai_guardian.data.model.User
import com.example.ai_guardian.ui.components.CallTypeDialog
import com.example.ai_guardian.viewmodel.CallViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import livekit.org.webrtc.EglBase
import java.text.SimpleDateFormat
import java.util.*

private val DBlue     = Color(0xFF1976D2)
private val DBlueSoft = Color(0xFFE3F2FD)
private val DRedAlert = Color(0xFFE53935)
private val DOrange   = Color(0xFFFB8C00)
private val DGreen    = Color(0xFF43A047)
private val DBgPage   = Color(0xFFF5F7FA)
private val DCard     = Color.White
private val DTextMain = Color(0xFF1A1A2E)
private val DTextSub  = Color(0xFF757575)

@Composable
fun DetailsScreen(userId: String, navController: NavController, callVM: CallViewModel, eglBase: EglBase) {
    var selectedTab by remember { mutableStateOf(0) }
    Scaffold(bottomBar = { BottomNavigationBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it }) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(DBgPage)) {
            when (selectedTab) {
                0 -> TabDetails(userId, navController, callVM, eglBase)
                1 -> TabConfig(userId)
                2 -> TabAppels(userId, navController, callVM, eglBase)
            }
        }
    }
}

@Composable
fun BottomNavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar(containerColor = DCard) {
        listOf(Triple("Détails", Icons.Default.Person, 0), Triple("Config", Icons.Default.Settings, 1), Triple("Appels", Icons.Default.Call, 2))
            .forEach { (label, icon, idx) ->
                NavigationBarItem(selected = selectedTab == idx, onClick = { onTabSelected(idx) },
                    icon = { Icon(icon, contentDescription = label) }, label = { Text(label, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = DBlue, selectedTextColor = DBlue,
                        indicatorColor = DBlueSoft, unselectedIconColor = DTextSub, unselectedTextColor = DTextSub))
            }
    }
}

// ── Tab 1 ─────────────────────────────────────────────────────────────────────
@Composable
private fun TabDetails(userId: String, navController: NavController, callVM: CallViewModel, eglBase: EglBase) {
    val db         = FirebaseFirestore.getInstance()
    val context    = LocalContext.current                          // ✅ context correct
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var user       by remember { mutableStateOf<User?>(null) }
    var alerts     by remember { mutableStateOf<List<Alert>>(emptyList()) }
    var showCallDialog by remember { mutableStateOf(false) }      // ✅ dialog

    LaunchedEffect(userId) {
        db.collection("Users").document(userId).addSnapshotListener { snap, _ -> user = snap?.toObject(User::class.java) }
        db.collection("Alerts").whereEqualTo("superviseeId", userId).addSnapshotListener { snap, _ ->
            alerts = snap?.documents?.mapNotNull { it.toObject(Alert::class.java)?.copy(id = it.id) }
                ?.sortedByDescending { it.timestamp } ?: emptyList()
        }
    }

    val isOnline = user?.let { it.isOnline || (System.currentTimeMillis() - it.lastSeen < 15000) } ?: false

    // ✅ CallTypeDialog
    if (showCallDialog) {
        CallTypeDialog(
            calleeName = user?.nom ?: "",
            onAudio = {
                showCallDialog = false
                callVM.sendCall(currentUid, userId, callType = "audio") { callId ->
                    navController.navigate("outgoing_call/$callId/$userId/audio")
                }
            },
            onVideo = {
                showCallDialog = false
                callVM.sendCall(currentUid, userId, callType = "video") { callId ->
                    callVM.startCall(
                        context       = context,
                        callId        = callId,
                        eglBase       = eglBase,
                        onLocalVideo  = {},
                        onRemoteVideo = {},
                        onOfferReady  = {
                            navController.navigate("outgoing_call/$callId/$userId/video")
                        }
                    )
                }
            },
            onDismiss = { showCallDialog = false }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 14.dp)) {

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DCard), elevation = CardDefaults.cardElevation(6.dp)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                    Box {
                        Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(DBlueSoft), contentAlignment = Alignment.Center) {
                            Text(user?.nom?.take(1)?.uppercase() ?: "?", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = DBlue)
                        }
                        Box(modifier = Modifier.align(Alignment.BottomEnd).size(18.dp).clip(CircleShape)
                            .background(if (isOnline) DGreen else Color(0xFFBDBDBD)).border(2.dp, DCard, CircleShape))
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(user?.nom ?: "", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DTextMain)
                    Spacer(Modifier.height(6.dp))

                    Box(modifier = Modifier.background(if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 5.dp)) {
                        Text(if (isOnline) "🟢 En ligne" else "🔴 Hors ligne", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828))
                    }

                    Spacer(Modifier.height(14.dp)); HorizontalDivider(color = Color(0xFFEEEEEE)); Spacer(Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        InfoChip(Icons.Default.Phone, user?.phone ?: "—")
                        InfoChip(Icons.Default.Email, user?.email ?: "—")
                    }

                    Spacer(Modifier.height(14.dp))

                    // ✅ Bouton ouvre le dialog
                    Button(
                        onClick = { showCallDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = DGreen)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Appeler ${user?.nom ?: ""}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        item {
            user?.let { u ->
                if (u.latitude != null && u.longitude != null) {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(6.dp), colors = CardDefaults.cardColors(containerColor = DCard)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = DBlue, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Localisation en temps réel", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DTextMain)
                            }
                            Spacer(Modifier.height(10.dp))
                            val pos = LatLng(u.latitude, u.longitude)
                            val cam = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(pos, 15f) }
                            GoogleMap(modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(14.dp)), cameraPositionState = cam) {
                                Marker(state = MarkerState(position = pos), title = u.nom)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("📍 %.5f, %.5f".format(u.latitude, u.longitude), fontSize = 12.sp, color = DBlue)
                            Spacer(Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    navController.navigate("map_history/$userId")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DBlue)
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Afficher sur la carte", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = DRedAlert, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Alertes récentes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DTextMain)
                if (alerts.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.background(DRedAlert, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("${alerts.size}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (alerts.isEmpty()) item { Text("Aucune alerte ✅", color = DTextSub) }
        else items(alerts.take(5)) { MiniAlertCard(it) }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = DBlue, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 12.sp, color = DTextSub)
    }
}

@Composable
private fun MiniAlertCard(alert: Alert) {
    val isDanger = alert.type == "danger"; val ac = if (isDanger) DRedAlert else DOrange
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DCard), elevation = CardDefaults.cardElevation(3.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(4.dp).height(70.dp).background(ac, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)))
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.background(ac.copy(0.12f), RoundedCornerShape(50)).padding(horizontal = 9.dp, vertical = 3.dp)) {
                        Text(if (isDanger) "🚨 DANGER" else "🟡 NORMAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ac)
                    }
                    Text(dFormatTime(alert.timestamp), fontSize = 11.sp, color = DTextSub)
                }
                Spacer(Modifier.height(5.dp))
                Text(alert.message.ifBlank { "—" }, fontSize = 13.sp, color = DTextMain)
            }
        }
    }
}

// ── Tab 2 — inchangé ──────────────────────────────────────────────────────────
@Composable
private fun TabConfig(userId: String) {
    val db = FirebaseFirestore.getInstance()
    var config    by remember { mutableStateOf(Config(userId = userId)) }
    var isSaved   by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        try {
            val doc = db.collection("SurveilleConfig").document(userId).get().await()
            if (doc.exists()) config = Config(userId = userId,
                age = (doc.getLong("age") ?: 0L).toInt(), maladies = doc.getString("maladies") ?: "",
                localisation = doc.getBoolean("localisation") ?: true, alertesActives = doc.getBoolean("alertesActives") ?: true,
                heureReveil = doc.getString("heureReveil") ?: "07:00", heureSommeil = doc.getString("heureSommeil") ?: "22:00")
        } catch (e: Exception) { e.printStackTrace() }
        isLoading = false
    }

    if (isLoading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = DBlue) }; return }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(4.dp))
        DConfigSection("👤 Informations générales", Icons.Default.Person) {
            OutlinedTextField(value = if (config.age == 0) "" else config.age.toString(), onValueChange = { config = config.copy(age = it.toIntOrNull() ?: 0) },
                label = { Text("Âge", fontSize = 13.sp) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null, tint = DBlue, modifier = Modifier.size(18.dp)) })
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = config.maladies, onValueChange = { config = config.copy(maladies = it) },
                label = { Text("Maladies / conditions médicales", fontSize = 13.sp) }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp), minLines = 2,
                leadingIcon = { Icon(Icons.Default.LocalHospital, contentDescription = null, tint = DRedAlert, modifier = Modifier.size(18.dp)) })
        }
        DConfigSection("📍 Surveillance", Icons.Default.Shield) {
            DToggleRow("Localisation GPS", "Suivre la position en temps réel", Icons.Default.LocationOn, config.localisation) { config = config.copy(localisation = it) }
            HorizontalDivider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(vertical = 4.dp))
            DToggleRow("Alertes actives", "Recevoir les alertes de cette personne", Icons.Default.Notifications, config.alertesActives) { config = config.copy(alertesActives = it) }
        }
        DConfigSection("🕐 Temps d'activité & sommeil", Icons.Default.Schedule) {
            DTimeRow("Heure de réveil", "Début période active", Icons.Default.WbSunny, Color(0xFFFFF8E1), Color(0xFFF9A825), config.heureReveil) { config = config.copy(heureReveil = it) }
            HorizontalDivider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(vertical = 4.dp))
            DTimeRow("Heure de sommeil", "Fin période active", Icons.Default.Bedtime, Color(0xFFEDE7F6), Color(0xFF7B1FA2), config.heureSommeil) { config = config.copy(heureSommeil = it) }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(DBlueSoft).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Text("🌅 ${config.heureReveil}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DBlue)
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = DBlue, modifier = Modifier.size(16.dp))
                Text("🌙 ${config.heureSommeil}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DBlue)
            }
        }
        Button(onClick = {
            db.collection("SurveilleConfig").document(userId).set(
                hashMapOf(
                    "userId" to userId,
                    "age" to config.age,
                    "maladies" to config.maladies,
                    "localisation" to config.localisation,
                    "alertesActives" to config.alertesActives,
                    "heureReveil" to config.heureReveil,
                    "heureSommeil" to config.heureSommeil,

                    // ✅ NEW FIELDS
                    "inactivityThresholdMinutes" to config.inactivityThresholdMinutes,
                    "inactivityEnabled" to config.inactivityEnabled,

                    "updatedAt" to System.currentTimeMillis()
                )
            ).addOnSuccessListener { isSaved = true }
        }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DBlue)) {
            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White); Spacer(Modifier.width(8.dp))
            Text("Sauvegarder la configuration", color = Color.White, fontWeight = FontWeight.Bold)
        }
        if (isSaved) {
            LaunchedEffect(isSaved) { delay(2500); isSaved = false }
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFE8F5E9)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DGreen); Spacer(Modifier.width(8.dp))
                Text("Configuration sauvegardée ✅", color = DGreen, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DConfigSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DCard), elevation = CardDefaults.cardElevation(3.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = DBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DTextMain)
            }
            Spacer(Modifier.height(14.dp)); content()
        }
    }
}

@Composable
private fun DToggleRow(label: String, subtitle: String, icon: ImageVector, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(DBlueSoft), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = DBlue, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DTextMain); Text(subtitle, fontSize = 12.sp, color = DTextSub) }
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = DBlue, checkedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFE0E0E0)))
    }
}

@Composable
private fun DTimeRow(label: String, subtitle: String, icon: ImageVector, iconBg: Color, iconTint: Color, value: String, onChange: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(iconBg), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DTextMain); Text(subtitle, fontSize = 12.sp, color = DTextSub) }
        OutlinedTextField(value = value, onValueChange = onChange, modifier = Modifier.width(88.dp), shape = RoundedCornerShape(10.dp), singleLine = true,
            placeholder = { Text("HH:mm", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DBlue))
    }
}

// ── Tab 3 ─────────────────────────────────────────────────────────────────────
@Composable
private fun TabAppels(userId: String, navController: NavController, callVM: CallViewModel, eglBase: EglBase) {
    val db         = FirebaseFirestore.getInstance()
    val context    = LocalContext.current                          // ✅ context correct
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var calls      by remember { mutableStateOf<List<Call>>(emptyList()) }
    var userName   by remember { mutableStateOf("") }
    var isLoading  by remember { mutableStateOf(true) }
    var showCallDialog by remember { mutableStateOf(false) }      // ✅ dialog

    LaunchedEffect(userId) {
        db.collection("Users").document(userId).get()
            .addOnSuccessListener { doc -> userName = doc.getString("nom") ?: "" }
        db.collection("calls").addSnapshotListener { snap, _ ->
            if (snap == null) { isLoading = false; return@addSnapshotListener }
            calls = snap.documents.mapNotNull { doc ->
                val from = doc.getString("from") ?: return@mapNotNull null
                val to   = doc.getString("to")   ?: return@mapNotNull null
                if (!((from == currentUid && to == userId) || (from == userId && to == currentUid))) return@mapNotNull null
                Call(id = doc.id, from = from, to = to, status = doc.getString("status") ?: "unknown",
                    timestamp = doc.getLong("timestamp") ?: 0L)
            }.sortedByDescending { it.timestamp }
            isLoading = false
        }
    }

    // ✅ CallTypeDialog
    if (showCallDialog) {
        CallTypeDialog(
            calleeName = userName,
            onAudio = {
                showCallDialog = false
                callVM.sendCall(currentUid, userId, callType = "audio") { callId ->
                    navController.navigate("outgoing_call/$callId/$userId/audio")
                }
            },
            onVideo = {
                showCallDialog = false
                callVM.sendCall(currentUid, userId, callType = "video") { callId ->
                    callVM.startCall(
                        context       = context,
                        callId        = callId,
                        eglBase       = eglBase,
                        onLocalVideo  = {},
                        onRemoteVideo = {},
                        onOfferReady  = {
                            navController.navigate("outgoing_call/$callId/$userId/video")
                        }
                    )
                }
            },
            onDismiss = { showCallDialog = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Spacer(Modifier.height(14.dp))

        // ✅ Bouton ouvre le dialog
        Button(
            onClick  = { showCallDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = DGreen)
        ) {
            Icon(Icons.Default.Call, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Appeler $userName", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(Modifier.height(14.dp))

        if (calls.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("✅ ${calls.count { it.status == "accepted" }}", DGreen, Modifier.weight(1f))
                StatChip("❌ ${calls.count { it.status == "rejected" }}", DRedAlert, Modifier.weight(1f))
                StatChip("⏳ ${calls.count { it.status == "pending"  }}", DOrange,   Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text("${calls.size} appel(s) avec $userName", fontSize = 12.sp, color = DTextSub)
            Spacer(Modifier.height(10.dp))
        }

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = DBlue) }
            calls.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📞", fontSize = 40.sp); Spacer(Modifier.height(8.dp))
                    Text("Aucun appel avec cette personne", color = DTextSub)
                }
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(calls) { call ->
                    val isOutgoing = call.from == currentUid
                    var now by remember { mutableStateOf(System.currentTimeMillis()) }
                    LaunchedEffect(call.id) { while (true) { delay(1000L); now = System.currentTimeMillis() } }
                    val displayStatus = when {
                        call.status == "accepted" -> "accepted"
                        call.status == "rejected" -> "rejected"
                        call.status == "pending" && (now - call.timestamp) > 60_000L -> "missed"
                        else -> call.status
                    }
                    val (statusIcon, statusColor, statusLabel) = when (displayStatus) {
                        "accepted" -> Triple("✅", DGreen,    "Accepté")
                        "rejected" -> Triple("❌", DRedAlert, "Rejeté")
                        "missed"   -> Triple("📵", DTextSub,  "Manqué")
                        else       -> Triple("⏳", DOrange,   "En attente")
                    }
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DCard), elevation = CardDefaults.cardElevation(3.dp)) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                                .background(if (isOutgoing) DBlueSoft else Color(0xFFF3E5F5)), contentAlignment = Alignment.Center) {
                                Icon(if (isOutgoing) Icons.Default.CallMade else Icons.Default.CallReceived,
                                    contentDescription = null, tint = if (isOutgoing) DBlue else Color(0xFF7B1FA2),
                                    modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (isOutgoing) "Appel sortant" else "Appel entrant",
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DTextMain)
                                Text(dFormatTime(call.timestamp), fontSize = 12.sp, color = DTextSub)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(statusIcon, fontSize = 16.sp)
                                Text(statusLabel, fontSize = 11.sp, color = statusColor, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.1f)).padding(vertical = 8.dp),
        contentAlignment = Alignment.Center) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

private fun dFormatTime(ts: Long): String {
    if (ts == 0L) return "—"
    return SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault()).format(Date(ts))
}