package com.example.ai_guardian.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
                    "settings" -> SettingsScreen(viewModel)
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
        "normal" -> Color(0xFFF59E0B)
        else -> Color(0xFF6366F1)

    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(Modifier.padding(16.dp)) {

            // ── Type + اسم المستخدم ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (alert.type) {
                        "danger" -> Icons.Default.Error
                        "normal" -> Icons.Default.Warning
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = color
                )
                Spacer(Modifier.width(8.dp))
                Text(alert.type.uppercase(), fontWeight = FontWeight.Bold, color = color)

                Spacer(Modifier.weight(1f))

                // 👤 اسم المستخدم
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = alert.superviseeName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.DarkGray
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Message ──
            Text(alert.message, fontSize = 15.sp)

            Spacer(Modifier.height(8.dp))

            // ── Timestamp ──
            Text(formatTimestamp(alert.timestamp), fontSize = 12.sp, color = Color.Gray)
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

// ================= STATS SCREEN =================

@Composable
fun StatsScreen(stats: Stats) {

    if (stats.total == 0) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.BarChart,
                    contentDescription = null,
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("No data yet", fontSize = 18.sp, color = Color(0xFF94A3B8))
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())  // ✅ Scroll fix
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Title ──
        Text(
            "📊 Dashboard",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )

        // ── Stats Cards ──
        StatsCardsRow(stats)

        // ── Pie Chart ──
        SectionCard(title = "👥 Users by Role") {
            UsersPieChart(stats)
        }

        // ── Bar Chart ──
        SectionCard(title = "📊 Users Status") {
            UsersBarChart(stats)
        }

        // ── Line Chart ──
        SectionCard(title = "🚨 Alerts Overview") {
            AlertsLineChart(stats)
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Section card wrapper ──────────────────────────────────────────────────────
@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF374151)
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

// ── Stats Cards Row ───────────────────────────────────────────────────────────
@Composable
fun StatsCardsRow(stats: Stats) {
    // ✅ 2x2 grid بدل row واحد باش ما يكونوش صغار
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Total Users",
                value = stats.total.toString(),
                icon = Icons.Default.Group,
                color = Color(0xFF6366F1)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Admins",
                value = stats.admins.toString(),
                icon = Icons.Default.AdminPanelSettings,
                color = Color(0xFFEF4444)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Superviseurs",
                value = stats.superviseurs.toString(),
                icon = Icons.Default.SupervisorAccount,
                color = Color(0xFF3B82F6)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Surveillés",
                value = stats.surveilles.toString(),
                icon = Icons.Default.RemoveRedEye,
                color = Color(0xFF22C55E)
            )
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF111827))
                Text(title, fontSize = 11.sp, color = Color(0xFF6B7280))
            }
        }
    }
}

// ── Pie Chart ─────────────────────────────────────────────────────────────────
@Composable
fun UsersPieChart(stats: Stats) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        factory = { context ->
            PieChart(context).apply {
                description.isEnabled = false
                setUsePercentValues(true)
                legend.isEnabled = true
                setEntryLabelColor(android.graphics.Color.BLACK)
                setEntryLabelTextSize(11f)
                centerText = "${stats.total}\nUsers"
                setCenterTextSize(14f)
                isDrawHoleEnabled = true
                holeRadius = 40f
            }
        },
        // ✅ update بدل factory — performance أحسن
        update = { chart ->
            val entries = listOf(
                PieEntry(stats.admins.toFloat(), "Admins"),
                PieEntry(stats.superviseurs.toFloat(), "Superviseurs"),
                PieEntry(stats.surveilles.toFloat(), "Surveillés")
            )
            val dataSet = PieDataSet(entries, "").apply {
                colors = listOf(
                    android.graphics.Color.parseColor("#EF4444"),
                    android.graphics.Color.parseColor("#3B82F6"),
                    android.graphics.Color.parseColor("#22C55E")
                )
                valueTextColor = android.graphics.Color.WHITE
                valueTextSize = 12f
                sliceSpace = 3f
            }
            chart.data = PieData(dataSet)
            chart.centerText = "${stats.total}\nUsers"
            chart.animateY(800)
            chart.invalidate()
        }
    )
}

