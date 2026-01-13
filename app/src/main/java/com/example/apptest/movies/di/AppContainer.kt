package com.example.apptest.movies.di

import com.example.apptest.core.data.network.NetworkModule
import com.example.apptest.features.movie_details.di.MovieDetailsContainer
import com.example.apptest.features.popular_movies.di.PopularMoviesContainer
import com.example.apptest.features.search.di.SearchContainer
import com.example.apptest.movies.data.remote.api.TMDBApiService
import com.example.apptest.movies.data.repository.MovieRepositoryImpl
import com.example.apptest.movies.domain.repository.MovieRepository

/**
 * CONTENEDOR DE DEPENDENCIAS PRINCIPAL
 *
 * Service Locator manual (Dependency Injection sin framework)
 *
 * Estado actual (Fase 4 completada):
 *  Feature: Search (migrado)
 *  Feature: Popular Movies (migrado)
 *  Feature: Movie Details (migrado)
 * Feature: Videos (pendiente - Fase 5)
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
     * Estado:  Migrado completamente (Fase 2)
     */
    val searchContainer: SearchContainer by lazy {
        SearchContainer()
    }

    /**
     * Feature: Popular Movies
     * Responsabilidad: Mostrar películas populares
     * Estado:  Migrado completamente (Fase 3)
     */
    val popularMoviesContainer: PopularMoviesContainer by lazy {
        PopularMoviesContainer()
    }

    /**
     * Feature: Movie Details
     * Responsabilidad: Mostrar detalles completos de una película
     * Estado: Migrado completamente (Fase 4)
     */
    val movieDetailsContainer: MovieDetailsContainer by lazy {
        MovieDetailsContainer()
    }

    // ═══════════════════════════════════════════════════════════════════
    // LEGACY CODE (Será eliminado en Fase 6)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * API monolítico legacy
     * Eliminar en Fase 6 (ya no se usa)
     */
    private val apiService: TMDBApiService by lazy {
        NetworkModule.provideTMDBApiService()
    }

    /**
     * Repositorio monolítico legacy
     * : Eliminar en Fase 6 (ya no se usa)
     */
    val movieRepository: MovieRepository by lazy {
        MovieRepositoryImpl(apiService)
    }

}