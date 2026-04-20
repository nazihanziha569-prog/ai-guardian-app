package com.example.ai_guardian.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_guardian.R
import com.example.ai_guardian.viewmodel.QRViewModel
import com.google.zxing.integration.android.IntentIntegrator

@Composable
fun QRScreen(
    qrViewModel: QRViewModel,
    onBack: () -> Unit,
    onSuccessNavigate: () -> Unit
) {

    val context = LocalContext.current
    val activity = context as Activity

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->

        val intentResult = IntentIntegrator.parseActivityResult(
            result.resultCode,
            result.data
        )

        if (intentResult != null && intentResult.contents != null) {

            val scannedData = intentResult.contents
            qrViewModel.onQrScanned(scannedData)

            Toast.makeText(context, "QR Scanné ✅", Toast.LENGTH_SHORT).show()

        } else {
            Toast.makeText(context, "Scan annulé ❌", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA)) // 🌤 نفس dashboard
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(30.dp))

        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "logo",
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Scanner QR",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2)
        )

        Text(
            text = "Scannez le QR du superviseur",
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 🔥 CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {

            Column(modifier = Modifier.padding(20.dp)) {

                Button(
                    onClick = {
                        val integrator = IntentIntegrator(activity)
                        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                        integrator.setPrompt("Scanner le QR")
                        integrator.setBeepEnabled(true)
                        integrator.setOrientationLocked(false)

                        launcher.launch(integrator.createScanIntent())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    )
                ) {
                    Text("Scanner QR", color = Color.White)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 🔥 RESULT
                if (qrViewModel.qrResult.isNotEmpty()) {
                    Text(
                        text = "Résultat:",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = qrViewModel.qrResult,
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 🔥 ACTIONS
        Button(
            onClick = {
                qrViewModel.createAssociation(
                    onSuccess = {
                        Toast.makeText(context, "Lien réussi ✅", Toast.LENGTH_SHORT).show()
                        onSuccessNavigate()
                    },
                    onError = { errorMessage: String ->
                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = qrViewModel.qrResult.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1976D2)
            )
        ) {
            Text("Confirmer le lien", color = Color.White)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Retour",
            color = Color.Gray,
            modifier = Modifier
                .clickable { onBack() }
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}