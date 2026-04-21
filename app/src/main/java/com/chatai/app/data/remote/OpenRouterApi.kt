package com.chatai.app.data.remote

import android.util.Log
import com.chatai.app.data.remote.dto.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenRouterApi() {

    companion object {
        private const val TAG = "OpenRouterApi"
        private const val BASE_URL = "https://openrouter.ai/api/v1"
        private const val CHAT_ENDPOINT = "$BASE_URL/chat/completions"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun sendMessageStream(
        apiKey: String,
        model: String,
        messages: List<MessageDto>
    ): Flow<String> = flow {
        val request = ChatRequest(
            model = model,
            messages = messages,
            stream = true
        )

        val jsonBody = gson.toJson(request)
        Log.d(TAG, "Request: $jsonBody")

        val requestBody = jsonBody.toRequestBody(jsonMediaType)

        val httpRequest = Request.Builder()
            .url(CHAT_ENDPOINT)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("HTTP-Referer", "https://chatai.app")
            .addHeader("X-Title", "ChatAI Android")
            .build()

        val factory = EventSources.createFactory(client)

        var eventSource: EventSource? = null

        try {
            eventSource = factory.newEventSource(httpRequest, object : EventSourceListener() {
                override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                    if (data == "[DONE]") {
                        eventSource.cancel()
                        return
                    }

                    try {
                        val chunk = gson.fromJson(data, StreamChunk::class.java)
                        val content = chunk.choices?.firstOrNull()?.delta?.content
                        if (content != null) {
                            // We can't directly emit from here, use a callback approach
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing SSE chunk: ${e.message}")
                    }
                }

                override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                    Log.e(TAG, "SSE Error: ${t?.message}, Response: ${response?.code}")
                    eventSource.cancel()
                }

                override fun onClosed(eventSource: EventSource) {
                    Log.d(TAG, "SSE connection closed")
                }

                override fun onOpen(eventSource: EventSource, response: Response) {
                    Log.d(TAG, "SSE connection opened")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error creating SSE connection: ${e.message}")
            throw e
        }

        // Alternative: Use synchronous streaming for better control
        val response = client.newCall(httpRequest).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            Log.e(TAG, "API Error ${response.code}: $errorBody")
            throw IOException("API Error: ${response.code} - $errorBody")
        }

        val reader = response.body?.byteStream()?.bufferedReader()
            ?: throw IOException("Empty response body")

        reader.use { br ->
            var line: String?
            while (br.readLine().also { line = it } != null) {
                val currentLine = line ?: continue

                if (currentLine.startsWith("data: ")) {
                    val data = currentLine.removePrefix("data: ").trim()
                    if (data == "[DONE]") {
                        break
                    }

                    try {
                        val chunk = gson.fromJson(data, StreamChunk::class.java)
                        val content = chunk.choices?.firstOrNull()?.delta?.content
                        if (content != null) {
                            emit(content)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing chunk: ${e.message}, data: $data")
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun sendNonStreamMessage(
        apiKey: String,
        model: String,
        messages: List<MessageDto>
    ): String {
        val request = ChatRequest(
            model = model,
            messages = messages,
            stream = false
        )

        val jsonBody = gson.toJson(request)
        val requestBody = jsonBody.toRequestBody(jsonMediaType)

        val httpRequest = Request.Builder()
            .url(CHAT_ENDPOINT)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("HTTP-Referer", "https://chatai.app")
            .addHeader("X-Title", "ChatAI Android")
            .build()

        val response = client.newCall(httpRequest).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            throw IOException("API Error: ${response.code} - $errorBody")
        }

        val responseBody = response.body?.string() ?: throw IOException("Empty response")
        val fullResponse = gson.fromJson(responseBody, StreamChunk::class.java)
        return fullResponse.choices?.firstOrNull()?.delta?.content
            ?: fullResponse.choices?.firstOrNull()?.let {
                // Try parsing as full response
                val json = gson.fromJson(responseBody, Map::class.java)
                val choices = json["choices"] as? List<*>
                val firstChoice = choices?.firstOrNull() as? Map<*, *>
                val message = firstChoice?.get("message") as? Map<*, *>
                message?.get("content") as? String
            } ?: ""
    }
}
