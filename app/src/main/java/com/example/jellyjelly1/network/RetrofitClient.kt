package com.example.jellyjelly1.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // ✅ Base URL must end with /
    private const val BASE_URL = "https://www.jellyjelly.com/"

    // ✅ Lazy-initialized API service
    val apiService: JellyApiService by lazy {
        createRetrofit().create(JellyApiService::class.java)
    }

    // ✅ Retrofit builder with logging
    private fun createRetrofit(): Retrofit {
        // Logging interceptor for debug printing
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}