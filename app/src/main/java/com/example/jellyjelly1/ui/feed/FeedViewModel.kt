package com.example.jellyjelly1.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jellyjelly1.model.VideoItem
import com.example.jellyjelly1.network.RetrofitClient
import com.example.jellyjelly1.repository.JellyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import android.util.Log

class FeedViewModel : ViewModel() {
    private val repository = JellyRepository()
    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    fun fetchAllVideos() {
        viewModelScope.launch {
            try {
                _videos.value = repository.fetchAllVideos()
                _error.value = null
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Error fetching videos", e)
                _error.value = "Failed to load videos. Please try again."
            }
        }
    }
} 