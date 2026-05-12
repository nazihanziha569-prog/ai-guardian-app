package com.example.ai_guardian.ui.screens

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai_guardian.audio.SpeechRecognitionManager
import com.example.ai_guardian.data.model.ChatMessage
import com.example.ai_guardian.data.model.Config
import com.example.ai_guardian.viewmodel.ChatViewModel

private val CBlue   = Color(0xFF1976D2)
private val CBlueL  = Color(0xFFE3F2FD)
private val CGreen  = Color(0xFF43A047)
private val CDark   = Color(0xFF1A1A2E)
private val CBubble = Color(0xFF1565C0)

@Composable
fun ChatbotScreen(
    viewModel  : ChatViewModel,
    config     : Config? = null,
    userName   : String  = "Utilisateur",
    onSosClick : () -> Unit = {}
) {
    val speechState by viewModel.speechState.collectAsState()
    val voiceMode   by viewModel.voiceMode.collectAsState()
    val context   = LocalContext.current
    val messages  by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startListening()
    }

    // ✅ Init TTS avec le Context
    LaunchedEffect(Unit) {
        viewModel.initTts(context)
        viewModel.sendWelcome(userName)
    }

    // Scroll automatique
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }


    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFF))
    ) {

        // ── Header ────────────────────────────────────────────────────────
        ChatHeader(
            userName   = userName,
            isSpeaking = isSpeaking,
            onSosClick = onSosClick,
            onStopTts  = { viewModel.stopSpeaking() }
        )

        // ── Messages ──────────────────────────────────────────────────────
        LazyColumn(
            state               = listState,
            modifier            = Modifier.weight(1f),
            contentPadding      = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                AnimatedVisibility(
                    visible = true,
                    enter   = fadeIn() + slideInVertically { it / 2 }
                ) {
                    MessageBubble(message = msg)
                }
            }
            if (isLoading) {
                item { TypingIndicator() }
            }
        }

        // ── Quick Replies ─────────────────────────────────────────────────
        QuickReplies(onQuickReply = { inputText = it })

        // ── Input ─────────────────────────────────────────────────────────
        ChatInput(
            value       = inputText,
            onChange    = { inputText = it },
            isLoading   = isLoading,
            speechState = speechState,
            voiceMode   = voiceMode,
            onSend      = {
                val text = inputText.trim()
                if (text.isEmpty() || isLoading) return@ChatInput
                inputText = ""
                viewModel.sendMessage(text, userName, config)
            },
            onMicClick  = {
                if (speechState == SpeechRecognitionManager.SpeechState.LISTENING) {
                    viewModel.stopListening()
                } else {
                    viewModel.initSpeech(context)
                    // vérifier permission
                    val granted = ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) viewModel.startListening()
                    else permLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                }
            },
            onVoiceMode = { viewModel.toggleVoiceMode(context) }
        )
    }
    }


