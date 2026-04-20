package com.example.ai_guardian.ui.screens

import android.widget.Toast

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff

import androidx.compose.material3.*

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.ai_guardian.R
import com.example.ai_guardian.viewmodel.AuthViewModel
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onRegisterClick: () -> Unit,
    onLoginSuccess: (String) -> Unit,
    onScanClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA)) // نفس Dashboard
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(60.dp))

        // 🔵 LOGO + TITLE
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "logo",
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "AI Guardian",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2)
        )

        Spacer(modifier = Modifier.height(30.dp))

        // 🟦 CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    text = "Se connecter",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = viewModel.email,
                    onValueChange = { viewModel.email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = viewModel.password,
                    onValueChange = { viewModel.password = it },
                    label = { Text("Mot de passe") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (viewModel.passwordVisible)
                        VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image = if (viewModel.passwordVisible)
                            Icons.Default.Visibility
                        else
                            Icons.Default.VisibilityOff

                        IconButton(onClick = {
                            viewModel.togglePasswordVisibility()
                        }) {
                            Icon(imageVector = image, contentDescription = "Toggle Password")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Mot de passe oublié ?",
                    color = Color(0xFF1976D2),
                    modifier = Modifier.align(Alignment.End)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 🔵 BUTTON
                Button(
                    onClick = {
                        viewModel.login(
                            onSuccess = { role -> onLoginSuccess(role) },
                            onError = {
                                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Se connecter", color = Color.White)
                }

                Spacer(modifier = Modifier.height(15.dp))

                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Pas de compte ? ")
                    Text(
                        text = "Créer",
                        color = Color(0xFF1976D2),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onRegisterClick() }
                    )
                }
            }
        }
    }
}