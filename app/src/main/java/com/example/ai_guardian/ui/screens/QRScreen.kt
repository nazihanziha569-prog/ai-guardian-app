package com.example.ai_guardian.ui.screens


import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
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
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val activity = context as Activity

    // 🔥 launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->

        val intentResult = IntentIntegrator.parseActivityResult(
            result.resultCode,
            result.data
        )

        if (intentResult != null && intentResult.contents != null) {

            val scannedData = intentResult.contents

            // ✅ نخزنو في ViewModel
            qrViewModel.onQrScanned(scannedData)

            Toast.makeText(context, "QR Scanné ✅", Toast.LENGTH_SHORT).show()

        } else {
            Toast.makeText(context, "Scan annulé ❌", Toast.LENGTH_SHORT).show()
        }
    }

    // 🔥 UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Spacer(modifier = Modifier.height(40.dp))

            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "logo",
                modifier = Modifier.size(120.dp)
            )

            Text(
                text = "Scanner QR",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text("Scannez le QR du superviseur")

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    val integrator = IntentIntegrator(activity)
                    integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                    integrator.setPrompt("Scanner le QR")
                    integrator.setBeepEnabled(true)
                    integrator.setOrientationLocked(false)

                    launcher.launch(integrator.createScanIntent())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scanner QR")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 🔥 عرض النتيجة
            if (qrViewModel.qrResult.isNotEmpty()) {
                Text("Résultat: ${qrViewModel.qrResult}")
            }
        }
        Button(
            onClick = {
                qrViewModel.linkToSupervisor(
                    onSuccess = {
                        Toast.makeText(context, "Lien réussi ✅", Toast.LENGTH_SHORT).show()
                    },
                    onError = {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Confirmer le lien")
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Retour")
        }
    }
}