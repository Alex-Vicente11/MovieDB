package com.example.apptest.movies.di

import com.example.apptest.features.search.di.SearchContainer
import com.example.apptest.core.data.network.NetworkModule
import com.example.apptest.movies.data.remote.api.TMDBApiService
import com.example.apptest.movies.data.repository.MovieRepositoryImpl
import com.example.apptest.movies.domain.repository.MovieRepository
import com.example.apptest.movies.domain.usecase.GetMovieDetailsUseCase
import com.example.apptest.movies.domain.usecase.GetPopularMoviesUseCase


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
    // FEATURE CONTAINERS
    // ═══════════════════════════════════════════════════════

    /**
     * Feature: Search
     */
    val searchContainer: SearchContainer by lazy {
        SearchContainer()
    }

    // ═══════════════════════════════════════════════════════
    // DATA LAYER (Legacy)
    // ═══════════════════════════════════════════════════════

    private val apiService: TMDBApiService by lazy {
        NetworkModule.provideTMDBApiService()
    }

    val movieRepository: MovieRepository by lazy {
        MovieRepositoryImpl(apiService)
    }

    // ═══════════════════════════════════════════════════════
    // DOMAIN LAYER - USE CASES (Legacy)
    // ═══════════════════════════════════════════════════════

    // ⬇️ ELIMINAR ESTA PROPIEDAD COMPLETA:
    // val searchMoviesUseCase: SearchMoviesUseCase by lazy {
    //     SearchMoviesUseCase(movieRepository)
    // }

    val getPopularMoviesUseCase: GetPopularMoviesUseCase by lazy {
        GetPopularMoviesUseCase(movieRepository)
    }

    val getMovieDetailsUseCase: GetMovieDetailsUseCase by lazy {
        GetMovieDetailsUseCase(movieRepository)
    }
}