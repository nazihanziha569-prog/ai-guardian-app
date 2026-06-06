package com.example.ai_guardian.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

@Composable
fun Base64Image(base64: String, modifier: Modifier = Modifier) {
    val bitmap = remember(base64) {
        try {
            val clean = base64.removePrefix("data:image/jpeg;base64,")
            val bytes = android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?.asImageBitmap()
        } catch (e: Exception) { null }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}