// ════════════════════════════════════════════════════════════════════════════
// Header — avec indicateur TTS + bouton stop
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun ChatHeader(
    userName   : String,
    isSpeaking : Boolean,
    onSosClick : () -> Unit,
    onStopTts  : () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(CBlue, Color(0xFF1565C0))))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar avec animation quand il parle
            Box(
                modifier = Modifier
                    .size(42.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = if (isSpeaking) 0.35f else 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isSpeaking) "🔊" else "🤖", fontSize = 22.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("AI Guardian", color = Color.White,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // ✅ indicateur "يتكلم..." أو "متصل"
                    Box(
                        modifier = Modifier.size(7.dp).clip(CircleShape)
                            .background(if (isSpeaking) Color(0xFFFFC107) else CGreen)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (isSpeaking) "يتكلم..." else "متصل دايماً",
                        color    = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }

            // ✅ زر إيقاف الصوت — يظهر فقط وقت الكلام
            if (isSpeaking) {
                IconButton(onClick = onStopTts) {
                    Icon(Icons.Default.VolumeOff, contentDescription = "Stop",
                        tint = Color.White)
                }
            }

            Spacer(Modifier.width(4.dp))

            // زر SOS
            Button(
                onClick        = onSosClick,
                colors         = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                shape          = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("🚨 SOS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Message Bubble
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment     = Alignment.Bottom
    ) {
        if (!message.isUser) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(CBlueL),
                contentAlignment = Alignment.Center
            ) { Text("🤖", fontSize = 16.sp) }
            Spacer(Modifier.width(6.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(
                    topStart    = 18.dp, topEnd      = 18.dp,
                    bottomStart = if (message.isUser) 18.dp else 4.dp,
                    bottomEnd   = if (message.isUser) 4.dp  else 18.dp
                ))
                .background(if (message.isUser) CBubble else Color.White)
                .border(
                    width = if (message.isUser) 0.dp else 1.dp,
                    color = if (message.isUser) Color.Transparent else Color(0xFFE0E0E0),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text       = message.content,
                color      = if (message.isUser) Color.White else CDark,
                fontSize   = 14.sp,
                lineHeight = 20.sp
            )
        }

        if (message.isUser) {
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(CBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) { Text("👤", fontSize = 16.sp) }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Typing Indicator
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun TypingIndicator() {
    val inf   = rememberInfiniteTransition(label = "typing")
    val alpha by inf.animateFloat(
        initialValue  = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label         = "alpha"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(CBlueL),
            contentAlignment = Alignment.Center
        ) { Text("🤖", fontSize = 16.sp) }
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(CBlue.copy(alpha = alpha)))
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Quick Replies
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun QuickReplies(onQuickReply: (String) -> Unit) {
    val replies = listOf(
        "وقتاش ناخذ دوائي؟",
        "عندي شي rappel اليوم؟",
        "نحس بدوخة",
        "نسيت الدواء",
        "عيط للمشرف",
        "أنا بخير ✅"
    )
    LazyRow(
        modifier              = Modifier.fillMaxWidth(),
        contentPadding        = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(replies) { reply ->
            AssistChip(
                onClick = { onQuickReply(reply) },
                label   = { Text(reply, fontSize = 12.sp) },
                colors  = AssistChipDefaults.assistChipColors(
                    containerColor = CBlueL, labelColor = CBlue
                ),
                border  = AssistChipDefaults.assistChipBorder(
                    enabled = true, borderColor = CBlue.copy(alpha = 0.3f)
                )
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Chat Input
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun ChatInput(
    value      : String,
    onChange   : (String) -> Unit,
    isLoading  : Boolean,
    speechState: SpeechRecognitionManager.SpeechState,
    voiceMode  : Boolean,
    onSend     : () -> Unit,
    onMicClick : () -> Unit,
    onVoiceMode: () -> Unit
) {
    val isListening   = speechState == SpeechRecognitionManager.SpeechState.LISTENING
    val isProcessing  = speechState == SpeechRecognitionManager.SpeechState.PROCESSING

    // Animation pulse pour le micro
    val inf = rememberInfiniteTransition(label = "mic")
    val micScale by inf.animateFloat(
        initialValue  = 1f, targetValue = if (isListening) 1.2f else 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label         = "micScale"
    )

    Surface(tonalElevation = 4.dp, color = Color.White) {
        Column {
            // ── Indicateur état vocal ──────────────────────────────────────
            if (speechState != SpeechRecognitionManager.SpeechState.IDLE || voiceMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (voiceMode) Color(0xFFE3F2FD) else Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        Modifier.size(8.dp).clip(CircleShape).background(
                            when (speechState) {
                                SpeechRecognitionManager.SpeechState.LISTENING   -> Color(0xFF43A047)
                                SpeechRecognitionManager.SpeechState.PROCESSING  -> Color(0xFFFFA000)
                                else -> if (voiceMode) CBlue else Color.Transparent
                            }
                        )
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when {
                            isListening  -> "🎤 يسمعك..."
                            isProcessing -> "⏳ يفهم..."
                            voiceMode    -> "🔄 وضع صوتي مفعّل — انتظر..."
                            else         -> ""
                        },
                        fontSize = 12.sp, color = CBlue, fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── زر وضع المحادثة المستمر ──────────────────────────────
                IconButton(onClick = onVoiceMode) {
                    Icon(
                        imageVector = if (voiceMode) Icons.Default.RecordVoiceOver
                        else Icons.Default.VoiceChat,
                        contentDescription = "Voice Mode",
                        tint = if (voiceMode) Color(0xFFE53935) else Color.Gray,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // ── حقل النص ──────────────────────────────────────────────
                OutlinedTextField(
                    value         = value,
                    onValueChange = onChange,
                    modifier      = Modifier.weight(1f),
                    placeholder   = {
                        Text(
                            if (isListening) "يسمعك..." else "اكتب أو تكلم...",
                            color = Color.Gray, fontSize = 14.sp
                        )
                    },
                    shape  = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = CBlue,
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    ),
                    maxLines  = 3,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )

                Spacer(Modifier.width(8.dp))

                // ── زر الميكروفون (استخدام واحد) ─────────────────────────
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .scale(if (isListening) micScale else 1f)
                        .clip(CircleShape)
                        .background(
                            when {
                                isListening  -> Color(0xFF43A047)
                                isProcessing -> Color(0xFFFFA000)
                                else         -> Color(0xFF5C6BC0)
                            }
                        )
                        .clickable(enabled = !isProcessing) { onMicClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mic",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                // ── زر الإرسال ────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(46.dp).clip(CircleShape)
                        .background(if (!isLoading && value.isNotBlank()) CBlue else Color(0xFFBDBDBD))
                        .clickable(enabled = !isLoading && value.isNotBlank()) { onSend() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Send",
                            tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}