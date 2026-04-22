package com.chatai.app.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatai.app.data.remote.AiModels
import com.chatai.app.domain.model.ChatMessage
import com.chatai.app.domain.model.Conversation
import com.chatai.app.ui.components.*
import com.chatai.app.ui.screens.gallery.GalleryScreen
import com.chatai.app.ui.theme.ChatColors

sealed class DisplayItem {
    data class SingleMessage(val message: ChatMessage) : DisplayItem()
    data class GalleryGroup(val messages: List<ChatMessage>) : DisplayItem()
}

fun buildDisplayItems(messages: List<ChatMessage>): List<DisplayItem> {
    val items = mutableListOf<DisplayItem>()
    var i = 0
    while (i < messages.size) {
        val msg = messages[i]
        if (msg.role == "image" && msg.imageType == "gallery" && msg.galleryId != null) {
            val galleryMessages = mutableListOf<ChatMessage>()
            val gId = msg.galleryId
            while (i < messages.size &&
                messages[i].role == "image" &&
                messages[i].imageType == "gallery" &&
                messages[i].galleryId == gId
            ) {
                galleryMessages.add(messages[i])
                i++
            }
            if (galleryMessages.size == 1) {
                items.add(DisplayItem.SingleMessage(galleryMessages[0]))
            } else {
                items.add(DisplayItem.GalleryGroup(galleryMessages))
            }
        } else {
            items.add(DisplayItem.SingleMessage(msg))
            i++
        }
    }
    return items
}

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
    var showGallery by remember { mutableStateOf(false) }
    val messages by chatViewModel.messages.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val isGeneratingImage by chatViewModel.isGeneratingImage.collectAsState()
    var currentInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val showImageSheet by chatViewModel.showImageSheet.collectAsState()
    val showModelSheet by chatViewModel.showModelSheet.collectAsState()
    val selectedModel by chatViewModel.selectedModel.collectAsState()

    val displayItems = remember(messages) { buildDisplayItems(messages) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val error by chatViewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            chatViewModel.clearError()
        }
    }

    // Show Gallery screen
    if (showGallery) {
        GalleryScreen(onBack = { showGallery = false })
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ChatColors.Background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Main content - rendered first (bottom layer)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(ChatColors.Background)
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { sidebarOpen = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = ChatColors.TextSecondary)
                    }

                    // Model selector button
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        onClick = { chatViewModel.toggleModelSheet() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = selectedModel.name,
                                color = ChatColors.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Change model",
                                tint = ChatColors.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "Free",
                        color = ChatColors.Accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Messages or Welcome
                if (messages.isEmpty()) {
                    WelcomeScreen(
                        modifier = Modifier.weight(1f),
                        onSendExample = { example -> currentInput = example }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(items = displayItems, key = {
                            when (it) {
                                is DisplayItem.SingleMessage -> "msg_${it.message.id}"
                                is DisplayItem.GalleryGroup -> "gallery_${it.messages.firstOrNull()?.galleryId ?: ""}_${it.messages.size}_${it.messages.firstOrNull()?.id ?: ""}"
                            }
                        }) { item ->
                            when (item) {
                                is DisplayItem.SingleMessage -> MessageBubble(message = item.message)
                                is DisplayItem.GalleryGroup -> GalleryRow(messages = item.messages)
                            }
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }

                // Input
                ChatInput(
                    message = currentInput,
                    onMessageChange = { currentInput = it },
                    onSend = {
                        if (currentInput.isNotBlank()) {
                            chatViewModel.sendMessage(currentInput)
                            currentInput = ""
                        }
                    },
                    onGenerateImage = { chatViewModel.toggleImageSheet() },
                    isStreaming = isLoading || isGeneratingImage
                )
            }

            // Sidebar overlay: scrim FIRST, then sidebar ON TOP of scrim
            // This fixes the bug where scrim blocked sidebar touch events
            if (sidebarOpen) {
                // Scrim covers full screen, catches clicks to close
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { sidebarOpen = false }
                )
                // Sidebar rendered AFTER scrim so it's on top and fully interactive
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
                    onOpenGallery = { showGallery = true },
                    isOpen = true,
                    onClose = { sidebarOpen = false }
                )
            }
        }
    }

    // Image generation bottom sheet
    if (showImageSheet) {
        ModalBottomSheet(
            onDismissRequest = { chatViewModel.dismissImageSheet() },
            containerColor = ChatColors.SidebarBackground
        ) {
            ImageGenerationSheet(
                onDismiss = { chatViewModel.dismissImageSheet() },
                onGenerate = { prompt, width, height ->
                    chatViewModel.generateImage(prompt, width, height)
                },
                isGenerating = isGeneratingImage
            )
        }
    }

    // Model picker bottom sheet
    if (showModelSheet) {
        ModalBottomSheet(
            onDismissRequest = { chatViewModel.dismissModelSheet() },
            containerColor = ChatColors.SidebarBackground
        ) {
            ModelPickerSheet(
                models = AiModels.freeModels,
                selectedModelId = selectedModel.id,
                onSelectModel = { chatViewModel.selectModel(it) },
                onDismiss = { chatViewModel.dismissModelSheet() }
            )
        }
    }
}

@Composable
private fun WelcomeScreen(
    modifier: Modifier = Modifier,
    onSendExample: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = ChatColors.Accent
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "AI",
                    color = ChatColors.TextOnAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "What can I help with?",
            color = ChatColors.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(32.dp))

        val examples = listOf(
            "Generate an image of a sunset over mountains",
            "Explain quantum computing simply",
            "Write a Python web scraper",
            "Tell me about Vietnamese culture"
        )

        examples.forEach { example ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = ChatColors.SurfaceVariant,
                onClick = { onSendExample(example) }
            ) {
                Text(
                    text = example,
                    color = ChatColors.TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
