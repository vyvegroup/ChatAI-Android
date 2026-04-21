package com.chatai.app

import android.app.Application
import android.util.Log
import com.chatai.app.di.AppContainer
import java.io.PrintWriter
import java.io.StringWriter

class ChatApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Install global uncaught exception handler
        installCrashHandler()
    }

    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Log the crash to file
            CrashLogger.logCrash(
                context = this,
                tag = "FATAL",
                message = "Uncaught exception on thread: ${thread.name}",
                throwable = throwable
            )

            // Also log to logcat
            Log.e("ChatAI", "═══ FATAL CRASH ═══")
            Log.e("ChatAI", "Thread: ${thread.name}")
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            Log.e("ChatAI", sw.toString())

            // Let the default handler continue (shows system crash dialog)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
