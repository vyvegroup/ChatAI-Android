package com.chatai.app.ui.screens.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chatai.app.ChatApplication
import com.chatai.app.domain.model.ChatMessage
import com.chatai.app.data.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository = (application as ChatApplication).container.repository

    private val _images = MutableStateFlow<List<ChatMessage>>(emptyList())
    val images: StateFlow<List<ChatMessage>> = _images

    init {
        viewModelScope.launch {
            repository.getAllCompletedImages().collect { imageList ->
                _images.value = imageList
            }
        }
    }
}
