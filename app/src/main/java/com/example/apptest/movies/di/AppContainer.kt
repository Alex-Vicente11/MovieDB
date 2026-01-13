package com.example.apptest.movies.di

import com.example.apptest.core.data.network.NetworkModule
import com.example.apptest.features.popular_movies.di.PopularMoviesContainer
import com.example.apptest.features.search.di.SearchContainer
import com.example.apptest.movies.data.remote.api.TMDBApiService
import com.example.apptest.movies.data.repository.MovieRepositoryImpl
import com.example.apptest.movies.domain.repository.MovieRepository
import com.example.apptest.movies.domain.usecase.GetMovieDetailsUseCase

/**
 * CONTENEDOR DE DEPENDENCIAS PRINCIPAL
 *
 * Service Locator manual (Dependency Injection sin framework)
 *
 * Estado actual (Fase 3 completada):
 * ✅ Feature: Search (migrado)
 * ✅ Feature: Popular Movies (migrado)
 * ⏳ Feature: Movie Details (pendiente - Fase 4)
 * ⏳ Feature: Videos (pendiente - Fase 5)
 *
 * Arquitectura de Features Modulares:
 * - Cada feature tiene su propio contenedor
 * - AppContainer solo coordina features
 * - Legacy code se eliminará progresivamente
 */
class AppContainer {

    // ═══════════════════════════════════════════════════════════════════
    // FEATURE CONTAINERS (Arquitectura Modular)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Feature: Search
     * Responsabilidad: Búsqueda de películas
     * Estado: ✅ Migrado completamente (Fase 2)
     */
    val searchContainer: SearchContainer by lazy {
        SearchContainer()
    }

    /**
     * Feature: Popular Movies
     * Responsabilidad: Mostrar películas populares
     * Estado: ✅ Migrado completamente (Fase 3)
     */
    val popularMoviesContainer: PopularMoviesContainer by lazy {
        PopularMoviesContainer()
    }

    // ═══════════════════════════════════════════════════════════════════
    // LEGACY CODE (Será eliminado en Fase 6)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * API monolítico legacy
     * : Eliminar cuando se complete migración de Movie Details (Fase 4)
     */
    private val apiService: TMDBApiService by lazy {
        NetworkModule.provideTMDBApiService()
    }

    /**
     * Repositorio monolítico legacy
     * : Eliminar cuando se complete migración de Movie Details (Fase 4)
     */
    val movieRepository: MovieRepository by lazy {
        MovieRepositoryImpl(apiService)
    }

    /**
     * UseCase de detalles (legacy)
     * : Mover a features/movie_details/ en Fase 4
     */
    val getMovieDetailsUseCase: GetMovieDetailsUseCase by lazy {
        GetMovieDetailsUseCase(movieRepository)
    }
}