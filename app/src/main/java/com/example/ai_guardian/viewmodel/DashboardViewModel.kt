package com.example.ai_guardian.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class DashboardViewModel : ViewModel() {

    var selectedScreen by mutableStateOf("home")
        private set

    fun selectScreen(screen: String) {
        selectedScreen = screen
    }
}