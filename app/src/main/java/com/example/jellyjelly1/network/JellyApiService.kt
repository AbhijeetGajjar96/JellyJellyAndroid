package com.example.jellyjelly1.network

import com.example.jellyjelly1.model.FeedResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface JellyApiService {
    // TODO: Replace "/feed" with the actual endpoint path if different (e.g., "/api/feed", "/feed/more", etc.)
    @GET("/feed")
    suspend fun getFeed(
        @Query("cursor") cursor: String? = null
    ): FeedResponse
} 