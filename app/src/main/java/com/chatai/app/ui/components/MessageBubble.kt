package com.chatai.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
    when (message.role) {
        "image" -> ImageMessage(message, modifier)
        "user" -> UserMessage(message, modifier)
        "assistant" -> AssistantMessage(message, modifier)
        else -> AssistantMessage(message, modifier)
    }
}

@Composable
private fun ImageMessage(message: ChatMessage, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = ChatColors.AssistantAvatarBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = "ChatAI",
                color = ChatColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        when {
            message.imageStatus == "generating" -> {
                ImageGeneratingPlaceholder(prompt = message.content)
            }
            message.imageUrl != null -> {
                GeneratedImageCard(
                    imageUrl = message.imageUrl,
                    prompt = message.content,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }
            message.imageStatus == "failed" -> {
                Surface(
                    color = ChatColors.SurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text(
                        text = "Failed to generate image",
                        color = ChatColors.Error,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UserMessage(message: ChatMessage, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = ChatColors.UserAvatarBg
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "You",
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "You",
                color = ChatColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = message.content,
                color = ChatColors.TextPrimary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AssistantMessage(message: ChatMessage, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar - use headshot if available
        if (message.characterHeadshotUrl != null) {
            AsyncImage(
                model = message.characterHeadshotUrl,
                contentDescription = message.characterName ?: "Assistant",
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape),
            )
        } else {
            Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = ChatColors.AssistantAvatarBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "AI",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Name row with optional inline headshot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // If character headshot is available, show it inline next to the name
                if (message.characterHeadshotUrl != null && message.characterName != null) {
                    AsyncImage(
                        model = message.characterHeadshotUrl,
                        contentDescription = message.characterName,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape),
                    )
                }

                Text(
                    text = message.characterName ?: "ChatAI",
                    color = ChatColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                if (message.modelName != null) {
                    Surface(
                        color = ChatColors.SurfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = message.modelName,
                            color = ChatColors.TextTertiary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Content
            when {
                message.isStreaming && message.content.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = ChatColors.Accent,
                        strokeWidth = 2.dp
                    )
                }
                else -> {
                    MarkdownContent(
                        markdown = message.content,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (message.isStreaming) {
                        Surface(
                            modifier = Modifier.padding(top = 2.dp).size(6.dp, 18.dp),
                            color = ChatColors.Accent,
                            shape = RoundedCornerShape(2.dp)
                        ) {}
                    }
                }
            }

            // Image attachment (for standalone images)
            message.imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Chat image",
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .clip(RoundedCornerShape(12.dp))
                        .padding(top = 8.dp),
                )
            }
        }
    }
}

/**
 * Horizontal scrollable gallery row for grouped gallery images.
 * No scrollbar shown, user can swipe horizontally.
 */
@Composable
fun GalleryRow(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Assistant header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = ChatColors.AssistantAvatarBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "AI",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "ChatAI",
                color = ChatColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Horizontal scrollable image row - no scrollbar visible
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = 60.dp), // offset to align with text content
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(messages) { message ->
                when {
                    message.imageStatus == "generating" -> {
                        Surface(
                            modifier = Modifier
                                .width(240.dp)
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            color = ChatColors.SurfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = ChatColors.Accent,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        text = "Generating...",
                                        color = ChatColors.TextTertiary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                    message.imageUrl != null -> {
                        Surface(
                            modifier = Modifier
                                .width(240.dp)
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            color = ChatColors.SurfaceVariant
                        ) {
                            AsyncImage(
                                model = message.imageUrl,
                                contentDescription = message.content,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                        }
                    }
                    message.imageStatus == "failed" -> {
                        Surface(
                            modifier = Modifier
                                .width(180.dp)
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            color = ChatColors.SurfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BrokenImage,
                                        contentDescription = null,
                                        tint = ChatColors.Error,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        text = "Failed",
                                        color = ChatColors.Error,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
