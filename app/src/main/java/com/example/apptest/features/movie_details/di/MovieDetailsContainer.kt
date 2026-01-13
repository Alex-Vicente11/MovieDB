package com.example.apptest.features.movie_details.di

import com.example.apptest.core.di.CoreContainer
import com.example.apptest.features.movie_details.data.remote.api.MovieDetailsApi
import com.example.apptest.features.movie_details.data.repository.MovieDetailsRepositoryImpl
import com.example.apptest.features.movie_details.domain.repository.MovieDetailsRepository
import com.example.apptest.features.movie_details.domain.usecase.GetMovieDetailsUseCase

/**
 * CONTENEDOR DE DEPENDENCIAS - Feature: Movie Details
 *
 * UBICACIÓN: features/movie_details/di/
 *
 * Responsabilidad:
 * - Proveer todas las dependencias del feature movie_details
 * - Gestionar ciclo de vida de dependencias (singletons con lazy)
 * - Usar CoreContainer para dependencias compartidas (Retrofit)
 *
 * Service Locator Pattern:
 * Este contenedor implementa Service Locator manual
 * (alternativa a Dagger/Hilt cuando no quieres la complejidad)
 *
 * Dependency Injection Pattern aplicado:
 * - Data Layer depende de Domain (Repository implementa interface)
 * - Domain Layer no depende de nadie
 * - Presentation Layer depende de Domain (ViewModel usa UseCase)
 *
 * Patrón de construcción:
 * 1. API (capa más baja - acceso a datos externos)
 * 2. Repository (implementa lógica de acceso a datos)
 * 3. UseCase (implementa lógica de negocio)
 *
 * Uso de lazy:
 * - Inicialización diferida (solo cuando se necesita)
 * - Singleton por contenedor (una sola instancia)
 * - Thread-safe (lazy es synchronized por defecto)
 *
 * Comparación con AppContainer monolítico:
 * ❌ ANTES: Todas las dependencias mezcladas en un solo contenedor
 * ✅ AHORA: Cada feature tiene su propio contenedor
 *
 * Beneficios de Feature Containers:
 * - Separación de responsabilidades
 * - Fácil de testear (mock solo el feature que necesitas)
 * - Escalable (agregar features no afecta contenedores existentes)
 * - Claro qué dependencias usa cada feature
 */
class MovieDetailsContainer {

    // ═══════════════════════════════════════════════════════════════════
    // DATA LAYER
    // ═══════════════════════════════════════════════════════════════════

    /**
     * API especializada para detalles de películas
     *
     * Usa Retrofit de CoreContainer:
     * - CoreContainer.retrofit es un singleton compartido
     * - Solo creamos la interfaz API específica del feature
     *
     * lazy: Se crea solo cuando se accede por primera vez
     */
    private val movieDetailsApi: MovieDetailsApi by lazy {
        CoreContainer.retrofit.create(MovieDetailsApi::class.java)
    }

    /**
     * Repositorio de detalles de películas
     *
     * Implementación concreta que:
     * - Usa movieDetailsApi para acceso a datos
     * - Implementa MovieDetailsRepository (interface del domain)
     *
     * private: Solo se expone a través del UseCase
     */
    private val movieDetailsRepository: MovieDetailsRepository by lazy {
        MovieDetailsRepositoryImpl(movieDetailsApi)
    }

    // ═══════════════════════════════════════════════════════════════════
    // DOMAIN LAYER - USE CASES
    // ═══════════════════════════════════════════════════════════════════

    /**
     * UseCase para obtener detalles de películas
     *
     * PUBLIC: Esta es la dependencia que se inyecta en ViewModels
     *
     * Dependency Inversion:
     * - GetMovieDetailsUseCase depende de MovieDetailsRepository (interface)
     * - NO depende de MovieDetailsRepositoryImpl (implementación)
     * - Esto permite cambiar la implementación sin afectar el UseCase
     *
     * Ejemplo de uso:
     * ```kotlin
     * val appContainer = (application as MyApplication).appContainer
     * val useCase = appContainer.movieDetailsContainer.getMovieDetailsUseCase
     *
     * viewModel = MovieDetailsViewModel(useCase)
     * ```
     */
    val getMovieDetailsUseCase: GetMovieDetailsUseCase by lazy {
        GetMovieDetailsUseCase(movieDetailsRepository)
    }

    // ═══════════════════════════════════════════════════════════════════
    // NOTAS DE DISEÑO
    // ═══════════════════════════════════════════════════════════════════

    /*
     * ¿Por qué este patrón es mejor que el AppContainer monolítico?
     *
     * ANTES (AppContainer monolítico):
     * class AppContainer {
     *     val apiService: TMDBApiService
     *     val movieRepository: MovieRepository  // 3 responsabilidades
     *     val searchUseCase: SearchMoviesUseCase
     *     val popularUseCase: GetPopularMoviesUseCase
     *     val detailsUseCase: GetMovieDetailsUseCase
     *     // Todas las dependencias mezcladas
     * }
     *
     * DESPUÉS (Feature Containers):
     * class SearchContainer { ... }          // Solo search
     * class PopularMoviesContainer { ... }   // Solo popular
     * class MovieDetailsContainer { ... }    // Solo details
     *
     * class AppContainer {
     *     val searchContainer = SearchContainer()
     *     val popularMoviesContainer = PopularMoviesContainer()
     *     val movieDetailsContainer = MovieDetailsContainer()
     * }
     *
     * Beneficios:
     *  Separación clara de responsabilidades
     *  Cada feature es independiente
     *  Fácil de testear (mock solo el container que necesitas)
     *  Escalable (agregar features no afecta existentes)
     *  Código más organizado y mantenible
     */
}