package com.chatai.app.ui.screens.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chatai.app.ChatApplication
import com.chatai.app.CrashLogger
import com.chatai.app.data.remote.AiModels
import com.chatai.app.data.remote.ImageApi
import com.chatai.app.data.repository.ChatRepository
import com.chatai.app.data.repository.StreamEvent
import com.chatai.app.domain.model.ChatMessage
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

// Data classes for parsed image tags
data class ImageTag(
    val type: String, // "image", "headshot", "gallery"
    val prompts: List<String>,
    val name: String? = null // for headshot
)

data class ParsedContent(
    val cleanContent: String,
    val headshotTag: ImageTag? = null,
    val imageTags: List<ImageTag> = emptyList(),
    val galleryTags: List<ImageTag> = emptyList()
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val appContext: Application get() = getApplication()
    private val repository: ChatRepository

    init {
        val app = application as ChatApplication
        repository = app.container.repository
    }

    // Global coroutine exception handler — catches ALL uncaught exceptions in viewModelScope
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Uncaught coroutine exception: ${throwable.javaClass.simpleName}: ${throwable.message}")
        CrashLogger.logCrash(
            context = appContext,
            tag = TAG,
            message = "Uncaught coroutine exception: ${throwable.message}",
            throwable = throwable
        )
        // Set error state so user sees something instead of silent crash
        _error.value = "Lỗi hệ thống: ${throwable.message}"
        _isLoading.value = false
        _isGeneratingImage.value = false
    }

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey

    private val _selectedModel = MutableStateFlow(AiModels.getDefaultModel())
    val selectedModel: StateFlow<com.chatai.app.data.remote.AiModel> = _selectedModel

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isGeneratingImage = MutableStateFlow(false)
    val isGeneratingImage: StateFlow<Boolean> = _isGeneratingImage

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _streamingMessageId = MutableStateFlow<String?>(null)
    val streamingMessageId: StateFlow<String?> = _streamingMessageId

    private val _showImageSheet = MutableStateFlow(false)
    val showImageSheet: StateFlow<Boolean> = _showImageSheet

    private val _showModelSheet = MutableStateFlow(false)
    val showModelSheet: StateFlow<Boolean> = _showModelSheet

    private var chatHistory = mutableListOf<ChatMessage>()

    init {
        _apiKey.value = "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"
    }

    fun setApiKey(key: String) { _apiKey.value = key }

    fun selectModel(model: com.chatai.app.data.remote.AiModel) {
        _selectedModel.value = model
        _showModelSheet.value = false
    }

    fun toggleImageSheet() { _showImageSheet.value = !_showImageSheet.value }
    fun dismissImageSheet() { _showImageSheet.value = false }

    fun toggleModelSheet() { _showModelSheet.value = !_showModelSheet.value }
    fun dismissModelSheet() { _showModelSheet.value = false }

    fun startNewChat() {
        viewModelScope.launch(exceptionHandler) {
            try {
                val conversation = repository.createConversation()
                _currentConversationId.value = conversation.id
                chatHistory.clear()
                _messages.value = emptyList()
                loadMessages(conversation.id)
            } catch (e: Exception) {
                CrashLogger.log(appContext, TAG, "startNewChat failed", e)
                _error.value = "Không thể tạo cuộc trò chuyện mới"
            }
        }
    }

    fun selectConversation(conversationId: String) {
        if (conversationId == _currentConversationId.value) return
        _currentConversationId.value = conversationId
        chatHistory.clear()
        loadMessages(conversationId)
    }

    private fun loadMessages(conversationId: String) {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.getMessages(conversationId).collect { messageList ->
                    _messages.value = messageList
                    chatHistory.clear()
                    chatHistory.addAll(messageList.filter { !it.isStreaming && it.role != "image" })
                }
            } catch (e: Exception) {
                CrashLogger.log(appContext, TAG, "loadMessages failed for $conversationId", e)
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        if (_apiKey.value.isBlank()) {
            _error.value = "Please set API key"
            return
        }

        viewModelScope.launch(exceptionHandler) {
            try {
                val conversationId = _currentConversationId.value
                if (conversationId == null) {
                    val conversation = repository.createConversation()
                    _currentConversationId.value = conversation.id
                    loadMessages(conversation.id)
                    sendMessageToApi(conversation.id, content)
                } else {
                    sendMessageToApi(conversationId, content)
                }
            } catch (e: Exception) {
                CrashLogger.log(appContext, TAG, "sendMessage failed", e)
                _error.value = "Không thể gửi tin nhắn: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private suspend fun sendMessageToApi(conversationId: String, content: String) {
        _isLoading.value = true
        _error.value = null

        try {
            repository.sendMessageStream(
                apiKey = apiKey.value,
                model = selectedModel.value.id,
                conversationId = conversationId,
                messages = chatHistory.filter { it.content.isNotBlank() && it.role != "image" },
                userMessage = content,
                modelName = selectedModel.value.name
            ).collect { event ->
                when (event) {
                    is StreamEvent.UserMessageSaved -> { }
                    is StreamEvent.AssistantMessageStarted -> {
                        _streamingMessageId.value = event.message.id
                    }
                    is StreamEvent.ContentDelta -> {
                        _messages.value = _messages.value.map { msg ->
                            if (msg.id == event.messageId) {
                                msg.copy(content = event.fullContent, isStreaming = true)
                            } else msg
                        }
                    }
                    is StreamEvent.StreamCompleted -> {
                        _streamingMessageId.value = null
                        _isLoading.value = false

                        try {
                            // Parse content for image generation tags
                            val parsed = parseImageTags(event.fullContent)
                            val hasImageTags = parsed.headshotTag != null || parsed.imageTags.isNotEmpty() || parsed.galleryTags.isNotEmpty()

                            if (hasImageTags) {
                                // Update assistant message with clean content (tags removed)
                                repository.updateMessageContent(event.messageId, parsed.cleanContent)
                                _messages.value = _messages.value.map { msg ->
                                    if (msg.id == event.messageId) {
                                        msg.copy(content = parsed.cleanContent, isStreaming = false)
                                    } else msg
                                }

                                // Process image tags in separate coroutine with full error protection
                                viewModelScope.launch(exceptionHandler) {
                                    try {
                                        processImageTags(conversationId, event.messageId, parsed)
                                    } catch (e: Exception) {
                                        CrashLogger.log(appContext, TAG, "processImageTags crashed", e)
                                        _error.value = "Lỗi tạo ảnh: ${e.message}"
                                        _isGeneratingImage.value = false
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            CrashLogger.log(appContext, TAG, "Error parsing image tags", e)
                            Log.e(TAG, "Image tag parse error (continuing normally): ${e.message}")
                        }

                        chatHistory.add(ChatMessage(
                            conversationId = conversationId,
                            role = "user",
                            content = content,
                            modelName = selectedModel.value.name
                        ))
                        chatHistory.add(ChatMessage(
                            conversationId = conversationId,
                            role = "assistant",
                            content = try { parseImageTags(event.fullContent).cleanContent } catch (_: Exception) { event.fullContent },
                            modelName = selectedModel.value.name
                        ))
                    }
                    is StreamEvent.StreamError -> {
                        _streamingMessageId.value = null
                        _isLoading.value = false
                        _error.value = event.error
                        CrashLogger.log(appContext, TAG, "Stream error", Exception(event.error))
                    }
                    else -> { }
                }
            }
        } catch (e: Exception) {
            _streamingMessageId.value = null
            _isLoading.value = false
            _isGeneratingImage.value = false
            CrashLogger.log(appContext, TAG, "sendMessageToApi failed", e)
            _error.value = "Lỗi kết nối: ${e.message}"
        }
    }

    /**
     * Parse the AI response content for image generation tags:
     * [IMAGE: prompt] - single image
     * [HEADSHOT: prompt] name - character portrait
     * [GALLERY: prompt1 | prompt2 | prompt3] - horizontal gallery
     */
    private fun parseImageTags(content: String): ParsedContent {
        try {
            val galleryTags = mutableListOf<ImageTag>()
            val headshotTag = mutableListOf<ImageTag>()
            val imageTags = mutableListOf<ImageTag>()
            var cleanContent = content

            // Parse [GALLERY: prompt1 | prompt2 | prompt3]
            val galleryPattern = Regex("\\[GALLERY:\\s*(.+?)\\]", RegexOption.DOT_MATCHES_ALL)
            galleryPattern.findAll(content).forEach { match ->
                val prompts = match.groupValues[1].split("\\|").map { it.trim() }.filter { it.isNotBlank() }
                if (prompts.isNotEmpty()) {
                    galleryTags.add(ImageTag(type = "gallery", prompts = prompts))
                }
                cleanContent = cleanContent.replace(match.value, "")
            }

            // Parse [HEADSHOT: prompt] name
            val headshotPattern = Regex("\\[HEADSHOT:\\s*(.+?)\\]\\s*([^\n]+)")
            headshotPattern.findAll(content).forEach { match ->
                headshotTag.add(ImageTag(
                    type = "headshot",
                    prompts = listOf(match.groupValues[1].trim()),
                    name = match.groupValues[2].trim()
                ))
                cleanContent = cleanContent.replace(match.value, match.groupValues[2].trim())
            }

            // Parse [IMAGE: prompt]
            val imagePattern = Regex("\\[IMAGE:\\s*(.+?)\\]", RegexOption.DOT_MATCHES_ALL)
            imagePattern.findAll(content).forEach { match ->
                imageTags.add(ImageTag(type = "image", prompts = listOf(match.groupValues[1].trim())))
                cleanContent = cleanContent.replace(match.value, "")
            }

            // Clean up extra whitespace and newlines
            cleanContent = cleanContent
                .replace(Regex("\\n{3,}"), "\n\n")
                .replace(Regex("^[\\s\\n]+"), "")
                .replace(Regex("[\\s\\n]+$"), "")
                .trim()

            return ParsedContent(
                cleanContent = cleanContent,
                headshotTag = headshotTag.firstOrNull(),
                imageTags = imageTags,
                galleryTags = galleryTags
            )
        } catch (e: Exception) {
            CrashLogger.log(appContext, TAG, "parseImageTags regex error", e)
            // Return unparsed content on regex failure
            return ParsedContent(cleanContent = content)
        }
    }

    private suspend fun processImageTags(conversationId: String, assistantMessageId: String, parsed: ParsedContent) {
        try {
            // Process headshot first (sequential)
            parsed.headshotTag?.let { tag ->
                generateHeadshot(conversationId, assistantMessageId, tag)
            }

            // Process standalone images (sequential to avoid race conditions)
            parsed.imageTags.forEach { tag ->
                try {
                    generateInlineImage(conversationId, tag)
                } catch (e: Exception) {
                    CrashLogger.log(appContext, TAG, "generateInlineImage failed for: ${tag.prompts.first()}", e)
                    _error.value = "Không thể tạo ảnh: ${e.message}"
                }
            }

            // Process galleries (sequential within gallery)
            parsed.galleryTags.forEach { tag ->
                try {
                    generateGallery(conversationId, tag)
                } catch (e: Exception) {
                    CrashLogger.log(appContext, TAG, "generateGallery failed", e)
                    _error.value = "Không thể tạo gallery: ${e.message}"
                }
            }
        } finally {
            _isGeneratingImage.value = false
        }
    }

    private suspend fun generateHeadshot(conversationId: String, assistantMessageId: String, tag: ImageTag) {
        _isGeneratingImage.value = true
        try {
            val prompt = tag.prompts.first()
            Log.d(TAG, "Generating headshot: $prompt")
            CrashLogger.log(appContext, TAG, "Generating headshot: $prompt")

            val result = ImageApi.generateImage(prompt)
            result.onSuccess { imageUrl ->
                // Update assistant message with headshot
                repository.updateAssistantMessage(
                    id = assistantMessageId,
                    content = _messages.value.find { it.id == assistantMessageId }?.content ?: "",
                    characterName = tag.name,
                    characterHeadshotUrl = imageUrl
                )
                _messages.value = _messages.value.map { msg ->
                    if (msg.id == assistantMessageId) {
                        msg.copy(characterName = tag.name, characterHeadshotUrl = imageUrl)
                    } else msg
                }
                Log.d(TAG, "Headshot generated: $imageUrl")
                CrashLogger.log(appContext, TAG, "Headshot success: $imageUrl")
            }.onFailure { e ->
                Log.e(TAG, "Headshot generation failed: ${e.message}")
                CrashLogger.log(appContext, TAG, "Headshot failed", e)
                _error.value = "Không thể tạo chân dung: ${e.message}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Headshot error: ${e.message}")
            CrashLogger.log(appContext, TAG, "Headshot crash", e)
            _error.value = "Lỗi tạo chân dung: ${e.message}"
        }
    }

    private suspend fun generateInlineImage(conversationId: String, tag: ImageTag) {
        val prompt = tag.prompts.first()
        _isGeneratingImage.value = true

        try {
            Log.d(TAG, "Generating inline image: $prompt")
            CrashLogger.log(appContext, TAG, "Generating inline image: $prompt")

            val baseTime = System.currentTimeMillis()
            val imageMsg = repository.generateImage(
                conversationId = conversationId,
                prompt = prompt,
                imageType = "standalone",
                timestamp = baseTime
            )
            _messages.value = _messages.value + imageMsg

            val result = ImageApi.generateImage(prompt)
            result.onSuccess { imageUrl ->
                repository.updateImageResult(imageMsg.id, imageUrl, "completed")
                _messages.value = _messages.value.map { msg ->
                    if (msg.id == imageMsg.id) {
                        msg.copy(imageUrl = imageUrl, imageStatus = "completed")
                    } else msg
                }
                Log.d(TAG, "Inline image generated: $imageUrl")
                CrashLogger.log(appContext, TAG, "Inline image success: $imageUrl")
            }.onFailure { e ->
                repository.updateImageResult(imageMsg.id, null, "failed")
                _messages.value = _messages.value.map { msg ->
                    if (msg.id == imageMsg.id) {
                        msg.copy(imageStatus = "failed")
                    } else msg
                }
                _error.value = "Tạo ảnh thất bại: ${e.message}"
                CrashLogger.log(appContext, TAG, "Inline image failed", e)
                Log.e(TAG, "Inline image failed: ${e.message}")
            }
        } catch (e: Exception) {
            CrashLogger.log(appContext, TAG, "Inline image CRASH", e)
            _error.value = "Lỗi tạo ảnh: ${e.message}"
            Log.e(TAG, "Inline image CRASH: ${e.message}", e)
        }
    }

    private suspend fun generateGallery(conversationId: String, tag: ImageTag) {
        _isGeneratingImage.value = true

        try {
            Log.d(TAG, "Generating gallery with ${tag.prompts.size} images")
            CrashLogger.log(appContext, TAG, "Generating gallery with ${tag.prompts.size} images")

            val galleryId = UUID.randomUUID().toString()
            val baseTime = System.currentTimeMillis()

            // Create all gallery image messages at once (all in "generating" state)
            val galleryMessages = mutableListOf<ChatMessage>()
            tag.prompts.forEachIndexed { index, prompt ->
                try {
                    val imageMsg = repository.generateImage(
                        conversationId = conversationId,
                        prompt = prompt,
                        imageType = "gallery",
                        galleryId = galleryId,
                        timestamp = baseTime + index
                    )
                    galleryMessages.add(imageMsg)
                } catch (e: Exception) {
                    CrashLogger.log(appContext, TAG, "Failed to create gallery message $index", e)
                }
            }

            if (galleryMessages.isNotEmpty()) {
                _messages.value = _messages.value + galleryMessages
            }

            // Generate images SEQUENTIALLY to avoid race conditions on _messages
            tag.prompts.forEachIndexed { index, prompt ->
                if (index >= galleryMessages.size) return@forEachIndexed

                val msgId = galleryMessages[index].id
                try {
                    val result = ImageApi.generateImage(prompt)
                    result.onSuccess { imageUrl ->
                        repository.updateImageResult(msgId, imageUrl, "completed")
                        _messages.value = _messages.value.map { msg ->
                            if (msg.id == msgId) {
                                msg.copy(imageUrl = imageUrl, imageStatus = "completed")
                            } else msg
                        }
                    }.onFailure { e ->
                        repository.updateImageResult(msgId, null, "failed")
                        _messages.value = _messages.value.map { msg ->
                            if (msg.id == msgId) {
                                msg.copy(imageStatus = "failed")
                            } else msg
                        }
                        CrashLogger.log(appContext, TAG, "Gallery image $index failed", e)
                        Log.e(TAG, "Gallery image $index failed: ${e.message}")
                    }
                } catch (e: Exception) {
                    try {
                        repository.updateImageResult(msgId, null, "failed")
                        _messages.value = _messages.value.map { msg ->
                            if (msg.id == msgId) {
                                msg.copy(imageStatus = "failed")
                            } else msg
                        }
                    } catch (dbErr: Exception) {
                        CrashLogger.log(appContext, TAG, "Failed to update gallery image $index status after crash", dbErr)
                    }
                    CrashLogger.log(appContext, TAG, "Gallery image $index CRASH", e)
                    Log.e(TAG, "Gallery image $index CRASH: ${e.message}", e)
                }
            }

            Log.d(TAG, "Gallery generation completed")
        } catch (e: Exception) {
            CrashLogger.log(appContext, TAG, "generateGallery CRASH", e)
            _error.value = "Lỗi tạo gallery: ${e.message}"
            Log.e(TAG, "generateGallery CRASH: ${e.message}", e)
        }
    }

    fun generateImage(prompt: String, width: Int = 1024, height: Int = 1024) {
        if (prompt.isBlank()) return

        // Dismiss sheet first to prevent crash
        _showImageSheet.value = false
        _isGeneratingImage.value = true

        viewModelScope.launch(exceptionHandler) {
            try {
                // Small delay to let the sheet dismiss animation complete
                delay(300)

                val conversationId = _currentConversationId.value
                if (conversationId == null) {
                    val conversation = repository.createConversation()
                    _currentConversationId.value = conversation.id
                    loadMessages(conversation.id)
                    doGenerateImage(conversation.id, prompt, width, height)
                } else {
                    doGenerateImage(conversationId, prompt, width, height)
                }
            } catch (e: Exception) {
                CrashLogger.log(appContext, TAG, "generateImage CRASH", e)
                _error.value = "Lỗi tạo ảnh: ${e.message}"
                _isGeneratingImage.value = false
                Log.e(TAG, "generateImage CRASH: ${e.message}", e)
            }
        }
    }

    private suspend fun doGenerateImage(conversationId: String, prompt: String, width: Int, height: Int) {
        _error.value = null

        try {
            // Add placeholder
            val imageMsg = repository.generateImage(conversationId, prompt)
            _messages.value = _messages.value + imageMsg

            val result = ImageApi.generateImage(prompt, width, height)
            result.onSuccess { imageUrl ->
                repository.updateImageResult(imageMsg.id, imageUrl, "completed")
                _messages.value = _messages.value.map { msg ->
                    if (msg.id == imageMsg.id) {
                        msg.copy(imageUrl = imageUrl, imageStatus = "completed")
                    } else msg
                }
                chatHistory.add(imageMsg.copy(imageUrl = imageUrl, imageStatus = "completed"))
                CrashLogger.log(appContext, TAG, "Sheet image success: $imageUrl")
            }.onFailure { error ->
                repository.updateImageResult(imageMsg.id, null, "failed")
                _messages.value = _messages.value.map { msg ->
                    if (msg.id == imageMsg.id) {
                        msg.copy(imageStatus = "failed")
                    } else msg
                }
                _error.value = "Tạo ảnh thất bại: ${error.message}"
                CrashLogger.log(appContext, TAG, "Sheet image failed", error)
            }
        } catch (e: Exception) {
            CrashLogger.log(appContext, TAG, "doGenerateImage CRASH", e)
            _error.value = "Lỗi tạo ảnh: ${e.message}"
            Log.e(TAG, "doGenerateImage CRASH: ${e.message}", e)
        }

        _isGeneratingImage.value = false
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.deleteConversation(id)
                if (id == _currentConversationId.value) {
                    _currentConversationId.value = null
                    _messages.value = emptyList()
                    chatHistory.clear()
                }
            } catch (e: Exception) {
                CrashLogger.log(appContext, TAG, "deleteConversation failed", e)
            }
        }
    }

    fun clearError() { _error.value = null }

    override fun onCleared() {
        super.onCleared()
        CrashLogger.log(appContext, TAG, "ViewModel cleared")
    }
}
