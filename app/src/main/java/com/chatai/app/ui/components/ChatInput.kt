package com.chatai.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatai.app.ui.theme.ChatColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInput(
    message: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onGenerateImage: () -> Unit = {},
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "Message ChatAI..."
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = ChatColors.InputBackground,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = ChatColors.Border,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image generation button
                IconButton(
                    onClick = onGenerateImage,
                    modifier = Modifier.size(40.dp),
                    enabled = !isStreaming
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Generate image",
                        tint = if (isStreaming) ChatColors.IconTint.copy(alpha = 0.4f) else ChatColors.IconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Text field
                OutlinedTextField(
                    value = message,
                    onValueChange = onMessageChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            text = placeholder,
                            color = ChatColors.TextTertiary,
                            fontSize = 15.sp
                        )
                    },
                    textStyle = LocalTextStyle.current.copy(
                        color = ChatColors.TextPrimary,
                        fontSize = 15.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        cursorColor = ChatColors.TextPrimary
                    ),
                    maxLines = 8,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (message.isNotBlank() && !isStreaming) {
                                onSend()
                                keyboardController?.hide()
                            }
                        }
                    ),
                    enabled = enabled && !isStreaming
                )

                // Send/Stop button
                IconButton(
                    onClick = {
                        if (isStreaming) {
                            // TODO: Stop streaming
                        } else if (message.isNotBlank()) {
                            onSend()
                            keyboardController?.hide()
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    enabled = message.isNotBlank() || isStreaming
                ) {
                    Icon(
                        imageVector = if (isStreaming) Icons.Default.StopCircle else Icons.Default.Send,
                        contentDescription = if (isStreaming) "Stop" else "Send",
                        tint = if (message.isNotBlank() || isStreaming)
                            ChatColors.TextOnAccent
                        else
                            ChatColors.IconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Bottom text
            Text(
                text = "ChatAI can make mistakes. Consider checking important information.",
                color = ChatColors.TextTertiary,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
    }
}
