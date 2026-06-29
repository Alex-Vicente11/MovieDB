package com.alexvicente.moviedb.features.search.domain.repository

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.domain.model.Movie
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