// ── Bar Chart ─────────────────────────────────────────────────────────────────
@Composable
fun UsersBarChart(stats: Stats) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        factory = { context ->
            com.github.mikephil.charting.charts.BarChart(context).apply {
                description.isEnabled = false
                axisRight.isEnabled = false
                axisLeft.axisMinimum = 0f
                xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
                xAxis.setDrawGridLines(false)
                legend.isEnabled = false
                setFitBars(true)
            }
        },
        update = { chart ->
            val entries = listOf(
                com.github.mikephil.charting.data.BarEntry(0f, stats.active.toFloat()),
                com.github.mikephil.charting.data.BarEntry(1f, stats.blocked.toFloat()),
                com.github.mikephil.charting.data.BarEntry(2f, stats.inactive.toFloat())
            )
            val dataSet = com.github.mikephil.charting.data.BarDataSet(entries, "Status").apply {
                colors = listOf(
                    android.graphics.Color.parseColor("#22C55E"),  // Active
                    android.graphics.Color.parseColor("#EF4444"),  // Blocked
                    android.graphics.Color.parseColor("#94A3B8")   // Inactive
                )
                valueTextSize = 12f
            }
            val labels = listOf("Active", "Blocked", "Inactive")
            chart.xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float) = labels.getOrNull(value.toInt()) ?: ""
            }
            chart.data = com.github.mikephil.charting.data.BarData(dataSet)
            chart.animateY(1000)
            chart.invalidate()
        }
    )
}

// ── Line Chart ────────────────────────────────────────────────────────────────
@Composable
fun AlertsLineChart(stats: Stats) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        factory = { context ->
            com.github.mikephil.charting.charts.LineChart(context).apply {
                description.isEnabled = false
                axisRight.isEnabled = false
                axisLeft.axisMinimum = 0f
                xAxis.setDrawGridLines(false)
            }
        },
        update = { chart ->
            val dangerSet = com.github.mikephil.charting.data.LineDataSet(
                listOf(com.github.mikephil.charting.data.Entry(0f, stats.danger.toFloat())),
                "Danger"
            ).apply {
                color = android.graphics.Color.parseColor("#EF4444")
                setCircleColor(android.graphics.Color.parseColor("#EF4444"))
                circleRadius = 6f
                lineWidth = 3f
                valueTextSize = 12f
                mode = com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER
            }

            val normalSet = com.github.mikephil.charting.data.LineDataSet(
                listOf(com.github.mikephil.charting.data.Entry(0f, stats.normal.toFloat())),
                "Normal"
            ).apply {
                color = android.graphics.Color.parseColor("#22C55E")
                setCircleColor(android.graphics.Color.parseColor("#22C55E"))
                circleRadius = 6f
                lineWidth = 3f
                valueTextSize = 12f
                mode = com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER
            }

            chart.data = com.github.mikephil.charting.data.LineData(dangerSet, normalSet)
            chart.animateX(1000)
            chart.invalidate()
        }
    )
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

// ── Colors ────────────────────────────────────────────────────────────────────
private val BgPage        = Color(0xFFF8F7FF)
private val BgCard        = Color(0xFFFFFFFF)
private val BgCardBorder  = Color(0xFFEEEDFE)
private val BgRowBorder   = Color(0xFFF1EFE8)
private val PurplePrimary = Color(0xFF534AB7)
private val PurpleLight   = Color(0xFFEEEDFE)
private val TextDark      = Color(0xFF26215C)
private val TextMid       = Color(0xFF374151)
private val TextGray      = Color(0xFF9CA3AF)
private val GreenBg       = Color(0xFFEAF3DE)
private val GreenText     = Color(0xFF3B6D11)
private val RedBg         = Color(0xFFFCEBEB)
private val RedText       = Color(0xFFA32D2D)
private val RedBorder     = Color(0xFFF7C1C1)

