package com.chatai.app.data.repository

import com.chatai.app.data.local.ChatDatabase
import com.chatai.app.data.local.dao.ConversationDao
import com.chatai.app.data.local.dao.MessageDao
import com.chatai.app.data.local.entity.ConversationEntity
import com.chatai.app.data.local.entity.MessageEntity
import com.chatai.app.data.remote.OpenRouterApi
import com.chatai.app.data.remote.dto.MessageDto
import com.chatai.app.domain.model.ChatMessage
import com.chatai.app.domain.model.Conversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.util.UUID

class ChatRepository(
    private val database: ChatDatabase,
    private val api: OpenRouterApi
) {
    private val conversationDao: ConversationDao = database.conversationDao()
    private val messageDao: MessageDao = database.messageDao()

    // System prompt that teaches AI how to generate images
    val systemPrompt = """You are ChatAI, a helpful and friendly AI assistant with image generation capabilities. You can create images, character portraits, and photo galleries.

## Image Generation Commands

You have special commands to generate visual content. Use them when the user asks for images, drawings, portraits, or visual content.

### Generate a single image
When the user asks to generate, draw, create, or make an image, use:
[IMAGE: detailed description of what to generate]

Example response:
"Here is a beautiful sunset painting for you!
[IMAGE: a breathtaking golden sunset over calm ocean waters, orange and purple sky, photorealistic style, 4k]"

### Character Headshot Portrait
When discussing a character, famous person, or entity and the user wants to see them, use:
[HEADSHOT: detailed visual description of the person/character] Display Name

Example response:
"[HEADSHOT: Albert Einstein portrait, wild white hair, prominent mustache, wearing dark suit, thoughtful expression, black and white photograph style] Albert Einstein was a German-born theoretical physicist who developed the theory of relativity."

### Image Gallery (multiple images shown horizontally)
When showing multiple images on a topic, use:
[GALLERY: description1 | description2 | description3]

Example response:
"Here are some popular dog breeds:
[GALLERY: a golden retriever playing fetch in a sunny green park | a noble german shepherd standing alert with pointed ears | a white poodle with elaborate show haircut at a dog competition]
Each breed has unique characteristics..."

## Important Rules
1. Only use [IMAGE:], [HEADSHOT:], [GALLERY:] tags when the user EXPLICITLY asks for images, drawings, portraits, or visual content
2. Always be very detailed and specific in image descriptions for best results
3. You can include multiple tags in a single response
4. [IMAGE:] generates one standalone image
5. [HEADSHOT:] generates a small circular portrait next to the name
6. [GALLERY:] generates multiple images shown side by side horizontally that the user can swipe through
7. Always respond in the SAME LANGUAGE the user uses
8. Be creative, helpful, and thorough in your responses"""

    // Conversations
    fun getConversations(): Flow<List<Conversation>> =
        conversationDao.getAllConversations()
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)

    suspend fun createConversation(title: String = "New Chat"): Conversation {
        val conversation = Conversation(title = title)
        conversationDao.insertConversation(ConversationEntity.fromDomainModel(conversation))
        return conversation
    }

    suspend fun updateConversationTitle(id: String, title: String) {
        val entity = conversationDao.getConversationById(id) ?: return
        conversationDao.updateConversation(entity.copy(title = title, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteConversation(id: String) {
        conversationDao.deleteConversationById(id)
        messageDao.deleteMessagesByConversation(id)
    }

    // Messages
    fun getMessages(conversationId: String): Flow<List<ChatMessage>> =
        messageDao.getMessagesByConversation(conversationId)
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)

    private suspend fun saveMessage(message: ChatMessage) {
        messageDao.insertMessage(MessageEntity.fromDomainModel(message))
    }

    private suspend fun updateMessageContent(id: String, content: String) {
        messageDao.updateMessageContent(id, content)
    }

    suspend fun updateImageResult(id: String, imageUrl: String?, imageStatus: String) {
        messageDao.updateImageResult(id, imageUrl, imageStatus)
    }

    suspend fun updateAssistantMessage(id: String, content: String, characterName: String? = null, characterHeadshotUrl: String? = null) {
        messageDao.updateAssistantMessage(id, content, characterName, characterHeadshotUrl)
    }

    // Image generation placeholder message
    suspend fun generateImage(conversationId: String, prompt: String, imageType: String = "standalone", galleryId: String? = null, timestamp: Long = System.currentTimeMillis()): ChatMessage {
        val imageMsg = ChatMessage(
            conversationId = conversationId,
            role = "image",
            content = prompt,
            imageStatus = "generating",
            imageGenerationId = UUID.randomUUID().toString(),
            imageType = imageType,
            galleryId = galleryId,
            timestamp = timestamp
        )
        saveMessage(imageMsg)

        // Update conversation timestamp
        val convEntity = conversationDao.getConversationById(conversationId)
        if (convEntity != null) {
            conversationDao.updateConversation(
                convEntity.copy(updatedAt = System.currentTimeMillis())
            )
        }

        return imageMsg
    }

    fun sendMessageStream(
        apiKey: String,
        model: String,
        conversationId: String,
        messages: List<ChatMessage>,
        userMessage: String,
        modelName: String? = null
    ): Flow<StreamEvent> = flow {
        // Save user message
        val userMsg = ChatMessage(
            conversationId = conversationId,
            role = "user",
            content = userMessage,
            modelName = modelName
        )
        saveMessage(userMsg)
        emit(StreamEvent.UserMessageSaved(userMsg))

        // Update conversation timestamp
        val convEntity = conversationDao.getConversationById(conversationId)
        if (convEntity != null) {
            conversationDao.updateConversation(
                convEntity.copy(updatedAt = System.currentTimeMillis())
            )
        }

        // Create assistant message placeholder
        val assistantMsg = ChatMessage(
            conversationId = conversationId,
            role = "assistant",
            content = "",
            isStreaming = true,
            modelName = modelName
        )
        saveMessage(assistantMsg)
        emit(StreamEvent.AssistantMessageStarted(assistantMsg))

        // Build API messages: system prompt + history + user message
        val apiMessages = mutableListOf<MessageDto>()
        apiMessages.add(MessageDto(role = "system", content = systemPrompt))

        // Add conversation history (filter out image-only messages)
        val historyMessages = messages.filter {
            it.role == "user" || it.role == "assistant"
        }.takeLast(20) // Keep last 20 messages for context

        for (msg in historyMessages) {
            val role = if (msg.role == "assistant") "assistant" else "user"
            apiMessages.add(MessageDto(role = role, content = msg.content))
        }
        apiMessages.add(MessageDto(role = "user", content = userMessage))

        var fullContent = ""
        try {
            api.sendMessageStream(apiKey, model, apiMessages).collect { chunk ->
                fullContent += chunk
                emit(StreamEvent.ContentDelta(assistantMsg.id, chunk, fullContent))
            }

            // Save final content
            updateMessageContent(assistantMsg.id, fullContent)
            emit(StreamEvent.StreamCompleted(assistantMsg.id, fullContent))

            // Auto-generate title from first message if conversation is new
            if (convEntity?.title == "New Chat" && messages.isEmpty()) {
                val title = if (userMessage.length > 40) userMessage.substring(0, 40) + "..." else userMessage
                updateConversationTitle(conversationId, title)
            }
        } catch (e: Exception) {
            emit(StreamEvent.StreamError(assistantMsg.id, e.message ?: "Unknown error"))
            if (fullContent.isNotEmpty()) {
                updateMessageContent(assistantMsg.id, fullContent)
            }
        }
    }.flowOn(Dispatchers.IO)
}

sealed class StreamEvent {
    data class UserMessageSaved(val message: ChatMessage) : StreamEvent()
    data class AssistantMessageStarted(val message: ChatMessage) : StreamEvent()
    data class ContentDelta(val messageId: String, val delta: String, val fullContent: String) : StreamEvent()
    data class StreamCompleted(val messageId: String, val fullContent: String) : StreamEvent()
    data class StreamError(val messageId: String, val error: String) : StreamEvent()
    data class ImageGenerated(val messageId: String, val imageUrl: String) : StreamEvent()
    data class ImageFailed(val messageId: String, val error: String) : StreamEvent()
}
