package com.example.ai_guardian.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.ai_guardian.R
import com.example.ai_guardian.ui.components.RatingBar
import com.example.ai_guardian.viewmodel.RatingViewModel

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val ratingViewModel = remember { RatingViewModel() }

    var rating by remember { mutableStateOf(0) }
    var isFrench by remember { mutableStateOf(true) }

    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(
                MediaItem.fromUri(
                    "android.resource://${context.packageName}/${R.raw.video}"
                )
            )
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {

        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }



    val textFR = """
AI Guardian est une solution intelligente de sécurité numérique conçue pour protéger les personnes en temps réel.

✔ Localisation GPS en direct
✔ Alertes instantanées en cas de danger
✔ Connexion sécurisée via QR Code
✔ Gestion des rôles (Superviseur / Surveillé)
✔ Suivi d’activité et historique
✔ Système intelligent basé sur Firebase

L’application garantit une protection continue et une tranquillité d’esprit totale pour les familles et les utilisateurs.
""".trimIndent()

    val textAR = """
AI Guardian هو تطبيق ذكي للحماية الرقمية.

✔ تتبع الموقع في الوقت الحقيقي
✔ تنبيهات فورية عند الخطر
✔ ربط آمن عبر QR Code
✔ نظام مشرف ومستخدم مراقب
✔ متابعة النشاط والسجل
✔ نظام ذكي يعتمد على Firebase

يوفر التطبيق حماية مستمرة وراحة بال للمستخدمين والعائلات.
""".trimIndent()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // LOGO
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = null,
            modifier = Modifier.height(80.dp)
        )

        Text(
            "AI Guardian",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2)
        )

        Text(
            "Smart Security App",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(Modifier.height(16.dp))

        // VIDEO
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            AndroidView(
                factory = { PlayerView(it).apply { player = exoPlayer } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        Button(onClick = { isFrench = !isFrench }) {
            Text(if (isFrench) "Switch AR" else "FR")
        }

        Spacer(Modifier.height(12.dp))

        // DESCRIPTION CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isFrench) textFR else textAR,
                modifier = Modifier.padding(16.dp),
                fontSize = 14.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        // ⭐ RATING SECTION (clean reusable)
        Text(
            "Rate AI Guardian",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color(0xFF1976D2)
        )

        RatingBar(
            rating = rating,
            onRatingChanged = {
                rating = it
                ratingViewModel.sendRating(it) // 🔥 SAVE TO FIREBASE
            }
        )

        Text(
            text = when (rating) {
                5 -> "🔥 Excellent"
                4 -> "👍 Very good"
                3 -> "🙂 Good"
                2 -> "😐 Average"
                1 -> "👎 Bad"
                else -> "Tap stars"
            },
            color = Color.Gray
        )

        Spacer(Modifier.height(20.dp))


    }
}