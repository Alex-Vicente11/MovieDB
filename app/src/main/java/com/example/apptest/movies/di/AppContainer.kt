package com.example.apptest.movies.di

import com.example.apptest.movies.data.network.NetworkModule
import com.example.apptest.movies.data.remote.api.TMDBApiService
import com.example.apptest.movies.data.repository.MovieRepositoryImpl
import com.example.apptest.movies.domain.repository.MovieRepository
import com.example.apptest.movies.domain.usecase.GetMovieDetailsUseCase
import com.example.apptest.movies.domain.usecase.GetPopularMoviesUseCase
import com.example.apptest.movies.domain.usecase.SearchMoviesUseCase

/**
 * CONTENEDOR DE DEPENDENCIAS
 *
 * Service Locator manual (Dependency Injection sin framework)
 *
 * Organización por capas:
 * 1. Network Layer → NetworkModule (separado)
 * 2. Data Layer → API Service, Repository
 * 3. Domain Layer → Use Cases
 *
 * Ventajas de esta refactorización:
 *  NetworkModule separado (responsabilidad única)
 *  AppContainer más limpio y enfocado
 *  Fácil migración futura a Hilt/Koin
 */
class AppContainer {

    // ═══════════════════════════════════════════════════════
    // DATA LAYER
    // ═══════════════════════════════════════════════════════

    /**
     * API Service (singleton)
     * Delegado al NetworkModule
     */
    private val apiService: TMDBApiService by lazy {
        NetworkModule.provideTMDBApiService()
    }

    /**
     * Repository (singleton)
     * Implementación concreta que cumple el contrato de MovieRepository
     */
    val movieRepository: MovieRepository by lazy {
        MovieRepositoryImpl(apiService)
    }

    // ═══════════════════════════════════════════════════════
    // DOMAIN LAYER - USE CASES
    // ═══════════════════════════════════════════════════════

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