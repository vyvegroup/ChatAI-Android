package com.chatai.app

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Saves crash/error logs to app-private external storage.
 * NO permissions needed. Accessible at:
 *   /storage/emulated/0/Android/data/com.chatai.app/files/logs/
 */
object CrashLogger {

    private const val TAG = "CrashLogger"

    fun log(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        try {
            val dir = getLogDir(context) ?: return
            if (!dir.exists()) dir.mkdirs()

            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val file = File(dir, "chatai_log_$date.txt")

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            val sb = StringBuilder()
            sb.appendLine("═══════════════════════════════════════")
            sb.appendLine("TIME: $timestamp")
            sb.appendLine("TAG:  $tag")
            sb.appendLine("MSG:  $message")

            if (throwable != null) {
                sb.appendLine()
                sb.appendLine("EXCEPTION: ${throwable.javaClass.simpleName}: ${throwable.message}")
                sb.appendLine()
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                sb.appendLine(sw.toString())
                var cause = throwable.cause
                var depth = 1
                while (cause != null && depth <= 5) {
                    sb.appendLine("── Caused by ($depth) ──")
                    sb.appendLine("${cause.javaClass.simpleName}: ${cause.message}")
                    val csw = StringWriter()
                    cause.printStackTrace(PrintWriter(csw))
                    sb.appendLine(csw.toString())
                    cause = cause.cause
                    depth++
                }
            }
            sb.appendLine()

            file.appendText(sb.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log: ${e.message}")
        }
    }

    fun logCrash(context: Context, tag: String, message: String, throwable: Throwable) {
        try {
            val dir = getLogDir(context) ?: return
            if (!dir.exists()) dir.mkdirs()

            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val time = SimpleDateFormat("HH-mm-ss", Locale.US).format(Date())
            val crashFile = File(dir, "CRASH_${date}_${time}.txt")

            val sb = StringBuilder()
            sb.appendLine("══════════════════════════════════════════")
            sb.appendLine("         CRASH LOG - ChatAI Android")
            sb.appendLine("══════════════════════════════════════════")
            sb.appendLine()
            sb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            sb.appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            sb.appendLine("App: ${getAppVersion(context)}")
            sb.appendLine("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())}")
            sb.appendLine("Tag: $tag")
            sb.appendLine("Message: $message")
            sb.appendLine()
            sb.appendLine("═══ STACK TRACE ═══")
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            sb.appendLine(sw.toString())

            var cause = throwable.cause
            var depth = 1
            while (cause != null && depth <= 10) {
                sb.appendLine("═══ CAUSE $depth ═══")
                sb.appendLine("${cause.javaClass.simpleName}: ${cause.message}")
                val csw = StringWriter()
                cause.printStackTrace(PrintWriter(csw))
                sb.appendLine(csw.toString())
                cause = cause.cause
                depth++
            }

            sb.appendLine("═══ THREAD INFO ═══")
            sb.appendLine("Thread: ${Thread.currentThread().name}")

            crashFile.writeText(sb.toString())
            log(context, "CRASH", "$tag: $message", throwable)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash log: ${e.message}")
        }
    }

    /**
     * Uses app-private external files dir.
     * NO permissions needed. Path: /Android/data/com.chatai.app/files/logs/
     */
    fun getLogDir(context: Context): File? {
        return try {
            val dir = File(context.getExternalFilesDir(null), "logs")
            dir
        } catch (e: Exception) {
            Log.e(TAG, "getLogDir failed: ${e.message}")
            try {
                File(context.filesDir, "logs")
            } catch (e2: Exception) {
                null
            }
        }
    }

    fun getLogDirPath(context: Context): String {
        return getLogDir(context)?.absolutePath ?: "N/A"
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pInfo.versionName} (${pInfo.versionCode})"
        } catch (e: Exception) {
            "unknown"
        }
    }
}
