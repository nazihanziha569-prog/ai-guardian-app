package com.example.ai_guardian.data.datasource

import android.content.Context
import android.graphics.BitmapFactory
import org.tensorflow.lite.InterpreterApi
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class FallModel(context: Context) {

    private val interpreter: InterpreterApi

    companion object {
        const val WINDOW_SIZE  = 50
        const val NUM_FEATURES = 3
    }

    init {
        val modelFile = loadModelFile(context, "fall_detection.tflite")
        // ✅ Options().setRuntime() — يستخدم أي runtime متاح (من LiveKit أو النظام)
        interpreter = InterpreterApi.create(modelFile, InterpreterApi.Options())
    }

    fun predict(window: Array<FloatArray>): FallResult {
        require(window.size == WINDOW_SIZE) {
            "Window size must be $WINDOW_SIZE, got ${window.size}"
        }

        // Input ByteBuffer: [1, 50, 3] float32
        val inputBuffer = ByteBuffer
            .allocateDirect(1 * WINDOW_SIZE * NUM_FEATURES * 4)
            .order(ByteOrder.nativeOrder())

        for (sample in window) {
            for (value in sample) {
                inputBuffer.putFloat(value)
            }
        }
        inputBuffer.rewind()

        // Output ByteBuffer: [1, 1] float32
        val outputBuffer = ByteBuffer
            .allocateDirect(1 * 1 * 4)
            .order(ByteOrder.nativeOrder())

        interpreter.run(inputBuffer, outputBuffer)

        outputBuffer.rewind()
        val score = outputBuffer.float

        return FallResult(
            isFall     = score > 0.7f,
            confidence = score
        )
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream    = FileInputStream(fileDescriptor.fileDescriptor)
        val channel        = inputStream.channel
        return channel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }
}

data class FallResult(
    val isFall:     Boolean,
    val confidence: Float
)