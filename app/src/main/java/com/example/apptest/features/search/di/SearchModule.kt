package com.example.apptest.features.search.di

import com.example.apptest.core.di.CoreContainer
import com.example.apptest.features.search.data.remote.api.SearchApi
import com.example.apptest.features.search.data.repository.SearchRepositoryImpl
import com.example.apptest.features.search.domain.repository.SearchRepository
import com.example.apptest.features.search.domain.usecase.SearchMoviesUseCase

/**
 * SEARCH FEATURE CONTAINER
 *
 * Dependency Injection manual para el feature de Search
 */
class SearchContainer {

    // ═══════════════════════════════════════════════════════
    // DATA LAYER
    // ═══════════════════════════════════════════════════════

    /**
     * API Service (singleton)
     * Usa Retrofit compartido de CoreContainer
     */
    private val searchApi: SearchApi by lazy {
        CoreContainer.retrofit.create(SearchApi::class.java)
    }

    /**
     * Repository (singleton)
     */
    val searchRepository: SearchRepository by lazy {
        SearchRepositoryImpl(searchApi)
    }

    // ═══════════════════════════════════════════════════════
    // DOMAIN LAYER - USE CASES
    // ═══════════════════════════════════════════════════════

    /**
     * Use Case: Buscar películas
     */
    val searchMoviesUseCase: SearchMoviesUseCase by lazy {
        SearchMoviesUseCase(searchRepository)
    }
}