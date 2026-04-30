package com.example.apptest.features.popular_movies.di

import com.example.apptest.core.di.CoreContainer
import com.example.apptest.features.popular_movies.data.remote.api.PopularMoviesApi
import com.example.apptest.features.popular_movies.data.repository.PopularMoviesRepositoryImpl
import com.example.apptest.features.popular_movies.domain.repository.PopularMoviesRepository
import com.example.apptest.features.popular_movies.domain.usecase.GetPopularMoviesUseCase

/**
 * CONTENEDOR DE DEPENDENCIAS - Feature: Popular Movies
 *
 * UBICACIÓN: features/popular_movies/di/
 *
 * Responsabilidad:
 * - Proveer todas las dependencias del feature popular_movies
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
 */
class PopularMoviesContainer {

    // ═══════════════════════════════════════════════════════════════════
    // DATA LAYER
    // ═══════════════════════════════════════════════════════════════════

    /**
     * API especializada para películas populares
     *
     * Usa Retrofit de CoreContainer:
     * - CoreContainer.retrofit es un singleton compartido
     * - Solo creamos la interfaz API específica del feature
     *
     * lazy: Se crea solo cuando se accede por primera vez
     */
    private val popularMoviesApi: PopularMoviesApi by lazy {
        CoreContainer.retrofit.create(PopularMoviesApi::class.java)
    }

    /**
     * Repositorio de películas populares
     *
     * Implementación concreta que:
     * - Usa popularMoviesApi para acceso a datos
     * - Implementa PopularMoviesRepository (interface del domain)
     *
     * private: Solo se expone a través del UseCase
     */
    private val popularMoviesRepository: PopularMoviesRepository by lazy {
        PopularMoviesRepositoryImpl(popularMoviesApi)
    }

    // ═══════════════════════════════════════════════════════════════════
    // DOMAIN LAYER - USE CASES
    // ═══════════════════════════════════════════════════════════════════

    /**
     * UseCase para obtener películas populares
     *
     * PUBLIC: Esta es la dependencia que se inyecta en ViewModels
     *
     * Dependency Inversion:
     * - GetPopularMoviesUseCase depende de PopularMoviesRepository (interface)
     * - NO depende de PopularMoviesRepositoryImpl (implementación)
     * - Esto permite cambiar la implementación sin afectar el UseCase
     */
    val getPopularMoviesUseCase: GetPopularMoviesUseCase by lazy {
        GetPopularMoviesUseCase(popularMoviesRepository)
    }

    // ═══════════════════════════════════════════════════════════════════
    // NOTAS DE DISEÑO
    // ═══════════════════════════════════════════════════════════════════

    /*
     * ¿Por qué este patrón es mejor que el AppContainer monolítico?
     *
     * ANTES (AppContainer monolítico):
     * - Un contenedor con TODAS las dependencias
     * - movieRepository, searchRepository, detailsRepository mezclados
     * - Difícil de mantener y testear
     *
     * DESPUÉS (Feature Containers):
     * - Cada feature tiene su propio contenedor
     * - Separación de responsabilidades
     * - Fácil de testear (mock solo un feature)
     * - Escalable (agregar features no afecta contenedores existentes)
     *
     *
     * ¿Cómo se integra con AppContainer?
     *
     * AppContainer {
     *     val searchContainer = SearchContainer()
     *     val popularMoviesContainer = PopularMoviesContainer()
     *     val detailsContainer = DetailsContainer()  // Futuro
     * }
     *
     * ViewModels acceden así:
     * appContainer.popularMoviesContainer.getPopularMoviesUseCase
     */
}