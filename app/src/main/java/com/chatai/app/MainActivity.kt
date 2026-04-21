package com.chatai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatai.app.ui.screens.chat.ChatScreen
import com.chatai.app.ui.screens.chat.ChatViewModel
import com.chatai.app.ui.screens.conversations.ConversationListViewModel
import com.chatai.app.ui.theme.ChatAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChatAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val convViewModel: ConversationListViewModel = viewModel()
                    val conversations by convViewModel.conversations.collectAsState()

                    ChatScreen(
                        conversations = conversations,
                        onNewChat = {
                            // Handled inside ChatViewModel
                        },
                        onDeleteConversation = { /* Handled inside ChatViewModel */ },
                        onSelectConversation = { /* Handled inside ChatViewModel */ }
                    )
                }
            }
        }
    }
}
