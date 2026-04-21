package com.chatai.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    val model: String,
    val messages: List<MessageDto>,
    val stream: Boolean = true
)

data class MessageDto(
    val role: String,
    val content: String
)

data class StreamChunk(
    val id: String? = null,
    val choices: List<ChunkChoice>? = null,
    val usage: UsageInfo? = null
)

data class ChunkChoice(
    val index: Int = 0,
    val delta: DeltaContent? = null,
    val finish_reason: String? = null
)

data class DeltaContent(
    val role: String? = null,
    val content: String? = null
)

data class UsageInfo(
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null
)

data class ErrorResponse(
    val error: ErrorDetail? = null
)

data class ErrorDetail(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)
