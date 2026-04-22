package com.example.ai_guardian.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_guardian.data.model.Alert
import com.example.ai_guardian.data.model.User
import com.example.ai_guardian.viewmodel.AdminViewModel
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ai_guardian.R
import com.example.ai_guardian.data.model.Stats
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onLogoutClick: () -> Unit
) {
    val viewModel: AdminViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    var selectedTab by remember { mutableStateOf("users") }

    Scaffold(

        // ✅ هنا نحط TopBar
        topBar = {
            AdminTopBar(
                onLogout = onLogoutClick,
                onRefresh = {
                    // 🔄 تنجم تزيد reload (اختياري)
                }
            )
        }

    ) { padding ->

        Row(
            Modifier
                .fillMaxSize()
                .padding(padding) // 🔥 مهم باش ما يغطّيش الـ TopBar
        ) {

            // ================= SIDEBAR =================
            Column(
                modifier = Modifier
                    .width(140.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF111827)),
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                Column(Modifier.padding(12.dp)) {

                    Text(
                        "AI GUARDIAN",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Spacer(Modifier.height(20.dp))

                    SidebarButton("Users", Icons.Default.Person, selectedTab == "users") {
                        selectedTab = "users"
                    }

                    SidebarButton("Stats", Icons.Default.BarChart, selectedTab == "stats") {
                        selectedTab = "stats"
                    }

                    SidebarButton("Alerts", Icons.Default.Warning, selectedTab == "alerts") {
                        selectedTab = "alerts"
                    }

                    SidebarButton("Settings", Icons.Default.Settings, selectedTab == "settings") {
                        selectedTab = "settings"
                    }
                }

                SidebarButton("Logout", Icons.Default.Logout, false, onClick = onLogoutClick)
            }

            // ================= CONTENT =================
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF3F4F6))
                    .padding(16.dp)
            ) {

                when (selectedTab) {
                    "users" -> UsersScreen(viewModel)
                    "stats" -> StatsScreen(viewModel.stats)
                    "alerts" -> AlertsScreen(viewModel)
                    "settings" -> Text("⚙️ Settings coming soon")
                }
            }
        }
    }
}
@Composable
fun AlertsScreen(viewModel: AdminViewModel) {

    val alerts = viewModel.alerts // لازمك list من ViewModel

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            "🚨 Alerts",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        if (alerts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No alerts yet 😴")
            }
        } else {

            LazyColumn {
                items(alerts) { alert ->
                    AlertCard(alert)
                }
            }
        }
    }
}
@Composable
fun AlertCard(alert: Alert) {

    val color = when (alert.type) {
        "danger" -> Color.Red
        "warning" -> Color(0xFFF59E0B)
        else -> Color(0xFF6366F1)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        )
    ) {

        Column(Modifier.padding(16.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = when (alert.type) {
                        "danger" -> Icons.Default.Error
                        "warning" -> Icons.Default.Warning
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = color
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    alert.type.uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                alert.message,
                fontSize = 15.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                formatTimestamp(alert.timestamp),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}
fun formatTimestamp(time: Long): String {
    val sdf = java.text.SimpleDateFormat("dd MMM yyyy - HH:mm")
    return sdf.format(java.util.Date(time))
}
@Composable
fun SidebarButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) Color(0xFF6366F1) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(10.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White)
        Spacer(Modifier.width(10.dp))
        Text(text, color = Color.White)
    }
}

// ================= USERS =================

