package com.chatai.app.domain.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val characterName: String? = null,
    val characterHeadshotUrl: String? = null,
    val isStreaming: Boolean = false
)
