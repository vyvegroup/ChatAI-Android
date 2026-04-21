package com.chatai.app.ui.screens.conversations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatai.app.domain.model.Conversation

@Composable
fun ConversationListScreen(
    viewModel: ConversationListViewModel = viewModel(),
    onConversationsLoaded: (List<Conversation>) -> Unit
) {
    val conversations by viewModel.conversations.collectAsState()

    LaunchedEffect(conversations) {
        onConversationsLoaded(conversations)
    }

    Box(modifier = Modifier.fillMaxSize())
}
