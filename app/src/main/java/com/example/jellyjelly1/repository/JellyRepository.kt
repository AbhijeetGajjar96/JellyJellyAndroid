package com.example.jellyjelly1.repository

import com.example.jellyjelly1.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.*
import com.google.gson.reflect.TypeToken

class JellyRepository {
    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun fetchAllVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val url = "https://cbtzdoasmkbbiwnyoxvz.supabase.co/rest/v1/shareable_data?select=&limit=5&privacy=eq.public&order=updated_at.desc"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiYW5vbiIsImlhdCI6MTYzNjM4MjEwOCwiZXhwIjoxOTUxOTU4MTA4fQ.YdFG3RUvDJmRHoUQV4C5TsZcg2moGDDmnr4RNKO-Bcg")
            .addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiYW5vbiIsImlhdCI6MTYzNjM4MjEwOCwiZXhwIjoxOTUxOTU4MTA4fQ.YdFG3RUvDJmRHoUQV4C5TsZcg2moGDDmnr4RNKO-Bcg")
            .addHeader("Accept", "application/json")
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext emptyList<VideoItem>()

        //println("[JellyRepository] Raw JSON response: $body")

        // Parse the JSON array
        val jsonArray = JsonParser.parseString(body).asJsonArray
        val videos = mutableListOf<VideoItem>()
        for (item in jsonArray) {
            val obj = item.asJsonObject
            val id = obj.get("id")?.asString ?: ""
            val content = obj.get("content")?.asJsonObject
            val videoUrl = content?.get("url")?.asString ?: ""
            val thumbnailsArray = content?.getAsJsonArray("thumbnails")
            val thumbnail = if (thumbnailsArray != null && thumbnailsArray.size() > 0) {
                thumbnailsArray[0].asString
            } else {
                ""
            }
            val title = if (obj.has("title") && !obj.get("title").isJsonNull) obj.get("title").asString else ""
            val author = if (obj.has("user_id") && !obj.get("user_id").isJsonNull) obj.get("user_id").asString else ""

            println("[JellyRepository] id=$id, videoUrl=$videoUrl, thumbnail=$thumbnail, title=$title, author=$author")

            videos.add(
                VideoItem(
                    id = id,
                    video_url = videoUrl,
                    thumbnail = thumbnail,
                    title = title,
                    author = author
                )
            )
        }
        return@withContext videos
    }
}