package com.chatai.app.data.local.dao

import androidx.room.*
import com.chatai.app.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesByConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE imageUrl IS NOT NULL AND imageStatus = 'completed' ORDER BY timestamp DESC")
    fun getAllCompletedImages(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesByConversation(conversationId: String)

    @Query("UPDATE messages SET content = :content WHERE id = :id")
    suspend fun updateMessageContent(id: String, content: String)

    @Query("UPDATE messages SET imageUrl = :url, imageStatus = :status WHERE id = :id")
    suspend fun updateImageResult(id: String, url: String?, status: String)

    @Query("UPDATE messages SET content = :content, characterName = :name, characterHeadshotUrl = :headshotUrl WHERE id = :id")
    suspend fun updateAssistantMessage(id: String, content: String, name: String?, headshotUrl: String?)

    @Query("SELECT * FROM messages WHERE imageUrl IS NOT NULL AND imageStatus = 'completed' ORDER BY timestamp DESC")
    suspend fun getAllCompletedImagesOnce(): List<MessageEntity>
}
