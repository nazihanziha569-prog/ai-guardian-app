package com.example.ai_guardian

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.navigation.compose.*
import com.example.ai_guardian.audio.AlarmHolder
import com.example.ai_guardian.audio.AlarmSoundManager
import com.example.ai_guardian.data.datasource.FirebaseDataSource
import com.example.ai_guardian.data.repository.UserRepository
import com.example.ai_guardian.ui.screens.AboutScreen
import com.example.ai_guardian.ui.screens.ActiveCallScreen
import com.example.ai_guardian.ui.screens.AdminDashboardScreen
import com.example.ai_guardian.ui.screens.AlarmScreen
import com.example.ai_guardian.ui.screens.AudioCallScreen
import com.example.ai_guardian.ui.screens.DashboardScreen
import com.example.ai_guardian.ui.screens.DashboardSurveilleScreen
import com.example.ai_guardian.ui.screens.DetailsScreen
import com.example.ai_guardian.ui.screens.GenerateQRScreen
import com.example.ai_guardian.ui.screens.IncomingCallScreen
import com.example.ai_guardian.ui.screens.LoginScreen
import com.example.ai_guardian.ui.screens.OutgoingCallScreen
import com.example.ai_guardian.ui.screens.ProfileScreen
import com.example.ai_guardian.ui.screens.QRScreen
import com.example.ai_guardian.ui.screens.RappelScreen
import com.example.ai_guardian.ui.screens.RegisterSuperviseurScreen
import com.example.ai_guardian.ui.screens.RegisterSurveilleScreen
import com.example.ai_guardian.ui.screens.RoleSelectionScreen
import com.example.ai_guardian.ui.screens.SplashScreen
import com.example.ai_guardian.ui.screens.VideoCallScreen
import com.example.ai_guardian.ui.screens.WelcomeScreen
import com.example.ai_guardian.viewmodel.AlertViewModel
import com.example.ai_guardian.viewmodel.AuthViewModel
import com.example.ai_guardian.viewmodel.CallViewModel
import com.example.ai_guardian.viewmodel.QRViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import livekit.org.webrtc.EglBase




