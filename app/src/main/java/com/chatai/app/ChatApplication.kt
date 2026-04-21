package com.chatai.app

import android.app.Application
import com.chatai.app.di.AppContainer

class ChatApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
