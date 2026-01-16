package com.example.apptest.di

import com.example.apptest.features.movie_details.di.MovieDetailsContainer
import com.example.apptest.features.popular_movies.di.PopularMoviesContainer
import com.example.apptest.features.search.di.SearchContainer
import com.example.apptest.features.videos.di.VideosContainer

/**
 * CONTENEDOR DE DEPENDENCIAS PRINCIPAL
 *
 * UBICACIÓN: di/ (raíz del package base)
 *
 * Service Locator manual (Dependency Injection sin framework)
 *
 * Arquitectura de Features Modulares:
 * - Cada feature tiene su propio contenedor independiente
 * - AppContainer solo coordina y expone los features
 * - Separación clara de responsabilidades
 *
 * Estado actual :
 *  Feature: Search (migrado y funcional)
 *  Feature: Popular Movies (migrado y funcional)
 *  Feature: Movie Details (migrado y funcional)
 *  Feature: Videos (migrado y funcional)
 *
 * Ventajas de esta arquitectura:
 * - Cada feature es independiente y autocontenido
 * - Fácil agregar/quitar features sin afectar otros
 * - Testeable (mock solo el feature que necesitas)
 * - Escalable (nuevos features = nuevos containers)
 * - Código organizado y mantenible
 */
class AppContainer {

    // ═══════════════════════════════════════════════════════════════════
    // FEATURE CONTAINERS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Feature: Search
     *
     * Responsabilidad: Búsqueda de películas
     *
     * Dependencias expuestas:
     * - searchMoviesUseCase: Buscar películas por query
     *
     * Uso:
     * ```kotlin
     * val searchUseCase = appContainer.searchContainer.searchMoviesUseCase
     * ```
     */
    val searchContainer: SearchContainer by lazy {
        SearchContainer()
    }

    /**
     * Feature: Popular Movies
     *
     * Responsabilidad: Mostrar películas populares
     *
     * Dependencias expuestas:
     * - getPopularMoviesUseCase: Obtener películas populares
     *
     * Uso:
     * ```kotlin
     * val popularUseCase = appContainer.popularMoviesContainer.getPopularMoviesUseCase
     * ```
     */
    val popularMoviesContainer: PopularMoviesContainer by lazy {
        PopularMoviesContainer()
    }

    /**
     * Feature: Movie Details
     *
     * Responsabilidad: Mostrar detalles completos de una película
     *
     * Dependencias expuestas:
     * - getMovieDetailsUseCase: Obtener detalles de una película por ID
     *
     * Uso:
     * ```kotlin
     * val detailsUseCase = appContainer.movieDetailsContainer.getMovieDetailsUseCase
     * ```
     */
    val movieDetailsContainer: MovieDetailsContainer by lazy {
        MovieDetailsContainer()
    }

    /** Feature: Videos
     *
     * Responsabilidad: Mostrar videos y trailers de peliculas
     *
     * Dependencias expuestas:
     * - getMovieVideosUseCase: Obtener videos de una pelicula por ID
     *
     * Use:
     * Kotlin
     * val videosUseCase = appContainer.videosContainer.getMovieVideosUseCase
     */
    val videosContainer: VideosContainer by lazy {
        VideosContainer()
    }
}