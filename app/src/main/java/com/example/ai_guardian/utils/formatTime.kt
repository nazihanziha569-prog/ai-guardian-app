package com.example.ai_guardian.utils

import java.text.SimpleDateFormat
import java.util.*

fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return "Heure inconnue"
    return SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault())
        .format(Date(timestamp))
}