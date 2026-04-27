package com.example.ai_guardian.audio

import android.content.Context
import android.media.MediaPlayer
import com.example.ai_guardian.R

class AlarmSoundManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    fun start() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, R.raw.alarm_sound)
            mediaPlayer?.isLooping = true
        }
        mediaPlayer?.start()
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}