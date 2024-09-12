package com.littlekai.kspeechtranslator

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject

class SentenceProcessor(private val context: Context, private var geminiApiKey: String) {
    private val TAG = "SentenceProcessor"
    private val client = OkHttpClient()
    private val geminiApiUrl =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent"

    // Minimum length of text to process (adjust as needed)
    private val MIN_TEXT_LENGTH = 3

    suspend fun processSentence(text: String, language: String): String {
        return withContext(Dispatchers.IO) {
            val trimmedText = text.trim()

            if (trimmedText.length < MIN_TEXT_LENGTH) {
                Log.d(TAG, "Text too short, returning original: $trimmedText")
                return@withContext trimmedText
            }

            val prompt = """
            Process the following sentence in $language. 
            Return only the processed sentence without any additional explanation or analysis.
            If the sentence is grammatically correct and doesn't need changes, return it as is.
            If changes are needed, make minimal necessary corrections to improve grammar and clarity.
            
            Input: "$trimmedText"
            Processed sentence:
            """

            val response = sendRequestToGemini(prompt)
            parseGeminiResponse(response).trim()
        }
    }

    private suspend fun sendRequestToGemini(prompt: String): String {
        val jsonBody = JSONObject().apply {
            put("contents", JSONObject().apply {
                put("parts", JSONObject().apply {
                    put("text", prompt)
                })
            })
        }

        val request = Request.Builder()
            .url("$geminiApiUrl?key=$geminiApiKey")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Gemini API request failed: ${response.code}")
        }

        return response.body?.string() ?: throw Exception("Empty response from Gemini API")
    }

    private fun parseGeminiResponse(response: String): String {
        val jsonResponse = JSONObject(response)
        val candidates = jsonResponse.getJSONArray("candidates")
        if (candidates.length() > 0) {
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            if (parts.length() > 0) {
                return parts.getJSONObject(0).getString("text")
            }
        }
        throw Exception("Unable to parse Gemini response")
    }

    fun updateApiKey(newApiKey: String) {
        geminiApiKey = newApiKey
    }
}