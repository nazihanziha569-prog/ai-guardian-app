package com.example.ai_guardian.ui.screens

import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_guardian.R
import com.example.ai_guardian.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current

    var currentName by remember { mutableStateOf("") }
    var currentEmail by remember { mutableStateOf("") }

    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var visible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }


    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            kotlinx.coroutines.delay(2000)
            onLogoutClick()
        }
    }
    LaunchedEffect(Unit) {

        val uid = user?.uid ?: return@LaunchedEffect

        db.collection("Users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                name = doc.getString("nom") ?: ""
                currentName = name
            }

        email = user?.email ?: ""
        currentEmail = email
    }

    Scaffold(

        // 🔵 TOP BAR (نفس Dashboard)
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Logo",
                            modifier = Modifier.size(45.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "AI Guardian",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                ),



                actions = {
                    IconButton(onClick = { onLogoutClick() }) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = Color(0xFF1976D2)
                        )
                    }
                }
            )
        }

    ) { padding ->

        // 🔽 CONTENT تحت الـ TopBar
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding) // 👈 مهم برشة
                .background(Color(0xFFF5F7FA))
                .padding(16.dp)
        ) {

            // 🔥 TITLE تحت الـ header
            Text(
                text = "👤 Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )

            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {

                Column(modifier = Modifier.padding(20.dp)) {

                    Text("Informations", fontWeight = FontWeight.Bold)

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nom") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(20.dp))

                    Text("🔐 Change Password", fontWeight = FontWeight.Bold)

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("Ancien mot de passe") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation =
                            if (visible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { visible = !visible }) {
                                Icon(
                                    imageVector = if (visible)
                                        Icons.Default.Visibility
                                    else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        }
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Nouveau mot de passe") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation =
                            if (visible) VisualTransformation.None
                            else PasswordVisualTransformation()
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirmer mot de passe") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation =
                            if (visible) VisualTransformation.None
                            else PasswordVisualTransformation()
                    )

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = {



                            // 🔵 NAME
                            if (name.isNotBlank() && name != currentName) {
                                viewModel.updateName(name)
                            }

                            // 🔵 EMAIL
                            if (email.isNotBlank() && email != currentEmail) {

                                isLoading = true

                                viewModel.reAuth(oldPassword,
                                    onSuccess = {
                                        viewModel.updateEmail(
                                            newEmail = email,
                                            password = oldPassword,
                                            onSuccess = {
                                                isLoading = false
                                                Toast.makeText(
                                                    context,
                                                    "Email updated. Please login again.",
                                                    Toast.LENGTH_LONG
                                                ).show()

                                                onLogoutClick() // 🔥 logout direct
                                            },
                                            onError = {
                                                isLoading = false
                                                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    },
                                    onError = {
                                        Toast.makeText(context, "Wrong password", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            // 🔵 PASSWORD
                            if (newPassword.isNotBlank()) {

                                if (newPassword == confirmPassword) {

                                    viewModel.reAuth(oldPassword,
                                        onSuccess = {
                                            viewModel.changePassword(newPassword,
                                                onSuccess = {
                                                    showSuccess = true
                                                },
                                                onError = {
                                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        },
                                        onError = {
                                            Toast.makeText(context, "Wrong password", Toast.LENGTH_SHORT).show()
                                        }
                                    )

                                } else {
                                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                                }
                            }


                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }

        // 🔄 LOADING
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // 🎉 SUCCESS
        if (showSuccess) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text(
                        "✅ Updated successfully",
                        color = Color.White,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}