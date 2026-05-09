package com.example.ai_guardian.ai

import android.content.res.AssetManager
import org.tensorflow.lite.Interpreter
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class FallEngine(private val assets: AssetManager) {

    private val tflite: Interpreter
    private val lock = Any()

    // window glissante de 50 échantillons — même logique que FallDetectionService
    private val window = ArrayDeque<FloatArray>()

    init {
        tflite = Interpreter(loadModel())
    }

    fun addSample(x: Float, y: Float, z: Float) {
        window.addLast(floatArrayOf(x, y, z))
        if (window.size > 50) window.removeFirst()
    }

    fun predict(x: Float, y: Float, z: Float): Boolean {
        addSample(x, y, z)

        // pas assez de données encore
        if (window.size < 50) return false

        return try {
            synchronized(lock) {
                // input shape : [1][50][3]  — identique à FallDetectionService
                val input = Array(1) { Array(50) { FloatArray(3) } }
                val data  = window.toList()

                for (i in 0 until 50) {
                    input[0][i][0] = data[i][0]
                    input[0][i][1] = data[i][1]
                    input[0][i][2] = data[i][2]
                }

                val output = Array(1) { FloatArray(1) }
                tflite.run(input, output)

                // double vérification : confiance AI + magnitude physique
                val confidence = output[0][0] > 0.85f
                val magnitude  = kotlin.math.sqrt(x * x + y * y + z * z)
                val possibleFall = magnitude > 20f

                confidence && possibleFall
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun loadModel(): MappedByteBuffer {
        val file  = assets.openFd("fall_detection.tflite")
        val input = file.createInputStream()
        return input.channel.map(
            FileChannel.MapMode.READ_ONLY,
            file.startOffset,
            file.declaredLength
        )
    }
}