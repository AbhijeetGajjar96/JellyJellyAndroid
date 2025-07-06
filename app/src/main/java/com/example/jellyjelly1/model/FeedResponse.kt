package com.example.jellyjelly1.model

data class FeedResponse(
    val videos: List<VideoItem>,
    val nextCursor: String?
) 