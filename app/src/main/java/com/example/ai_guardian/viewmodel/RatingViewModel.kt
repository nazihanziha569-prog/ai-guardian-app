package com.example.ai_guardian.viewmodel

import androidx.lifecycle.ViewModel
import com.example.ai_guardian.data.repository.RatingRepository

class RatingViewModel : ViewModel() {

    private val repo = RatingRepository()

    fun sendRating(rating: Int) {
        repo.saveRating(rating)
    }
}