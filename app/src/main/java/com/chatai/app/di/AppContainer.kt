package com.chatai.app.di

import android.content.Context
import com.chatai.app.data.local.ChatDatabase
import com.chatai.app.data.remote.OpenRouterApi
import com.chatai.app.data.repository.ChatRepository

class AppContainer(private val context: Context) {
    val database: ChatDatabase = ChatDatabase.getDatabase(context)
    val api: OpenRouterApi = OpenRouterApi()
    val repository: ChatRepository = ChatRepository(database, api)
}
