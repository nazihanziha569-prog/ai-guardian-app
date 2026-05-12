package com.example.ai_guardian.utils


import android.content.Context
import org.json.JSONObject
import kotlin.math.*

class KeywordClassifier(context: Context) {

    enum class Intent { NORMAL, DISTRESS, EMERGENCY }

    data class Result(val intent: Intent, val confidence: Float)

    private val vocab    : Map<String, Int>
    private val idf      : DoubleArray
    private val coef     : Array<DoubleArray>
    private val intercept: DoubleArray
    private val ngramMin : Int
    private val ngramMax : Int

    init {
        val json = JSONObject(
            context.assets.open("keyword_model.json")
                .bufferedReader().readText()
        )

        val vocabJ = json.getJSONObject("vocab")
        vocab = buildMap {
            vocabJ.keys().forEach { k -> put(k, vocabJ.getInt(k)) }
        }

        val idfJ = json.getJSONArray("idf")
        idf = DoubleArray(idfJ.length()) { idfJ.getDouble(it) }

        val coefJ = json.getJSONArray("coef")
        coef = Array(coefJ.length()) { i ->
            val row = coefJ.getJSONArray(i)
            DoubleArray(row.length()) { j -> row.getDouble(j) }
        }

        val intJ = json.getJSONArray("intercept")
        intercept = DoubleArray(intJ.length()) { intJ.getDouble(it) }

        ngramMin = json.getInt("ngram_min")
        ngramMax = json.getInt("ngram_max")
    }

    fun classify(text: String): Result {
        val clean  = preprocess(text)
        val vector = tfidfVectorize(clean)
        val scores = linearDecision(vector)
        val probs  = softmax(scores)

        val maxIdx = probs.indices.maxByOrNull { probs[it] } ?: 0
        val conf   = probs[maxIdx].toFloat()

        val intent = when (maxIdx) {
            2    -> Intent.EMERGENCY
            1    -> Intent.DISTRESS
            else -> Intent.NORMAL
        }

        return if (conf >= 0.65f) Result(intent, conf)
        else Result(Intent.NORMAL, conf)
    }

    private fun preprocess(text: String): String {
        return text.lowercase().trim()
            .replace(Regex("[^\\w\\s\\u0600-\\u06FF]"), " ")
    }

    private fun tfidfVectorize(text: String): DoubleArray {
        val padded   = " $text "
        val termFreq = mutableMapOf<Int, Int>()

        for (n in ngramMin..ngramMax) {
            for (i in 0..padded.length - n) {
                val ngram = padded.substring(i, i + n)
                val idx   = vocab[ngram] ?: continue
                termFreq[idx] = (termFreq[idx] ?: 0) + 1
            }
        }

        val vector = DoubleArray(idf.size)
        val total  = termFreq.values.sum().toDouble()
        if (total == 0.0) return vector

        termFreq.forEach { (idx, count) ->
            if (idx < idf.size) {
                val tf = ln(1.0 + count / total)
                vector[idx] = tf * idf[idx]
            }
        }

        val norm = sqrt(vector.sumOf { it * it })
        if (norm > 0) vector.indices.forEach { vector[it] /= norm }

        return vector
    }

    private fun linearDecision(vector: DoubleArray): DoubleArray {
        return DoubleArray(coef.size) { i ->
            intercept[i] + coef[i].indices.sumOf { j ->
                coef[i][j] * vector[j]
            }
        }
    }

    private fun softmax(scores: DoubleArray): DoubleArray {
        val max = scores.max()
        val exp = scores.map { exp(it - max) }
        val sum = exp.sum()
        return exp.map { it / sum }.toDoubleArray()
    }
}