@androidx.annotation.RequiresApi(26)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        createNotificationChannel()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        AlarmHolder.soundManager = AlarmSoundManager(this)

        setContent {

            val qrViewModel = remember { QRViewModel() }
            val firebaseDataSource = remember { FirebaseDataSource() }
            val userRepository = remember { UserRepository(firebaseDataSource) }
            val authViewModel = remember { AuthViewModel(userRepository) }
            val callVM = remember { CallViewModel() }          // ✅ ajouté
            val eglBase = remember { EglBase.create() }

            val navController = rememberNavController()

            // ✅ FIX: lire alarm_message depuis l'intent au démarrage
            val alarmMessage = remember {
                mutableStateOf<String?>(intent.getStringExtra("alarm_message"))
            }

            // ✅ FIX: naviguer vers "alarm/{message}" (pas "alarm_screen/...")
            LaunchedEffect(alarmMessage.value) {
                alarmMessage.value?.let { msg ->
                    // Encoder le message pour éviter les / dans la route
                    val encoded = java.net.URLEncoder.encode(msg, "UTF-8")
                    navController.navigate("alarm/$encoded")
                    alarmMessage.value = null  // consommer une seule fois
                }
            }
            DisposableEffect(Unit) {
                onDispose { eglBase.release() }
            }

            NavHost(
                navController = navController,
                startDestination = "splash"
            ) {

                // ── Splash ──────────────────────────────────────────────────
                composable("splash") {
                    SplashScreen {
                        val uid = FirebaseAuth.getInstance().currentUser?.uid

                        if (uid != null) {
                            FirebaseFirestore.getInstance()
                                .collection("Users")
                                .document(uid)
                                .get()
                                .addOnSuccessListener { doc ->
                                    val role = doc.getString("role") ?: ""
                                    val dest = when (role) {
                                        "admin"       -> "admin_dashboard"
                                        "superviseur" -> "dashboard"
                                        "surveille"   -> "dashboard_surveille"
                                        else          -> "welcome"
                                    }
                                    navController.navigate(dest) {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                                .addOnFailureListener {
                                    navController.navigate("welcome") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                        } else {
                            navController.navigate("welcome") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    }
                }

                // ── Welcome ──────────────────────────────────────────────────
                composable("welcome") {
                    WelcomeScreen(
                        onLoginClick    = { navController.navigate("login") },
                        onRegisterClick = { navController.navigate("role") },
                        onAboutClick    = { navController.navigate("about") }
                    )
                }

                // ── About ─────────────────────────────────────────────────────
                composable("about") {
                    AboutScreen(onBack = { navController.popBackStack() })
                }

                // ── Role ──────────────────────────────────────────────────────
                composable("role") {
                    RoleSelectionScreen(
                        onSuperviseurClick = {
                            authViewModel.role = "superviseur"
                            navController.navigate("register_superviseur")
                        },
                        onSurveilleClick = {
                            authViewModel.role = "surveille"
                            navController.navigate("register_surveille")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // ── Login ─────────────────────────────────────────────────────
                composable("login") {
                    LoginScreen(
                        viewModel       = authViewModel,
                        onRegisterClick = { navController.navigate("role") },
                        onLoginSuccess  = { role ->
                            when (role) {
                                "admin"       -> navController.navigate("admin_dashboard") {
                                    popUpTo("login") { inclusive = true }
                                }
                                "superviseur" -> navController.navigate("dashboard") {
                                    popUpTo("login") { inclusive = true }
                                }
                                else          -> navController.navigate("dashboard_surveille") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        },
                        onScanClick = { navController.navigate("qr_scan") }
                    )
                }

                // ── Register ──────────────────────────────────────────────────
                composable("register_superviseur") {
                    RegisterSuperviseurScreen(
                        authViewModel = authViewModel,
                        onSuccess = {
                            navController.navigate("dashboard") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        },
                        onCancel = {
                            navController.navigate("welcome") {
                                popUpTo("role") { inclusive = true }
                            }
                        }
                    )
                }

                composable("register_surveille") {
                    RegisterSurveilleScreen(
                        authViewModel = authViewModel,
                        onSuccess = { navController.navigate("qr_scan") },
                        onCancel = {
                            navController.navigate("welcome") {
                                popUpTo("role") { inclusive = true }
                            }
                        }
                    )
                }

                // ── Dashboards ────────────────────────────────────────────────
                composable("admin_dashboard") {
                    AdminDashboardScreen(
                        onLogoutClick = {
                            FirebaseAuth.getInstance().signOut()
                            navController.navigate("login") {
                                popUpTo("admin_dashboard") { inclusive = true }
                            }
                        }
                    )
                }

                composable("dashboard") {
                    DashboardScreen(
                        authViewModel = authViewModel,
                        navController = navController,
                        callVM        = callVM,
                        eglBase       = eglBase,
                        onQrClick     = { navController.navigate("qr") },
                        onLogoutClick = { navController.navigate("login") { popUpTo("dashboard") { inclusive = true } } }
                    )
                }

                composable("dashboard_surveille") {
                    val alertViewModel = remember { AlertViewModel() }
                    DashboardSurveilleScreen(
                        alertViewModel = alertViewModel,
                        navController  = navController,
                        callVM = callVM,
                        eglBase = eglBase,

                        onLogoutClick  = {
                            navController.navigate("login") {
                                popUpTo("dashboard_surveille") { inclusive = true }
                            }
                        }
                    )
                }

                // ── Détails surveillé ─────────────────────────────────────────
                composable("details/{userId}") { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId") ?: ""
                    DetailsScreen(
                        userId        = userId,
                        navController = navController,
                        callVM        = callVM,    // ✅ maintenant accessible
                        eglBase       = eglBase    // ✅ maintenant accessible
                    )
                }

                // ── QR ────────────────────────────────────────────────────────
                composable("qr") {
                    GenerateQRScreen(
                        qrViewModel = qrViewModel,
                        onBack      = { navController.popBackStack() }
                    )
                }

                composable("qr_scan") {
                    QRScreen(
                        qrViewModel        = qrViewModel,
                        onBack             = { navController.popBackStack() },
                        onSuccessNavigate  = {
                            navController.navigate("dashboard_surveille") {
                                popUpTo("qr_scan") { inclusive = true }
                            }
                        }
                    )
                }

                // ── Rappel ────────────────────────────────────────────────────
                composable("rappel") {
                    RappelScreen()
                }

                // ── Profile ───────────────────────────────────────────────────
                composable("profile") {
                    ProfileScreen(
                        viewModel     = authViewModel,
                        onBack        = { navController.popBackStack() },
                        onLogoutClick = {
                            FirebaseAuth.getInstance().signOut()
                            navController.navigate("login") {
                                popUpTo("dashboard") { inclusive = true }
                            }
                        }
                    )
                }

                // ── Alarm ─────────────────────────────────────────────────────
                // ✅ FIX: route unifiée "alarm/{message}" (supprime "alarm_screen/...")
                composable("alarm/{message}") { backStack ->
                    val msg = backStack.arguments?.getString("message")
                        ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
                        ?: ""
                    AlarmScreen(
                        message = msg,
                        onStop  = { navController.popBackStack() }
                    )
                }

                // ── Appel entrant ─────────────────────────────────────────────
                // ✅ OutgoingCallScreen — avec callType
                composable("outgoing_call/{callId}/{toUserId}/{callType}") { back ->
                    val callId   = back.arguments?.getString("callId")   ?: ""
                    val toUserId = back.arguments?.getString("toUserId") ?: ""
                    val callType = back.arguments?.getString("callType") ?: "video"
                    OutgoingCallScreen(
                        navController = navController,
                        callId        = callId,
                        toUserId      = toUserId,
                        callType      = callType,
                        callVM        = callVM,
                        eglBase       = eglBase
                    )
                }

                // ✅  écran d'appel unifié
                // active_call/{callId}/{participantName}/{isOutgoing}/{callType}
                composable("active_call/{callId}/{participantName}/{isOutgoing}/{callType}") { back ->
                    val encodedName = back.arguments?.getString("participantName") ?: ""
                    ActiveCallScreen(
                        navController   = navController,
                        callId          = back.arguments?.getString("callId") ?: "",
                        participantName = java.net.URLDecoder.decode(encodedName, "UTF-8"),
                        isOutgoing      = back.arguments?.getString("isOutgoing") == "true",
                        callType        = back.arguments?.getString("callType") ?: "video",
                        callVM          = callVM,
                        eglBase         = eglBase
                    )
                }

// ✅ VideoCallScreen
                composable("video_call/{callId}/{roomName}/{participantName}") { back ->
                    val callId          = back.arguments?.getString("callId") ?: ""
                    val roomName        = java.net.URLDecoder.decode(
                        back.arguments?.getString("roomName") ?: "offer", "UTF-8"
                    )
                    val participantName = back.arguments?.getString("participantName") ?: ""
                    VideoCallScreen(
                        navController   = navController,
                        callId          = callId,
                        roomName        = roomName,        // "offer" = appelant, "answer" = appelé
                        participantName = participantName,
                        callVM          = callVM,
                        eglBase         = eglBase
                    )
                }

// ✅ AudioCallScreen
                composable("audio_call/{callId}/{participantName}") { back ->
                    val callId          = back.arguments?.getString("callId")          ?: ""
                    val participantName = back.arguments?.getString("participantName") ?: ""
                    AudioCallScreen(
                        navController   = navController,
                        callId          = callId,
                        participantName = participantName,
                        callVM          = callVM
                    )
                }

// ✅ IncomingCallScreen — avec eglBase et callVM
                composable("incoming_call/{from}/{callId}") { back ->
                    val from   = back.arguments?.getString("from")   ?: ""
                    val callId = back.arguments?.getString("callId") ?: ""
                    IncomingCallScreen(
                        navController = navController,
                        fromUser      = from,
                        callId        = callId,
                        callVM        = callVM,
                        eglBase       = eglBase,
                        onAccept      = {},
                        onReject      = {}
                    )
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "alarm_channel",
                "Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm Notifications"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

}