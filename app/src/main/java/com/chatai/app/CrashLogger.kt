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
 * Utility to log crashes and errors to a persistent text file on device storage.
 * All logs are saved to: /storage/emulated/0/Download/ChatAI_logs/
 *
 * Usage:
 *   CrashLogger.log(context, tag, message, throwable?)
 *   CrashLogger.logError(context, tag, message, throwable?)
 */
object CrashLogger {

    private const val DIR_NAME = "ChatAI_logs"
    private const val TAG = "CrashLogger"

    /**
     * Log an error with optional throwable to a text file.
     * Creates a new file per session, appends entries.
     */
    fun log(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        try {
            val dir = getLogDir(context) ?: return
            if (!dir.exists()) dir.mkdirs()

            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val fileName = "chatai_log_$date.txt"
            val file = File(dir, fileName)

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
                // Full stack trace
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                sb.appendLine(sw.toString())
                // Also log the cause chain
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

            // Append to file
            file.appendText(sb.toString())

            Log.d(TAG, "Log written to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log: ${e.message}")
        }
    }

    /**
     * Log a crash-level error (fatal exceptions).
     * Creates a separate crash file for easy identification.
     */
    fun logCrash(context: Context, tag: String, message: String, throwable: Throwable) {
        try {
            val dir = getLogDir(context) ?: return
            if (!dir.exists()) dir.mkdirs()

            // Write to crash-specific file
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val time = SimpleDateFormat("HH-mm-ss", Locale.US).format(Date())
            val crashFileName = "CRASH_${date}_${time}.txt"
            val crashFile = File(dir, crashFileName)

            val sb = StringBuilder()
            sb.appendLine("╔══════════════════════════════════════════╗")
            sb.appendLine("║         CRASH LOG - ChatAI Android       ║")
            sb.appendLine("╚══════════════════════════════════════════╝")
            sb.appendLine()
            sb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            sb.appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            sb.appendLine("App Version: ${getAppVersion(context)}")
            sb.appendLine("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())}")
            sb.appendLine("Tag: $tag")
            sb.appendLine("Message: $message")
            sb.appendLine()
            sb.appendLine("═══ STACK TRACE ═══")
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            sb.appendLine(sw.toString())

            // Cause chain
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

            // Thread info
            sb.appendLine("═══ THREAD INFO ═══")
            sb.appendLine("Thread: ${Thread.currentThread().name}")
            sb.appendLine("Active Threads: ${Thread.getAllStackTraces().keys.size}")

            crashFile.writeText(sb.toString())

            // Also append to daily log
            log(context, "CRASH", "$tag: $message", throwable)

            Log.e(TAG, "Crash log written to ${crashFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash log: ${e.message}")
        }
    }

    /**
     * Get the log directory (Download/ChatAI_logs/).
     * Tries external storage first, falls back to internal.
     */
    private fun getLogDir(context: Context): File? {
        return try {
            // Try external downloads directory first (user can access it easily)
            val downloadsDir = File(
                android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                ), DIR_NAME
            )
            if (downloadsDir.canWrite() || downloadsDir.mkdirs()) {
                downloadsDir
            } else {
                // Fallback to app internal storage
                File(context.filesDir, DIR_NAME)
            }
        } catch (e: Exception) {
            // Final fallback
            File(context.filesDir, DIR_NAME)
        }
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pInfo.versionName} (${pInfo.versionCode})"
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Get the log file path for display to user.
     */
    fun getLogDirPath(context: Context): String {
        val dir = getLogDir(context)
        return dir?.absolutePath ?: "N/A"
    }

    /**
     * Read the latest log file content.
     */
    fun getLatestLog(context: Context): String {
        return try {
            val dir = getLogDir(context) ?: return "No log directory available"
            val files = dir.listFiles { f -> f.name.endsWith(".txt") }
                ?.sortedByDescending { it.lastModified() }
            if (files.isNullOrEmpty()) return "No logs found"
            files.first().readText()
        } catch (e: Exception) {
            "Error reading log: ${e.message}"
        }
    }
}
