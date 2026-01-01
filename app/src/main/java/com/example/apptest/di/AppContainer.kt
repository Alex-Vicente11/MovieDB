package com.example.apptest.di

import com.example.apptest.data.remote.api.TMDBApiService
import com.example.apptest.data.repository.MovieRepositoryImpl
import com.example.apptest.domain.repository.MovieRepository
import com.example.apptest.domain.usecase.GetMovieDetailsUseCase
import com.example.apptest.domain.usecase.GetPopularMoviesUseCase
import com.example.apptest.domain.usecase.SearchMoviesUseCase
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * CONTENEDOR DE DEPENDENCIAS
 *
 * Service Locator manual (antes de implementar Hilt/Koin)
 * Crea e inyecta todas las dependencias de la app
 *
 * Orden de creación:
 * 1. Networking (OkHttp, Retrofit)
 * 2. Data Layer (API Service, Repository)
 * 3. Domain Layer (Use Cases)
 */
class AppContainer {

    // ═══════════════════════════════════════════════════════════
    // NETWORKING
    // ═══════════════════════════════════════════════════════════

    private companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI4ZWE3MjJiMjQ4YjU1N2E0Mzc1YmQ4OTUzYmU5ZjRmNyIsIm5iZiI6MTc2NTQ4MTM0Mi44NTMsInN1YiI6IjY5M2IxYjdlYTYzOGMzMzk3MjM2MDdlNSIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.k7Zpf2F7axUGqVU1rElMqK5iLlM6i5W2Qxa7Up6_iLY"
    }

    /**
     * Interceptor para agregar Bearer Token a todas las requests
     */
    private val authInterceptor = Interceptor { chain ->
        val newRequest = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $ACCESS_TOKEN")
            .addHeader("accept", "application/json")
            .build()
        chain.proceed(newRequest)
    }

    /**
     * Interceptor para logging (solo en debug)
     */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /**
     * OkHttpClient con interceptors
     */
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    /**
     * Gson con configuración lenient
     */
    private val gson by lazy {
        GsonBuilder()
            .setLenient()
            .create()
    }

    /**
     * Retrofit instance (singleton)
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    // ═══════════════════════════════════════════════════════════
    // DATA LAYER
    // ═══════════════════════════════════════════════════════════

    /**
     * API Service (singleton)
     */
    private val apiService: TMDBApiService by lazy {
        retrofit.create(TMDBApiService::class.java)
    }

    /**
     * Repository (singleton)
     * Implementación concreta que cumple el contrato de MovieRepository
     */
    val movieRepository: MovieRepository by lazy {
        MovieRepositoryImpl(apiService)
    }

    // ═══════════════════════════════════════════════════════════
    // DOMAIN LAYER - USE CASES
    // ═══════════════════════════════════════════════════════════

    /**
     * Use Case: Buscar películas
     */
    val searchMoviesUseCase: SearchMoviesUseCase by lazy {
        SearchMoviesUseCase(movieRepository)
    }

    /**
     * Use Case: Obtener películas populares
     */
    val getPopularMoviesUseCase: GetPopularMoviesUseCase by lazy {
        GetPopularMoviesUseCase(movieRepository)
    }

    /**
     * Use Case: Obtener detalles de película
     */
    val getMovieDetailsUseCase: GetMovieDetailsUseCase by lazy {
        GetMovieDetailsUseCase(movieRepository)
    }
}