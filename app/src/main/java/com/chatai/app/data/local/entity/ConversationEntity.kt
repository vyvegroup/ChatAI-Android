package com.chatai.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chatai.app.domain.model.Conversation

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomainModel(): Conversation = Conversation(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomainModel(conversation: Conversation): ConversationEntity = ConversationEntity(
            id = conversation.id,
            title = conversation.title,
            createdAt = conversation.createdAt,
            updatedAt = conversation.updatedAt
        )
    }
}
