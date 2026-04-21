package com.chatai.app.ui.screens.conversations

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chatai.app.ChatApplication
import com.chatai.app.domain.model.Conversation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ConversationListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as ChatApplication).container.repository

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations

    init {
        viewModelScope.launch {
            repository.getConversations().collect { list ->
                _conversations.value = list
            }
        }
    }
}
