package com.example.ai_guardian.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_guardian.R
import com.example.ai_guardian.viewmodel.QRViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun GenerateQRScreen(
    qrViewModel: QRViewModel,
    onBack: () -> Unit
) {

    // 🔥 UID متاع superviseur
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val qrBitmap = generateQrCode(uid)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(40.dp))

        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "logo",
            modifier = Modifier.size(120.dp)
        )

        Text(
            text = "AI Guardian",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Text("QR du superviseur")

        Spacer(modifier = Modifier.height(30.dp))

        // 🔳 QR Code
        Image(
            bitmap = qrBitmap.asImageBitmap(),
            contentDescription = "QR Code",
            modifier = Modifier.size(250.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Faites scanner ce code par la personne surveillée",
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Retour")
        }
    }
}
fun generateQrCode(text: String): Bitmap {
    val size = 512
    val bits = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)

    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

    for (x in 0 until size) {
        for (y in 0 until size) {
            bmp.setPixel(
                x,
                y,
                if (bits[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            )
        }
    }

    return bmp
}