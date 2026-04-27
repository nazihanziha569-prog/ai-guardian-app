package com.example.ai_guardian.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.ai_guardian.R

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme


// ── Color palette ────────────────────────────────────────────────────────────
private val BgDeep        = Color(0xFF0A0D14)
private val BgCard        = Color(0x0AFFFFFF)
private val BgCardBorder  = Color(0x14FFFFFF)
private val BgPurpleCard  = Color(0x14534AB7)
private val PurplePrimary = Color(0xFF534AB7)
private val PurpleDark    = Color(0xFF3C3489)
private val GreenAccent   = Color(0xFF1D9E75)
private val TextPrimary   = Color(0xFFF0EEE8)
private val TextMuted     = Color(0xFF888780)
private val TextBody      = Color(0xFFB4B2A9)
private val StarColor     = Color(0xFFFAC775)
private val PurpleBorder  = Color(0x4D534AB7)

@Composable
@Preview(showBackground = true, showSystemUi = true)
fun AboutScreenPreview() {
    MaterialTheme {
        AboutScreen(onBack = {})
    }}
@Composable

fun AboutScreen(onBack: () -> Unit) {

    var isFrench by remember { mutableStateOf(true) }
    val context = LocalContext.current

    val featuresFR = listOf(
        "Localisation GPS en direct",
        "Alertes instantanées en cas de danger",
        "Connexion sécurisée via QR Code",
        "Gestion des rôles (Superviseur / Surveillé)",
        "Suivi d'activité et historique",
        "Système intelligent basé sur Firebase"
    )
    val featuresAR = listOf(
        "تتبع الموقع في الوقت الحقيقي",
        "تنبيهات فورية عند الخطر",
        "ربط آمن عبر QR Code",
        "نظام مشرف ومستخدم مراقب",
        "متابعة النشاط والسجل",
        "نظام ذكي يعتمد على Firebase"
    )
    val reviews = listOf(
        "Application très utile pour la sécurité des enfants",
        "Interface simple et efficace",
        "Meilleure app de surveillance"
    )

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(
                "android.resource://${context.packageName}/${R.raw.video}"
            )
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            // ── Logo ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PurplePrimary, GreenAccent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🛡", fontSize = 32.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "AI Guardian",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "PREMIUM SECURITY APP",
                fontSize = 11.sp,
                color = TextMuted,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Video player ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, PurpleBorder, RoundedCornerShape(16.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Language switcher ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF131620)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LangTab(label = "Français", isActive = isFrench, modifier = Modifier.weight(1f)) {
                    isFrench = true
                }
                LangTab(label = "العربية", isActive = !isFrench, modifier = Modifier.weight(1f)) {
                    isFrench = false
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Features card ─────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .border(1.dp, BgCardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = if (isFrench) "FONCTIONNALITÉS" else "المزايا",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PurplePrimary,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                val features = if (isFrench) featuresFR else featuresAR
                features.forEachIndexed { index, feature ->
                    FeatureRow(text = feature, isRtl = !isFrench)
                    if (index < features.lastIndex) {
                        Divider()
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Reviews card ──────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgPurpleCard)
                    .border(1.dp, PurpleBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "4.8",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = StarColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "★★★★★", fontSize = 14.sp, color = StarColor)
                }
                Spacer(modifier = Modifier.height(10.dp))
                reviews.forEachIndexed { index, review ->
                    Text(
                        text = "\"$review\"",
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 18.sp
                    )
                    if (index < reviews.lastIndex) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Back button ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PurplePrimary, PurpleDark)
                        )
                    )
                    .clickable {
                        exoPlayer.stop()
                        exoPlayer.release()
                        onBack()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Retour",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

// ── Composables helpers ───────────────────────────────────────────────────────

@Composable
private fun LangTab(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFF534AB7) else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "langTabBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFFF0EEE8) else Color(0xFF888780),
        animationSpec = tween(durationMillis = 200),
        label = "langTabText"
    )
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
private fun FeatureRow(text: String, isRtl: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = if (isRtl) Arrangement.End else Arrangement.Start
    ) {
        if (!isRtl) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .offset(y = 5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1D9E75))
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color(0xFFB4B2A9),
            lineHeight = 19.sp,
            textAlign = if (isRtl) TextAlign.End else TextAlign.Start,
            modifier = Modifier.weight(1f)
        )
        if (isRtl) {
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .offset(y = 5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1D9E75))
            )
        }
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(Color(0x0DFFFFFF))
    )
}
