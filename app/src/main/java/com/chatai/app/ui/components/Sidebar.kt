package com.chatai.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatai.app.domain.model.Conversation
import com.chatai.app.ui.theme.ChatColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun Sidebar(
    conversations: List<Conversation>,
    currentConversationId: String?,
    onConversationClick: (String) -> Unit,
    onNewChat: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    isOpen: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(280.dp)
                .background(ChatColors.SidebarBackground)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ChatAI",
                        color = ChatColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = MaterialTheme.typography.titleMedium.fontWeight
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close sidebar",
                        tint = ChatColors.TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // New Chat button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                shape = RoundedCornerShape(12.dp),
                color = ChatColors.SurfaceVariant,
                onClick = onNewChat
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "New Chat",
                        tint = ChatColors.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "New chat",
                        color = ChatColors.TextPrimary,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Conversations list grouped by date
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val grouped = groupConversationsByDate(conversations)
                grouped.forEach { (dateLabel, convs) ->
                    item {
                        Text(
                            text = dateLabel,
                            color = ChatColors.TextTertiary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    items(convs) { conversation ->
                        ConversationItem(
                            conversation = conversation,
                            isSelected = conversation.id == currentConversationId,
                            onClick = { onConversationClick(conversation.id) },
                            onDelete = { onDeleteConversation(conversation.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: Conversation,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDelete by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(if (isSelected) Modifier.background(ChatColors.SurfaceVariant) else Modifier),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = conversation.title,
                color = if (isSelected) ChatColors.TextPrimary else ChatColors.TextSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Delete button (appears on long press or hover)
            IconButton(
                onClick = {
                    onDelete()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (showDelete) Icons.Default.Delete else Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = ChatColors.TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun groupConversationsByDate(conversations: List<Conversation>): Map<String, List<Conversation>> {
    val calendar = Calendar.getInstance()
    val today = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
    val yesterday = Calendar.getInstance().apply { timeInMillis = today.timeInMillis; add(Calendar.DAY_OF_MONTH, -1) }
    val sevenDaysAgo = Calendar.getInstance().apply { timeInMillis = today.timeInMillis; add(Calendar.DAY_OF_MONTH, -7) }
    val thirtyDaysAgo = Calendar.getInstance().apply { timeInMillis = today.timeInMillis; add(Calendar.DAY_OF_MONTH, -30) }

    return conversations.groupBy { conv ->
        val convCalendar = Calendar.getInstance().apply { timeInMillis = conv.updatedAt }
        when {
            convCalendar.after(today) -> "Today"
            convCalendar.after(yesterday) -> "Yesterday"
            convCalendar.after(sevenDaysAgo) -> "Previous 7 Days"
            convCalendar.after(thirtyDaysAgo) -> "Previous 30 Days"
            else -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(conv.updatedAt))
        }
    }
}
