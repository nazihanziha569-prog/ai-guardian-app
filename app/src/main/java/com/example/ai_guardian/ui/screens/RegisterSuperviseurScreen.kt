package com.example.ai_guardian.ui.screens

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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