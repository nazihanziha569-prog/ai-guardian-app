package com.example.ai_guardian.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
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
@Composable
fun AboutScreen(onBack: () -> Unit) {

    var isFrench by remember { mutableStateOf(true) }

    val context = LocalContext.current

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

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(
                "android.resource://${context.packageName}/${R.raw.video}"
            )
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    fun stopVideo() {
        exoPlayer.stop()
        exoPlayer.release()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 🔷 HEADER (logo + title)
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = null,
            modifier = Modifier.height(80.dp)
        )

        Text(
            text = "AI Guardian",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Premium Security App",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(15.dp))

        // 🎥 VIDEO (premium style)
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 🌍 LANGUAGE SWITCH
        Button(onClick = { isFrench = !isFrench }) {
            Text(if (isFrench) "Switch to AR" else "FR")
        }

        Spacer(modifier = Modifier.height(15.dp))

        // 🧠 FEATURES CARDS STYLE
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isFrench) textFR else textAR,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        // ⭐ FAKE REVIEWS (pro touch)
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("⭐ 4.8 / 5")
                Text("“Application très utile pour la sécurité des enfants”")
                Text("“Interface simple et efficace”")
                Text("“Meilleure app de surveillance”")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 🔙 BACK BUTTON
        Button(onClick = {
            stopVideo()
            onBack()
        }) {
            Text("Retour")
        }
    }
}