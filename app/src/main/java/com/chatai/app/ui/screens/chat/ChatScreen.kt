package com.chatai.app.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatai.app.domain.model.Conversation
import com.chatai.app.ui.components.*
import com.chatai.app.ui.theme.ChatColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel = viewModel(),
    conversations: List<Conversation>,
    onNewChat: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onSelectConversation: (String) -> Unit
) {
    var sidebarOpen by remember { mutableStateOf(false) }
    val messages by chatViewModel.messages.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val inputText by remember { mutableStateOf("") }
    var currentInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Show error snackbar
    val error by chatViewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            chatViewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ChatColors.Background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Sidebar
            Sidebar(
                conversations = conversations,
                currentConversationId = chatViewModel.currentConversationId.collectAsState().value,
                onConversationClick = {
                    chatViewModel.selectConversation(it)
                    onSelectConversation(it)
                    sidebarOpen = false
                },
                onNewChat = {
                    chatViewModel.startNewChat()
                    onNewChat()
                    sidebarOpen = false
                },
                onDeleteConversation = {
                    chatViewModel.deleteConversation(it)
                    onDeleteConversation(it)
                },
                isOpen = sidebarOpen,
                onClose = { sidebarOpen = false }
            )

            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Top bar
                TopBar(
                    onMenuClick = { sidebarOpen = true },
                    model = chatViewModel.selectedModel.collectAsState().value
                )

                // Messages
                if (messages.isEmpty()) {
                    // Empty state - Welcome screen
                    WelcomeScreen(
                        onSendExample = { example ->
                            currentInput = example
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = messages,
                            key = { it.id }
                        ) { message ->
                            MessageBubble(message = message)
                        }

                        // Bottom spacing for input
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // Input area
                ChatInput(
                    message = currentInput,
                    onMessageChange = { currentInput = it },
                    onSend = {
                        if (currentInput.isNotBlank()) {
                            chatViewModel.sendMessage(currentInput)
                            currentInput = ""
                        }
                    },
                    isStreaming = isLoading
                )
            }

            // Sidebar overlay (scrim)
            if (sidebarOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ChatColors.Background.copy(alpha = 0.5f))
                        .padding(start = 280.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onMenuClick: () -> Unit,
    model: String
) {
    Surface(
        color = ChatColors.Background,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = ChatColors.TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = model.split("/").lastOrNull()?.replace("-", " ")?.replaceFirstChar { it.uppercase() } ?: "ChatAI",
                color = ChatColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun WelcomeScreen(
    onSendExample: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .weight(1f),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ChatAI Logo
        Surface(
            modifier = Modifier.size(64.dp),
            shape = MaterialTheme.shapes.large,
            color = ChatColors.Accent
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "AI",
                    color = ChatColors.TextOnAccent,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "How can I help you today?",
            color = ChatColors.TextPrimary,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Example prompts
        val examples = listOf(
            "Explain quantum computing in simple terms",
            "Write a Python script to sort a list",
            "What are the best practices for React?",
            "Tell me about the solar system"
        )

        examples.forEach { example ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(vertical = 4.dp),
                shape = MaterialTheme.shapes.medium,
                color = ChatColors.SurfaceVariant,
                onClick = { onSendExample(example) }
            ) {
                Text(
                    text = example,
                    color = ChatColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
