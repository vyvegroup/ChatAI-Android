package com.chatai.app

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Downloads image from URL and saves to device gallery.
 * Uses MediaStore on Android 10+ (no permission needed).
 * Falls back to direct file write on older versions.
 */
object ImageSaver {

    private const val TAG = "ImageSaver"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Save an image from URL to the device gallery.
     * Returns Result with the file path or error message.
     */
    suspend fun saveImage(context: Context, imageUrl: String, displayName: String): Result<String> {
        return try {
            // Download image bytes
            val request = Request.Builder().url(imageUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(Exception("Download failed: HTTP ${response.code}"))
            }

            val bytes = response.body?.bytes()
            if (bytes == null || bytes.isEmpty()) {
                return Result.failure(Exception("Empty image data"))
            }

            // Save using MediaStore (Android 10+) or direct file
            val fileName = "ChatAI_${System.currentTimeMillis()}.png"
            val savedPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(context, bytes, fileName, displayName)
            } else {
                saveDirectly(context, bytes, fileName)
            }

            Result.success(savedPath)
        } catch (e: Exception) {
            Log.e(TAG, "Save failed", e)
            CrashLogger.log(context, TAG, "Save image failed: $imageUrl", e)
            Result.failure(e)
        }
    }

    @Suppress("DEPRECATION")
    private fun saveViaMediaStore(context: Context, bytes: ByteArray, fileName: String, displayName: String): String {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.TITLE, displayName)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ChatAI")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw Exception("Failed to create MediaStore entry")

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(bytes)
            outputStream.flush()
        } ?: throw Exception("Failed to open output stream")

        // Mark as complete
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        context.contentResolver.update(uri, values, null, null)

        return uri.toString()
    }

    @Suppress("DEPRECATION")
    private fun saveDirectly(context: Context, bytes: ByteArray, fileName: String): String {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ChatAI")
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, fileName)
        FileOutputStream(file).use { it.write(bytes) }

        // Notify MediaScanner
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DATA, file.absolutePath)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.TITLE, fileName)
        }
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        return file.absolutePath
    }
}
