package com.chatai.app.ui.screens.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chatai.app.ChatApplication
import com.chatai.app.data.repository.ChatRepository
import com.chatai.app.data.repository.StreamEvent
import com.chatai.app.domain.model.ChatMessage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository

    init {
        val app = application as ChatApplication
        repository = app.container.repository
    }

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey

    private val _selectedModel = MutableStateFlow("google/gemma-4-31b-it:free")
    val selectedModel: StateFlow<String> = _selectedModel

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _streamingMessageId = MutableStateFlow<String?>(null)
    val streamingMessageId: StateFlow<String?> = _streamingMessageId

    private var chatHistory = mutableListOf<ChatMessage>()

    init {
        // Load API key from preferences (simplified - in real app use DataStore)
        _apiKey.value = "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"
    }

    fun setApiKey(key: String) {
        _apiKey.value = key
    }

    fun setModel(model: String) {
        _selectedModel.value = model
    }

    fun startNewChat() {
        viewModelScope.launch {
            val conversation = repository.createConversation()
            _currentConversationId.value = conversation.id
            chatHistory.clear()
            _messages.value = emptyList()
            loadMessages(conversation.id)
        }
    }

    fun selectConversation(conversationId: String) {
        if (conversationId == _currentConversationId.value) return
        _currentConversationId.value = conversationId
        chatHistory.clear()
        loadMessages(conversationId)
    }

    private fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            repository.getMessages(conversationId).collect { messageList ->
                _messages.value = messageList
                chatHistory.clear()
                chatHistory.addAll(messageList.filter { !it.isStreaming })
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        if (_apiKey.value.isBlank()) {
            _error.value = "Please set API key in settings"
            return
        }

        viewModelScope.launch {
            val conversationId = _currentConversationId.value
            if (conversationId == null) {
                // Create new conversation
                val conversation = repository.createConversation()
                _currentConversationId.value = conversation.id
                loadMessages(conversation.id)
                // Re-call with new id
                sendMessageToApi(conversation.id, content)
            } else {
                sendMessageToApi(conversationId, content)
            }
        }
    }

    private suspend fun sendMessageToApi(conversationId: String, content: String) {
        _isLoading.value = true
        _error.value = null

        repository.sendMessageStream(
            apiKey = apiKey.value,
            model = selectedModel.value,
            conversationId = conversationId,
            messages = chatHistory.filter { it.content.isNotBlank() },
            userMessage = content
        ).collect { event ->
            when (event) {
                is StreamEvent.UserMessageSaved -> {
                    // User message saved
                }
                is StreamEvent.AssistantMessageStarted -> {
                    _streamingMessageId.value = event.message.id
                }
                is StreamEvent.ContentDelta -> {
                    // Update streaming message in list
                    _messages.value = _messages.value.map { msg ->
                        if (msg.id == event.messageId) {
                            msg.copy(content = event.fullContent, isStreaming = true)
                        } else {
                            msg
                        }
                    }
                }
                is StreamEvent.StreamCompleted -> {
                    _streamingMessageId.value = null
                    _isLoading.value = false
                    // Add to history
                    chatHistory.add(ChatMessage(
                        conversationId = conversationId,
                        role = "assistant",
                        content = event.fullContent
                    ))
                    chatHistory.add(ChatMessage(
                        conversationId = conversationId,
                        role = "user",
                        content = content
                    ))
                }
                is StreamEvent.StreamError -> {
                    _streamingMessageId.value = null
                    _isLoading.value = false
                    _error.value = event.error
                }
            }
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            repository.deleteConversation(id)
            if (id == _currentConversationId.value) {
                _currentConversationId.value = null
                _messages.value = emptyList()
                chatHistory.clear()
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
