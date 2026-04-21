package com.chatai.app.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

data class ImageGenerateRequest(
    val prompt: String,
    val width: Int = 1024,
    val height: Int = 1024,
    val modelType: String = "turbo",
    val batchSize: Int = 1
)

data class ImageTaskResponse(
    val success: Boolean = false,
    val data: ImageTaskData? = null,
    val error: String? = null,
    val message: String? = null
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

    /**
     * Generate an image via zimage.run API.
     * This method is heavily guarded against crashes:
     * - All JSON parsing is try-caught
     * - Network errors are caught gracefully
     * - Null responses are handled
     * - Never throws exceptions to caller (always returns Result)
     */
    suspend fun generateImage(
        prompt: String,
        width: Int = 1024,
        height: Int = 1024,
        onProgress: ((Int) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        var lastError: Exception? = null

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

            var responseBody: String? = null
            try {
                val response = client.newCall(httpRequest).execute()
                responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    lastError = Exception("HTTP ${response.code}: ${responseBody?.take(500) ?: "empty body"}")
                    Log.e(TAG, "Generate failed: HTTP ${response.code}")
                    return@withContext Result.failure(lastError)
                }

                if (responseBody.isNullOrBlank()) {
                    lastError = Exception("Empty response from generate endpoint")
                    return@withContext Result.failure(lastError)
                }
            } catch (e: Exception) {
                lastError = when (e) {
                    is SocketTimeoutException -> Exception("Kết nối quá thời gian (timeout)")
                    is UnknownHostException -> Exception("Không thể kết nối đến máy chủ")
                    else -> Exception("Lỗi mạng: ${e.message}")
                }
                Log.e(TAG, "Generate request failed: ${lastError!!.message}")
                return@withContext Result.failure(lastError)
            }

            Log.d(TAG, "Generate response: $responseBody")

            // Safely parse response - this is the most crash-prone part
            val uuid: String? = try {
                val taskResponse = safeParseResponse<ImageTaskResponse>(responseBody)
                if (taskResponse?.error != null) {
                    lastError = Exception("API Error: ${taskResponse.error}")
                    return@withContext Result.failure(lastError)
                }
                taskResponse?.data?.task?.uuid
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse generate response as ImageTaskResponse", e)
                // Try parsing as raw JSON to extract uuid manually
                try {
                    val json = JsonParser.parseString(responseBody).asJsonObject
                    val dataObj = json.getAsJsonObject("data")?.getAsJsonObject("task")
                    dataObj?.get("uuid")?.asString
                } catch (e2: Exception) {
                    Log.e(TAG, "Manual JSON parsing also failed", e2)
                    lastError = Exception("Không thể đọc phản hồi từ server: ${responseBody.take(200)}")
                    return@withContext Result.failure(lastError)
                }
            }

            if (uuid == null) {
                lastError = Exception("Không nhận được ID tác vụ từ server. Response: ${responseBody?.take(200)}")
                return@withContext Result.failure(lastError)
            }

            Log.d(TAG, "Task created with UUID: $uuid")

            // Step 2: Poll for completion (sequential, not parallel)
            var attempts = 0
            val maxAttempts = 90 // ~7.5 minutes with 5s intervals

            while (attempts < maxAttempts) {
                delay(5000)
                attempts++

                var checkBody: String? = null
                try {
                    val checkRequest = Request.Builder()
                        .url("$BASE_URL/task/$uuid")
                        .addHeader("Accept", "*/*")
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Mobile Safari/537.36")
                        .addHeader("Referer", "https://zimage.run/")
                        .build()

                    val checkResponse = client.newCall(checkRequest).execute()
                    checkBody = checkResponse.body?.string()

                    if (checkBody.isNullOrBlank()) {
                        Log.w(TAG, "Empty check response at attempt $attempts, retrying...")
                        continue
                    }

                    // Safely parse check response
                    val task = try {
                        val checkResult = safeParseResponse<ImageTaskResponse>(checkBody)
                        checkResult?.data?.task
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse check response at attempt $attempts", e)
                        // Try raw JSON
                        try {
                            val json = JsonParser.parseString(checkBody).asJsonObject
                            val taskObj = json.getAsJsonObject("data")?.getAsJsonObject("task")
                            taskObj?.let { t ->
                                ImageTaskInfo(
                                    uuid = t.get("uuid")?.asString,
                                    taskStatus = t.get("taskStatus")?.asString,
                                    resultUrl = t.get("resultUrl")?.asString,
                                    progress = t.get("progress")?.asInt ?: 0,
                                    errorMessage = t.get("errorMessage")?.asString
                                )
                            }
                        } catch (e2: Exception) {
                            Log.w(TAG, "Raw JSON parsing also failed at attempt $attempts", e2)
                            null
                        }
                    }

                    when (task?.taskStatus) {
                        "completed" -> {
                            val imageUrl = task.resultUrl
                            if (imageUrl != null && imageUrl.isNotBlank()) {
                                Log.d(TAG, "Image generated: $imageUrl")
                                return@withContext Result.success(imageUrl)
                            } else {
                                lastError = Exception("Hoàn thành nhưng không có URL ảnh")
                                return@withContext Result.failure(lastError)
                            }
                        }
                        "failed" -> {
                            val errorMsg = task.errorMessage ?: "Lỗi không xác định"
                            Log.e(TAG, "Image generation failed: $errorMsg")
                            lastError = Exception(errorMsg)
                            return@withContext Result.failure(lastError)
                        }
                        "pending", "processing" -> {
                            val progress = task.progress
                            Log.d(TAG, "Progress: $progress%, attempt $attempts/$maxAttempts")
                            onProgress?.invoke(progress)
                        }
                        null -> {
                            Log.w(TAG, "No task status at attempt $attempts, response: ${checkBody?.take(100)}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Poll attempt $attempts failed: ${e.message}")
                    // Continue polling on transient errors
                    continue
                }
            }

            lastError = Exception("Hết thời gian tạo ảnh sau ${maxAttempts * 5} giây")
            Result.failure(lastError)
        } catch (e: Exception) {
            // Ultimate safety net - this should never be reached
            Log.e(TAG, "Unexpected error in generateImage", e)
            Result.failure(e)
        }
    }

    /**
     * Safely parse JSON response, catching all Gson exceptions.
     * Returns null on any parse failure instead of crashing.
     */
    private inline fun <reified T> safeParseResponse(json: String): T? {
        return try {
            gson.fromJson(json, T::class.java)
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "JSON parse error: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }
}