@Composable
fun UsersScreen(viewModel: AdminViewModel) {

    Column {

        // HEADER
        Text(
            "Users Management",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        LazyRow {
            items(listOf("all", "admin", "superviseur", "surveille")) { role ->
                FilterChip(
                    selected = viewModel.selectedRole == role,
                    onClick = { viewModel.selectedRole = role },
                    label = { Text(role) }
                )
                Spacer(Modifier.width(8.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn {
            items(viewModel.filteredUsers) { user ->
                UserCard(
                    user,
                    onDelete = { viewModel.deleteUser(user.uid) },
                    onBlock = { viewModel.toggleBlock(user) }
                )
            }
        }
    }
}

// ================= USER CARD =================
@Composable
fun UserCard(
    user: User,
    onDelete: () -> Unit,
    onBlock: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {

        Column {

            // 🔹 HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color(0xFF6366F1), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        user.nom.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(user.nom, fontWeight = FontWeight.Bold)
                    Text(
                        user.email,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            // 🔥 DETAILS
            AnimatedVisibility(visible = expanded) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {

                    Divider()

                    Spacer(Modifier.height(10.dp))

                    // 👤 Infos
                    InfoRow("Name", user.nom)
                    InfoRow("Email", user.email)
                    InfoRow("Role", user.role)

                    // 🔥 Age (important)
                    InfoRow("Age", user.age.toString())

                    // 🔒 Status
                    InfoRow(
                        "Status",
                        if (user.blocked) "Blocked 🚫" else "Active ✅"
                    )



                    Spacer(Modifier.height(12.dp))

                    // 🔘 ACTIONS
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        Button(onClick = onBlock) {
                            Text(if (user.blocked) "Unblock" else "Block")
                        }

                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

// ================= STATS =================

@Composable
fun StatsScreen(stats: Stats) {

    if (stats.total == 0) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No data yet 🚫")
        }
        return
    }

    Column(Modifier.fillMaxSize()) {

        Text(
            "📊 AI Guardian Dashboard",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        // 🔢 CARDS
        StatsCardsRow(stats)

        Spacer(Modifier.height(16.dp))

        // 🥧 PIE
        Card(Modifier.fillMaxWidth().height(250.dp)) {
            UsersPieChart(stats)
        }

        Spacer(Modifier.height(16.dp))

        // 📊 BAR
       Card(Modifier.fillMaxWidth().height(250.dp)) {
           UsersBarChart(stats)
        }

        Spacer(Modifier.height(16.dp))

        // 📈 LINE (REALISTIC)
        Card(Modifier.fillMaxWidth().height(250.dp)) {
            AlertsLineChart(stats)
        }
    }
}
@Composable
fun UsersPieChart(stats: Stats) {

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        factory = { context ->

            val chart = PieChart(context)

            val entries = listOf(
                PieEntry(stats.admins.toFloat(), "Admins"),
                PieEntry(stats.superviseurs.toFloat(), "Superviseurs"),
                PieEntry(stats.surveilles.toFloat(), "Surveillés")
            )
            val dataSet = PieDataSet(entries, "Users")


            // 🔥 IMPORTANT: colors
            dataSet.colors = listOf(
                android.graphics.Color.parseColor("#EF4444"), // red
                android.graphics.Color.parseColor("#3B82F6"), // blue
                android.graphics.Color.parseColor("#22C55E")  // green
            )

            dataSet.valueTextColor = android.graphics.Color.BLACK
            dataSet.valueTextSize = 12f

            val data = PieData(dataSet)

            chart.data = data

            // 🔥 FIX refresh
            chart.invalidate()
            chart.notifyDataSetChanged()

            chart.description.isEnabled = false
            chart.setUsePercentValues(false)
            chart.centerText = "${stats.total} Users"
            chart.setEntryLabelColor(android.graphics.Color.BLACK)
            chart.legend.isEnabled = true

            chart
        }
    )
}
@Composable
fun AlertsLineChart(stats: Stats) {

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        factory = { context ->

            val chart = com.github.mikephil.charting.charts.LineChart(context)

            // 🔴 Danger line
            val dangerEntries = listOf(
                com.github.mikephil.charting.data.Entry(0f, stats.danger.toFloat())
            )

            val dangerSet = com.github.mikephil.charting.data.LineDataSet(
                dangerEntries,
                "Danger"
            )

            dangerSet.color = android.graphics.Color.RED
            dangerSet.setCircleColor(android.graphics.Color.RED)
            dangerSet.circleRadius = 6f
            dangerSet.lineWidth = 3f

            // 🟢 Normal line
            val normalEntries = listOf(
                com.github.mikephil.charting.data.Entry(0f, stats.normal.toFloat())
            )

            val normalSet = com.github.mikephil.charting.data.LineDataSet(
                normalEntries,
                "Normal"
            )

            normalSet.color = android.graphics.Color.GREEN
            normalSet.setCircleColor(android.graphics.Color.GREEN)
            normalSet.circleRadius = 6f
            normalSet.lineWidth = 3f

            // 📊 combine both lines
            val data = com.github.mikephil.charting.data.LineData(dangerSet, normalSet)

            chart.data = data

            chart.description.isEnabled = false
            chart.axisRight.isEnabled = false
            chart.axisLeft.axisMinimum = 0f
            chart.animateX(1000)

            chart.invalidate()
            chart
        }
    )
}
@Composable
fun UsersBarChart(stats: Stats) {

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        factory = { context ->

            val chart = com.github.mikephil.charting.charts.BarChart(context)

            val entries = listOf(
                com.github.mikephil.charting.data.BarEntry(0f, stats.active.toFloat()),
                com.github.mikephil.charting.data.BarEntry(1f, stats.blocked.toFloat()),
                com.github.mikephil.charting.data.BarEntry(3f, stats.inactive.toFloat())
            )

            val dataSet = com.github.mikephil.charting.data.BarDataSet(entries, "Users Status")

            dataSet.colors = listOf(
                android.graphics.Color.GREEN,   // Active
                android.graphics.Color.RED,     // Blocked
                android.graphics.Color.GRAY     // Inactive
            )

            chart.data = com.github.mikephil.charting.data.BarData(dataSet)

            // 📌 labels
            val labels = listOf("Active", "Blocked", "Inactive")

            chart.xAxis.valueFormatter =
                object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return labels.getOrNull(value.toInt()) ?: ""
                    }
                }

            chart.xAxis.position =
                com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM

            chart.axisRight.isEnabled = false
            chart.axisLeft.axisMinimum = 0f

            chart.description.isEnabled = false
            chart.animateY(1200)

            chart.invalidate()

            chart
        }
    )
}
@Composable
fun StatsCardsRow(stats: Stats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard("Users", stats.total.toString(), Icons.Default.Group)
        StatCard("Admins", stats.admins.toString(), Icons.Default.AdminPanelSettings)
        StatCard("Superviseur", stats.superviseurs.toString(), Icons.Default.SupervisorAccount)
        StatCard("Surveillé", stats.surveilles.toString(), Icons.Default.RemoveRedEye)
    }
}
@Composable
fun StatCard(title: String, value: String, icon: ImageVector) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF6366F1))
            Spacer(Modifier.height(6.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(title, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
@Composable
fun InfoRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(value)
    }
}
@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun AdminTopBar(
    onLogout: () -> Unit,
    onRefresh: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF111827),
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = "logo",
                    modifier = Modifier.size(36.dp) // 🔥 صغّرها شوية باش تناسب الـ TopBar
                )
                Spacer(Modifier.width(8.dp))
                Text("AI GUARDIAN", fontWeight = FontWeight.Bold)
            }
        },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = null)
            }
            IconButton(onClick = onLogout) {
                Icon(Icons.Default.Logout, contentDescription = null)
            }
        }
    )
}
