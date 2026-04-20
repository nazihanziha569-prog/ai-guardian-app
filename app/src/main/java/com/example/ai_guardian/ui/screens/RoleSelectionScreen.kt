package com.example.ai_guardian.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.*

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.ai_guardian.R

@Composable
fun RoleSelectionScreen(
    onSuperviseurClick: () -> Unit,
    onSurveilleClick: () -> Unit,
    onBack: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA)) // نفس الخلفية
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 🔝 TOP
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Spacer(modifier = Modifier.height(60.dp))

            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(110.dp)
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
                        text = "Choisissez votre rôle",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onSuperviseurClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Je suis Superviseur", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(15.dp))

                    OutlinedButton(
                        onClick = onSurveilleClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Personne surveillée",
                            color = Color(0xFF1976D2)
                        )
                    }
                }
            }
        }

        // 🔽 BACK
        Text(
            text = "← Retour",
            color = Color(0xFF1976D2),
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .padding(bottom = 20.dp)
                .clickable { onBack() }
        )
    }
}