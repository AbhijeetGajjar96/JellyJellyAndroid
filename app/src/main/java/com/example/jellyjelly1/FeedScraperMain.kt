package com.example.jellyjelly1

import com.example.jellyjelly1.network.RetrofitClient
import com.example.jellyjelly1.repository.JellyRepository
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val repository = JellyRepository()
    val videos = repository.fetchAllVideos()
    videos.forEach { println(it.video_url) }
} 