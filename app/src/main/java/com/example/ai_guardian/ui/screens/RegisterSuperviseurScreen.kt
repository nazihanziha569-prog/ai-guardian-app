package com.example.ai_guardian.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_guardian.viewmodel.AuthViewModel

@Composable
fun RegisterSuperviseurScreen(
    authViewModel: AuthViewModel,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {

    val context = LocalContext.current

    RegisterContent(
        authViewModel = authViewModel,
        title = "Inscription Superviseur",
        onSubmit = {
            authViewModel.role = "superviseur"

            authViewModel.register(
                onSuccess = {
                    Toast.makeText(context, "Compte créé ✅", Toast.LENGTH_SHORT).show()
                    onSuccess()
                },
                onError = {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                }
            )
        },
        onCancel = onCancel
    )
}