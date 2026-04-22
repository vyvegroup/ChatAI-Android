package com.chatai.app.ui.screens.gallery

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.chatai.app.ImageSaver
import com.chatai.app.domain.model.ChatMessage
import com.chatai.app.ui.theme.ChatColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    galleryViewModel: GalleryViewModel = viewModel(),
    onBack: () -> Unit
) {
    val images by galleryViewModel.images.collectAsState()
    val context = LocalContext.current
    var fullscreenImage by remember { mutableStateOf<ChatMessage?>(null) }
    var showSaveConfirm by remember { mutableStateOf<ChatMessage?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatColors.Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(ChatColors.Background)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ChatColors.TextSecondary)
            }
            Text(
                text = "Gallery AI",
                color = ChatColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            if (images.isNotEmpty()) {
                Text(
                    text = "${images.size} images",
                    color = ChatColors.TextTertiary,
                    fontSize = 12.sp
                )
            }
        }

        HorizontalDivider(color = ChatColors.Divider)

        if (images.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = ChatColors.TextTertiary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No images yet",
                        color = ChatColors.TextTertiary,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Generated images will appear here",
                        color = ChatColors.TextTertiary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(images, key = { it.id }) { message ->
                    GalleryImageItem(
                        message = message,
                        onClick = { fullscreenImage = message },
                        onLongPress = { showSaveConfirm = message }
                    )
                }
            }
        }
    }

    // Fullscreen image dialog
    fullscreenImage?.let { msg ->
        Dialog(onDismissRequest = { fullscreenImage = null }) {
            val scope = rememberCoroutineScope()
            var isSaving by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = msg.imageUrl,
                    contentDescription = msg.content,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                IconButton(
                    onClick = { fullscreenImage = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                IconButton(
                    onClick = {
                        if (!isSaving) {
                            isSaving = true
                            scope.launch(Dispatchers.IO) {
                                msg.imageUrl?.let { url ->
                                    ImageSaver.saveImage(context, url, "ChatAI_Gallery")
                                    isSaving = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .background(ChatColors.Accent, RoundedCornerShape(20.dp))
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Save",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    // Save confirmation dialog
    showSaveConfirm?.let { msg ->
        val scope = rememberCoroutineScope()

        AlertDialog(
            onDismissRequest = { showSaveConfirm = null },
            containerColor = ChatColors.Surface,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text("Save Image", color = ChatColors.TextPrimary, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Text(
                    "Save this image to your device?",
                    color = ChatColors.TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveConfirm = null
                        scope.launch(Dispatchers.IO) {
                            msg.imageUrl?.let { url ->
                                ImageSaver.saveImage(context, url, "ChatAI_Gallery")
                            }
                        }
                    }
                ) {
                    Text("Save", color = ChatColors.Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveConfirm = null }) {
                    Text("Cancel", color = ChatColors.TextSecondary)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryImageItem(
    message: ChatMessage,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        color = ChatColors.SurfaceVariant
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = message.imageUrl,
                contentDescription = message.content,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
