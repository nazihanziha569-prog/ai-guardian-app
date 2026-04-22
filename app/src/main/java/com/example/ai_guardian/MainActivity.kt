package com.example.ai_guardian

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.*
import com.example.ai_guardian.ui.screens.DashboardScreen
import com.example.ai_guardian.ui.screens.GenerateQRScreen
import com.example.ai_guardian.ui.screens.QRScreen
import com.example.ai_guardian.ui.screens.SplashScreen
import com.example.ai_guardian.ui.screens.LoginScreen
import com.example.ai_guardian.ui.screens.WelcomeScreen
import com.example.ai_guardian.viewmodel.AuthViewModel
import com.example.ai_guardian.viewmodel.QRViewModel
import com.google.firebase.auth.FirebaseAuth
import com.example.ai_guardian.data.datasource.FirebaseDataSource
import com.example.ai_guardian.data.repository.UserRepository
import com.example.ai_guardian.ui.screens.DashboardSurveilleScreen
import com.example.ai_guardian.ui.screens.RegisterSuperviseurScreen
import com.example.ai_guardian.ui.screens.RegisterSurveilleScreen
import com.example.ai_guardian.ui.screens.RoleSelectionScreen
import com.example.ai_guardian.ui.screens.AboutScreen
import com.example.ai_guardian.ui.screens.AdminDashboardScreen
import com.example.ai_guardian.viewmodel.AlertViewModel
import com.google.firebase.firestore.FirebaseFirestore
@androidx.annotation.RequiresApi(26)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()
        setContent {

            val navController = rememberNavController()
            val qrViewModel = remember { QRViewModel() }

            val firebaseDataSource = remember { FirebaseDataSource() }
            val userRepository = remember { UserRepository(firebaseDataSource) }

            // ✅ ViewModel (instance wahda lel app kol)
            val authViewModel = remember {
                AuthViewModel(userRepository)
            }
            // ✅ auto login
            val startDestination = "splash"

            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
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

                // 🔹 Splash (optionnel)
                composable("splash") {
                    SplashScreen {

                        val uid = FirebaseAuth.getInstance().currentUser?.uid

                        if (uid != null) {

                            FirebaseFirestore.getInstance()
                                .collection("Users")
                                .document(uid)
                                .get()
                                .addOnSuccessListener { doc ->

                                    val role = doc.getString("role")
                                    if (role == "admin") {
                                        navController.navigate("admin_dashboard") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                    else if (role == "superviseur") {
                                        navController.navigate("dashboard") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    } else if (role == "surveille") {
                                        navController.navigate("dashboard_surveille") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate("welcome") {
                                            popUpTo("splash") { inclusive = true }
                                        }
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

                composable("welcome") {
                    WelcomeScreen(
                        onLoginClick = { navController.navigate("login") },
                        onRegisterClick = { navController.navigate("role") },
                        onAboutClick = { navController.navigate("about") }
                    )
                }

                // 🔹 About Screen (الجديد 🔥)
                composable("about") {
                    AboutScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("role") {
                    RoleSelectionScreen(
                        onSuperviseurClick = {
                            authViewModel.role = "superviseur" // ✅
                            navController.navigate("register_superviseur")
                        },
                        onSurveilleClick = {
                            authViewModel.role = "surveille" // ✅
                            navController.navigate("register_surveille")
                        },
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                // 🔹 Login
                composable("login") {
                    LoginScreen(
                        viewModel = authViewModel,
                        onRegisterClick = { navController.navigate("role") },
                        onLoginSuccess = { role ->

                            when (role) {
                                "admin" -> navController.navigate("admin_dashboard")

                                "superviseur" -> navController.navigate("dashboard")

                                else -> navController.navigate("dashboard_surveille")
                            }
                        },
                        onScanClick = { navController.navigate("qr_scan") }

                    )
                }

                // 🔹 Register
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
                        onSuccess = {
                            navController.navigate("qr_scan") // 👈 مهم
                        },
                        onCancel = {
                            navController.navigate("welcome") {
                                popUpTo("role") { inclusive = true }
                            }
                        }
                    )
                }
                // 🔹 Dashboard
                composable("dashboard") {
                    DashboardScreen(
                        authViewModel = authViewModel,
                        onQrClick = { navController.navigate("qr") },
                        onLogoutClick = {
                            navController.navigate("login") {
                                popUpTo("dashboard") { inclusive = true }
                            }
                        }
                    )}

                composable("dashboard_surveille") {
                    val alertViewModel = remember { AlertViewModel() }

                    DashboardSurveilleScreen(
                        alertViewModel = alertViewModel,
                        onLogoutClick = {
                            navController.navigate("login") {
                                popUpTo("dashboard_surveille") { inclusive = true }
                            }
                        }
                    )
                }

                // 🔹 Generate QR
                composable("qr") {
                    GenerateQRScreen(
                        qrViewModel = qrViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                // 🔹 Scan QR
                composable("qr_scan") {
                    QRScreen(
                        qrViewModel = qrViewModel,
                        onBack = { navController.popBackStack() },
                        onSuccessNavigate = {
                            navController.navigate("dashboard_surveille") {
                                popUpTo("qr_scan") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun createNotificationChannel() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                "alerts_channel",
                "Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
   }
