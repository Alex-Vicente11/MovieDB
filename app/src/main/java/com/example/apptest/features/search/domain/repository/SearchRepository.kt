package com.example.apptest.features.search.domain.repository

import com.example.apptest.core.data.util.Resource
import com.example.apptest.core.domain.model.Movie
import kotlinx.coroutines.flow.Flow

/**
 * CONTRATO del repositorio de búsqueda
 */
interface SearchRepository {

    /**
     * Buscar películas por texto
     */
    fun searchMovies(query: String): Flow<Resource<List<Movie>>>
}