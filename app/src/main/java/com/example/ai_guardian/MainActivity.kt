package com.example.ai_guardian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.*
import com.example.ai_guardian.ui.screens.*
import com.example.ai_guardian.viewmodel.*
import com.google.firebase.auth.FirebaseAuth
import com.example.ai_guardian.data.datasource.FirebaseDataSource
import com.example.ai_guardian.data.repository.UserRepository

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {

            val navController = rememberNavController()
            val qrViewModel = remember { QRViewModel() }

            val firebaseDataSource = remember { FirebaseDataSource() }
            val userRepository = remember { UserRepository(firebaseDataSource) }

            val authViewModel = remember {
                AuthViewModel(userRepository)
            }

            val startDestination = "splash"

            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {

                // 🔹 Splash
                composable("splash") {
                    SplashScreen {
                        if (FirebaseAuth.getInstance().currentUser != null) {
                            navController.navigate("dashboard") {
                                popUpTo("splash") { inclusive = true }
                            }
                        } else {
                            navController.navigate("welcome") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    }
                }

                // 🔹 Welcome
                composable("welcome") {
                    WelcomeScreen(
                        onLoginClick = { navController.navigate("login") },
                        onRegisterClick = { navController.navigate("role") },
                        onAboutClick = { navController.navigate("about") } // 👈 مهم
                    )
                }

                // 🔹 About Screen (الجديد 🔥)
                composable("about") {
                    AboutScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                // 🔹 Role Selection
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
                            if (role == "superviseur") {
                                navController.navigate("dashboard")
                            } else {
                                navController.navigate("dashboard_surveille")
                            }
                        },
                        onScanClick = { navController.navigate("qr_scan") }
                    )
                }

                // 🔹 Register Superviseur
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

                // 🔹 Register Surveillé
                composable("register_surveille") {
                    RegisterSurveilleScreen(
                        authViewModel = authViewModel,
                        onSuccess = {
                            navController.navigate("qr_scan")
                        },
                        onCancel = {
                            navController.navigate("welcome") {
                                popUpTo("role") { inclusive = true }
                            }
                        }
                    )
                }

                // 🔹 Dashboard Superviseur
                composable("dashboard") {
                    DashboardScreen(
                        authViewModel = authViewModel,
                        onQrClick = { navController.navigate("qr") },
                        onLogoutClick = {
                            navController.navigate("login") {
                                popUpTo("dashboard") { inclusive = true }
                            }
                        }
                    )
                }

                // 🔹 Dashboard Surveillé
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
}