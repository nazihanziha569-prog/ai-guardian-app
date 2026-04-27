package com.example.ai_guardian.ui.screens


import android.app.TimePickerDialog
import android.media.MediaPlayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_guardian.data.datasource.FirebaseDataSource
import com.example.ai_guardian.data.repository.AlarmRepository
import com.example.ai_guardian.viewmodel.RappelViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RappelScreen() {

    val context = LocalContext.current

    val viewModel = remember {
        RappelViewModel(
            FirebaseDataSource(),
            AlarmRepository(context.applicationContext)
        )
    }

    val rappels by viewModel.rappels.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        viewModel.listenRappels()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {

        // 🔵 HEADER
        Text(
            text = "⏰ AI Guardian - Rappels",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ➕ BUTTON
        Button(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1976D2),
                contentColor = Color.White
            )
        ) {
            Text("➕ Ajouter rappel")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 📋 LIST (ONLY SCROLL HERE — NO verticalScroll anywhere)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            items(rappels) { rappel ->

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {

                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Column {

                            Text(
                                text = "📌 ${rappel.message}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )

                            Text(
                                text = "⏱ ${formatTime(rappel.time)}",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }

                        Text(
                            text = "🗑",
                            fontSize = 20.sp,
                            color = Color.Red,
                            modifier = Modifier.clickable {
                                viewModel.deleteRappel(rappel.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // 🔥 DIALOG
    if (showDialog) {

        AlertDialog(
            onDismissRequest = { showDialog = false },

            title = {
                Text(
                    "Nouveau rappel",
                    color = Color(0xFF1976D2),
                    fontWeight = FontWeight.Bold
                )
            },

            text = {

                Column {

                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Message") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance()

                            TimePickerDialog(
                                context,
                                { _, hour, minute ->

                                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                                    calendar.set(Calendar.MINUTE, minute)
                                    calendar.set(Calendar.SECOND, 0)

                                    selectedTime = calendar.timeInMillis
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2)
                        )
                    ) {
                        Text("⏰ Choisir heure")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (selectedTime > 0)
                            "Heure: ${formatTime(selectedTime)}"
                        else "Aucune heure choisie",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            },

            confirmButton = {

                Button(
                    onClick = {
                        if (message.isNotBlank() && selectedTime > 0) {

                            viewModel.addRappel(message, selectedTime)

                            message = ""
                            selectedTime = 0L
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    )
                ) {
                    Text("Ajouter")
                }
            },

            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}


fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}