// ── Screen ────────────────────────────────────────────────────────────────────
@Composable
fun SettingsScreen(viewModel: AdminViewModel) {

    // ── States ─────────────────────────────────────────────────────────────
    var maintenanceMode   by remember { mutableStateOf(false) }
    var registrationOpen  by remember { mutableStateOf(true) }
    var dangerAlerts      by remember { mutableStateOf(true) }
    var newUserNotif      by remember { mutableStateOf(false) }
    var showAddAdminDialog  by remember { mutableStateOf(false) }
    var showClearDialog     by remember { mutableStateOf(false) }
    var showResetDialog     by remember { mutableStateOf(false) }

    val admins = viewModel.users.filter { it.role == "admin" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ── Title ─────────────────────────────────────────────────────────
        Text(
            text = "⚙️ Settings",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        // ── App Configuration ─────────────────────────────────────────────
        SettingsSection(title = "App Configuration", iconEmoji = "🛡", iconBg = PurpleLight) {

            ToggleRow(
                label = "Maintenance Mode",
                desc = "Disable access for all users",
                checked = maintenanceMode,
                onCheckedChange = { maintenanceMode = it }
            )
            RowDivider()
            ToggleRow(
                label = "Registration",
                desc = "Allow new sign-ups",
                checked = registrationOpen,
                onCheckedChange = { registrationOpen = it }
            )
            RowDivider()
            InfoRow(label = "App Version", value = "v2.4.1", valueBg = PurpleLight, valueColor = PurplePrimary)
        }

        // ── Notifications ─────────────────────────────────────────────────
        SettingsSection(title = "Notifications", iconEmoji = "🔔", iconBg = GreenBg) {

            ToggleRow(
                label = "Danger Alerts",
                desc = "Push to admin on danger",
                checked = dangerAlerts,
                onCheckedChange = { dangerAlerts = it }
            )
            RowDivider()
            ToggleRow(
                label = "New User Signup",
                desc = "Notify when user registers",
                checked = newUserNotif,
                onCheckedChange = { newUserNotif = it }
            )
        }

        // ── Admin Accounts ────────────────────────────────────────────────
        SettingsSection(title = "Admin Accounts", iconEmoji = "👥", iconBg = Color(0xFFFAECE7)) {

            admins.forEach { admin ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PurpleLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = admin.nom.take(2).uppercase(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimary
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(admin.nom, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                        Text(admin.email, fontSize = 11.sp, color = TextGray)
                    }
                    // Badge
                    val blocked = admin.blocked
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (blocked) RedBg else GreenBg)
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (blocked) "Blocked" else "Active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (blocked) RedText else GreenText
                        )
                    }
                }
                RowDivider()
            }

            // Add admin button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add a new admin", fontSize = 13.sp, color = TextGray)
                Button(
                    onClick = { showAddAdminDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("+ Add Admin", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Danger Zone ───────────────────────────────────────────────────
        SettingsSection(title = "Danger Zone", iconEmoji = "⚠️", iconBg = RedBg, titleColor = RedText) {

            DangerRow(
                label = "Clear all alerts",
                desc = "Delete every alert from DB",
                btnText = "Clear",
                onClick = { showClearDialog = true }
            )
            RowDivider()
            DangerRow(
                label = "Reset all users",
                desc = "Remove all non-admin accounts",
                btnText = "Reset",
                onClick = { showResetDialog = true }
            )
        }

        Spacer(Modifier.height(16.dp))
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────
    if (showClearDialog) {
        ConfirmDialog(
            title = "Clear all alerts?",
            message = "This will permanently delete all alerts from the database.",
            confirmText = "Clear",
            confirmColor = RedText,
            onConfirm = {
                viewModel.clearAllAlerts()
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false }
        )
    }

    if (showResetDialog) {
        ConfirmDialog(
            title = "Reset all users?",
            message = "All non-admin accounts will be permanently deleted.",
            confirmText = "Reset",
            confirmColor = RedText,
            onConfirm = {
                viewModel.resetAllUsers()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }

    if (showAddAdminDialog) {
        AddAdminDialog(
            onConfirm = { uid ->
                viewModel.makeAdmin(uid)
                showAddAdminDialog = false
            },
            onDismiss = { showAddAdminDialog = false }
        )
    }
}

// ── Composable Helpers ────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    iconEmoji: String,
    iconBg: Color,
    titleColor: Color = TextMid,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, BgCardBorder, RoundedCornerShape(16.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFAFAFF))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Text(iconEmoji, fontSize = 14.sp)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = title.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                letterSpacing = 1.sp
            )
        }
        HorizontalDivider(color = BgRowBorder, thickness = 0.5.dp)
        content()
    }
}

@Composable
private fun ToggleRow(
    label: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
            Text(desc, fontSize = 12.sp, color = TextGray)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PurplePrimary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE5E7EB)
            )
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueBg: Color, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(valueBg)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
        }
    }
}

@Composable
private fun DangerRow(label: String, desc: String, btnText: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
            Text(desc, fontSize = 12.sp, color = TextGray)
        }
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(RedBorder)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = RedBg,
                contentColor = RedText
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(btnText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = BgRowBorder,
        thickness = 0.5.dp
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text(title, fontWeight = FontWeight.Bold, color = TextDark) },
        text = { Text(message, color = TextGray, fontSize = 14.sp) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(confirmText, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextGray)
            }
        }
    )
}

@Composable
private fun AddAdminDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var uid by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Add Admin", fontWeight = FontWeight.Bold, color = TextDark) },
        text = {
            Column {
                Text("Enter the UID of the user to promote:", fontSize = 14.sp, color = TextGray)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = uid,
                    onValueChange = { uid = it },
                    placeholder = { Text("User UID", color = TextGray) },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (uid.isNotBlank()) onConfirm(uid) },
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextGray)
            }
        }
    )
}

