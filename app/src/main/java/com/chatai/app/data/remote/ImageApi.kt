package com.chatai.app.data.remote

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class ImageGenerateRequest(
    val prompt: String,
    val width: Int = 1024,
    val height: Int = 1024,
    val modelType: String = "turbo",
    val batchSize: Int = 1
)

data class ImageTaskResponse(
    val success: Boolean,
    val data: ImageTaskData? = null
)

data class ImageTaskData(
    val task: ImageTaskInfo? = null,
    val queuePosition: Int? = null
)

data class ImageTaskInfo(
    val id: String? = null,
    val uuid: String? = null,
    val taskStatus: String? = null,
    val prompt: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val model: String? = null,
    val resultUrl: String? = null,
    val progress: Int = 0,
    val errorMessage: String? = null
)

object ImageApi {
    private const val TAG = "ImageApi"
    private const val BASE_URL = "https://zimage.run/api/z-image"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateImage(
        prompt: String,
        width: Int = 1024,
        height: Int = 1024,
        onProgress: ((Int) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Create image generation task
            val request = ImageGenerateRequest(
                prompt = prompt,
                width = width,
                height = height,
                modelType = "turbo",
                batchSize = 1
            )

            val jsonBody = gson.toJson(request)
            Log.d(TAG, "Generate request: $jsonBody")

            val httpRequest = Request.Builder()
                .url("$BASE_URL/generate")
                .post(jsonBody.toRequestBody(jsonMediaType))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "*/*")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Mobile Safari/537.36")
                .addHeader("Origin", "https://zimage.run")
                .addHeader("Referer", "https://zimage.run/")
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))

            Log.d(TAG, "Generate response: $responseBody")

            val taskResponse = gson.fromJson(responseBody, ImageTaskResponse::class.java)
            val uuid = taskResponse.data?.task?.uuid

            if (uuid == null) {
                return@withContext Result.failure(Exception("Failed to create image task: $responseBody"))
            }

            Log.d(TAG, "Task created with UUID: $uuid")

            // Step 2: Poll for completion
            var attempts = 0
            val maxAttempts = 90 // ~7.5 minutes with 5s intervals

            while (attempts < maxAttempts) {
                delay(5000)
                attempts++

                val checkRequest = Request.Builder()
                    .url("$BASE_URL/task/$uuid")
                    .addHeader("Accept", "*/*")
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Mobile Safari/537.36")
                    .addHeader("Referer", "https://zimage.run/")
                    .build()

                val checkResponse = client.newCall(checkRequest).execute()
                val checkBody = checkResponse.body?.string() ?: continue

                val checkResult = gson.fromJson(checkBody, ImageTaskResponse::class.java)
                val task = checkResult.data?.task

                when (task?.taskStatus) {
                    "completed" -> {
                        val imageUrl = task.resultUrl
                        if (imageUrl != null) {
                            Log.d(TAG, "Image generated: $imageUrl")
                            return@withContext Result.success(imageUrl)
                        } else {
                            return@withContext Result.failure(Exception("Completed but no URL"))
                        }
                    }
                    "failed" -> {
                        val errorMsg = task.errorMessage ?: "Unknown error"
                        Log.e(TAG, "Image generation failed: $errorMsg")
                        return@withContext Result.failure(Exception(errorMsg))
                    }
                    "pending", "processing" -> {
                        val progress = task.progress
                        Log.d(TAG, "Progress: $progress%, attempt $attempts/$maxAttempts")
                        onProgress?.invoke(progress)
                    }
                }
            }

            Result.failure(Exception("Image generation timed out after ${maxAttempts * 5}s"))
        } catch (e: Exception) {
            Log.e(TAG, "Image generation error: ${e.message}")
            Result.failure(e)
        }
    }
}
