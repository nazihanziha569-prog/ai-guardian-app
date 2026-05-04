package com.example.ai_guardian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_guardian.data.model.Alert
import com.example.ai_guardian.data.model.Call
import com.example.ai_guardian.ui.components.CallCard
import com.example.ai_guardian.utils.formatTime
import com.example.ai_guardian.viewmodel.CallViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

sealed class HistoryItem {
    data class AlertItem(val alert: Alert) : HistoryItem()
    data class CallItem(val call: Call)    : HistoryItem()
    val timestamp: Long get() = when (this) {
        is AlertItem -> alert.timestamp
        is CallItem  -> call.timestamp
    }
}

@Composable
fun HistoryScreen() {

    val db        = FirebaseFirestore.getInstance()
    val uid       = FirebaseAuth.getInstance().currentUser?.uid
    val callVM    = remember { CallViewModel() }

    var alerts         by remember { mutableStateOf<List<Alert>>(emptyList()) }
    var isLoading      by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("all") }

    // Alerts — Firestore ici car pas de AlertViewModel dédié encore
    LaunchedEffect(uid) {
        if (uid == null) { isLoading = false; return@LaunchedEffect }
        db.collection("Alerts")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) { isLoading = false; return@addSnapshotListener }
                alerts = snapshot.documents
                    .mapNotNull { it.toObject(Alert::class.java)?.copy(id = it.id) }
                    .filter { it.superviseurId == uid || it.superviseeId == uid }
                    .sortedByDescending { it.timestamp }
                isLoading = false
            }
    }

    // Calls — via ViewModel
    LaunchedEffect(uid) {
        uid?.let { callVM.listenCallsByUid(it) }
    }

    val allItems = (
            alerts.map { HistoryItem.AlertItem(it) } +
                    callVM.calls.map { HistoryItem.CallItem(it) }
            ).sortedByDescending { it.timestamp }

    val filtered = when (selectedFilter) {
        "danger" -> allItems.filterIsInstance<HistoryItem.AlertItem>().filter { it.alert.type == "danger" }
        "normal" -> allItems.filterIsInstance<HistoryItem.AlertItem>().filter { it.alert.type == "normal" }
        "calls"  -> allItems.filterIsInstance<HistoryItem.CallItem>()
        else     -> allItems
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Text("📜 Historique", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip("Tous",      selectedFilter == "all",    Color(0xFF1976D2)) { selectedFilter = "all"    }
            FilterChip("🚨 Danger", selectedFilter == "danger", Color(0xFFF44336)) { selectedFilter = "danger" }
            FilterChip("🟡 Normal", selectedFilter == "normal", Color(0xFFFF9800)) { selectedFilter = "normal" }
            FilterChip("📞 Appels", selectedFilter == "calls",  Color(0xFF4CAF50)) { selectedFilter = "calls"  }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("${filtered.size} événement(s)", fontSize = 13.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF1976D2))
            }
            filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✅", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Aucun événement", color = Color.Gray, fontSize = 16.sp)
                }
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered) { item ->
                    when (item) {
                        is HistoryItem.AlertItem -> HistoryCard(item.alert)
                        is HistoryItem.CallItem  -> CallCard(item.call, uid)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(alert: Alert) {
    val isDanger    = alert.type == "danger"
    val accentColor = if (isDanger) Color(0xFFF44336) else Color(0xFFFF9800)

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(5.dp).height(90.dp)
                    .background(accentColor, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(if (isDanger) "🚨 DANGER" else "🟡 NORMAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                    }
                    Text("👤 ${alert.superviseeName}", fontSize = 12.sp, color = Color(0xFF1976D2), fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(alert.message.ifBlank { "Aucun message" }, fontSize = 14.sp, color = Color(0xFF333333), fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                Text("⏱ ${formatTime(alert.timestamp)}", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun FilterChip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Surface(
        onClick        = onClick,
        shape          = RoundedCornerShape(50),
        color          = if (selected) color else Color(0xFFEEEEEE),
        tonalElevation = if (selected) 4.dp else 0.dp
    ) {
        Text(
            text       = label,
            modifier   = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            fontSize   = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color      = if (selected) Color.White else Color.DarkGray
        )
    }
}