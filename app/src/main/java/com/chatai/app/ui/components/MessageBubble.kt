package com.chatai.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chatai.app.domain.model.ChatMessage
import com.chatai.app.ui.theme.ChatColors

@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar
        AvatarSection(message = message, isUser = isUser)

        // Content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Name with character headshot
            NameSection(message = message, isUser = isUser)

            // Message content
            when {
                message.isStreaming && message.content.isEmpty() -> {
                    // Loading indicator
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = ChatColors.Accent,
                        strokeWidth = 2.dp
                    )
                }
                message.isStreaming || !isUser -> {
                    // Markdown content for assistant
                    MarkdownContent(
                        markdown = if (message.content.endsWith("▊")) {
                            message.content.dropLast(1)
                        } else {
                            message.content
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Streaming cursor
                    if (message.isStreaming) {
                        Surface(
                            modifier = Modifier.padding(top = 4.dp).size(8.dp, 20.dp),
                            color = ChatColors.Accent,
                            shape = RoundedCornerShape(2.dp)
                        ) {}
                    }
                }
                isUser -> {
                    // Simple text for user
                    Text(
                        text = message.content,
                        color = ChatColors.TextPrimary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Image
            message.imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Chat image",
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .clip(RoundedCornerShape(12.dp))
                        .padding(top = 8.dp),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }
}

@Composable
private fun AvatarSection(message: ChatMessage, isUser: Boolean) {
    // If character has headshot, show it
    if (!isUser && message.characterHeadshotUrl != null) {
        AsyncImage(
            model = message.characterHeadshotUrl,
            contentDescription = message.characterName ?: "Assistant",
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = if (isUser) ChatColors.UserAvatarBg else ChatColors.AssistantAvatarBg
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = if (isUser) "You" else "AI",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun NameSection(message: ChatMessage, isUser: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (isUser) "You" else (message.characterName ?: "ChatAI"),
            color = ChatColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = MaterialTheme.typography.titleSmall.fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Character headshot thumbnail next to name
        if (!isUser && message.characterHeadshotUrl != null && message.characterName != null) {
            AsyncImage(
                model = message.characterHeadshotUrl,
                contentDescription = message.characterName,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
    }
}
