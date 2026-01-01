package com.example.apptest.data.api

import com.example.apptest.data.remote.api.TMDBApiService
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://api.themoviedb.org/3/"

    private const val ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI4ZWE3MjJiMjQ4YjU1N2E0Mzc1YmQ4OTUzYmU5ZjRmNyIsIm5iZiI6MTc2NTQ4MTM0Mi44NTMsInN1YiI6IjY5M2IxYjdlYTYzOGMzMzk3MjM2MDdlNSIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.k7Zpf2F7axUGqVU1rElMqK5iLlM6i5W2Qxa7Up6_iLY"

    private val authInterceptor = Interceptor { chain ->
        val newRequest = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $ACCESS_TOKEN")
            .addHeader("accept", "application/json")
            .build()

        chain.proceed(newRequest)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    private val gson = GsonBuilder()
        .setLenient() // Permite JSON no estricto
        .create()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: TMDBApiService = retrofit.create(TMDBApiService::class.java)
}