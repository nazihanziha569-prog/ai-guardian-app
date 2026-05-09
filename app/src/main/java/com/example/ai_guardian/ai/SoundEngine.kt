package com.example.ai_guardian.ai

class SoundEngine {

    fun computeSoundLevel(data: FloatArray): Float {
        return data.map { kotlin.math.abs(it) }.average().toFloat()
    }
}