package com.example.ai_guardian.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
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
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 🔝 Top
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Spacer(modifier = Modifier.height(60.dp))

            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(140.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Choisissez votre rôle",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onSuperviseurClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Je suis Superviseur")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSurveilleClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Je suis Personne surveillée")
            }
        }

        // 🔽 Back
        Text(
            text = "Retour",
            color = Color.Gray,
            modifier = Modifier
                .padding(bottom = 20.dp)
                .clickable { onBack() }
        )
    }
}