package com.example

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateContent(
        prompt: String,
        systemInstruction: String = "You are a warm, encouraging, and highly knowledgeable AI Tutor for Siphokazi Charity Timba, a South African Grade 11 & 12 student. Explain concepts clearly, align with the South African CAPS curriculum, and use local examples when appropriate."
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured or uses default template value")
            return@withContext "Hi Siphokazi! I would love to teach you today, but my Gemini API Key is not set in the workspace secrets. Please add GEMINI_API_KEY to secrets to turn on live AI chat!"
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        try {
            // Build the JSON body
            val requestJson = JSONObject().apply {
                // contents block
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                }
                put("contents", contentsArray)

                // systemInstruction block
                val systemPart = JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstruction)
                        })
                    })
                }
                put("systemInstruction", systemPart)
            }

            val requestBody = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "aistudio-build")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Gemini API call failed: ${response.code} $errBody")
                    return@withContext "I encountered an issue connecting to the AI brain (HTTP ${response.code}). Let's try again in a moment, Siphokazi!"
                }

                val responseBodyStr = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBodyStr)

                // Extract text from: candidates[0].content.parts[0].text
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.optJSONObject("content")
                    if (contentObj != null) {
                        val parts = contentObj.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text")
                            if (text.isNotEmpty()) {
                                return@withContext text
                            }
                        }
                    }
                }
                return@withContext "My AI brain returned an empty response. Ask me anything else Siphokazi!"
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error calling Gemini: ", e)
            return@withContext "Network error: I couldn't reach the AI tutor. Please check Siphokazi's internet connection and try again."
        } catch (e: Exception) {
            Log.e(TAG, "General error in Gemini Client: ", e)
            return@withContext "Oops, Siphokazi! Something went wrong while parsing the AI answer: ${e.localizedMessage}"
        }
    }
}
