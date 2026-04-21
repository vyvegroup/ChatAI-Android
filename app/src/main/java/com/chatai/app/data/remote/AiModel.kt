package com.chatai.app.data.remote

data class AiModel(
    val id: String,
    val name: String,
    val provider: String,
    val contextLength: Int,
    val description: String
)
