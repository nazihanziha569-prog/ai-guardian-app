package com.example.ai_guardian.viewmodel

import androidx.lifecycle.ViewModel
import com.example.ai_guardian.data.repository.CallRepository

class CallViewModel(
    private val repo: CallRepository = CallRepository()
) : ViewModel() {

    fun sendCall(
        from: String,
        to: String,
        onSuccess: (String) -> Unit
    ) {
        repo.createCall(from, to, onSuccess)
    }}
