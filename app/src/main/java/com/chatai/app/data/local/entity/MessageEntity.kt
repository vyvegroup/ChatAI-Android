package com.chatai.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chatai.app.domain.model.ChatMessage

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val imageUrl: String? = null,
    val characterName: String? = null,
    val characterHeadshotUrl: String? = null
) {
    fun toDomainModel(): ChatMessage = ChatMessage(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        timestamp = timestamp,
        imageUrl = imageUrl,
        characterName = characterName,
        characterHeadshotUrl = characterHeadshotUrl
    )

    companion object {
        fun fromDomainModel(message: ChatMessage): MessageEntity = MessageEntity(
            id = message.id,
            conversationId = message.conversationId,
            role = message.role,
            content = message.content,
            timestamp = message.timestamp,
            imageUrl = message.imageUrl,
            characterName = message.characterName,
            characterHeadshotUrl = message.characterHeadshotUrl
        )
    }
}
