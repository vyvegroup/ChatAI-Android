package com.chatai.app.domain.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val role: String, // "user", "assistant", "image"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val imageGenerationId: String? = null,
    val imageStatus: String? = null, // "generating", "completed", "failed"
    val characterName: String? = null,
    val characterHeadshotUrl: String? = null,
    val isStreaming: Boolean = false,
    val modelName: String? = null
)
