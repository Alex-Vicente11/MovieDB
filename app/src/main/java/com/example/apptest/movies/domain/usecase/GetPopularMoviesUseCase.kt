package com.example.apptest.movies.domain.usecase

import com.example.apptest.movies.domain.model.Movie
import com.example.apptest.movies.domain.repository.MovieRepository
import com.example.apptest.util.Resource
import kotlinx.coroutines.flow.Flow

/**
 * USE CASE: Obtener películas populares
 *
 * Encapsula la lógica de negocio para obtener películas populares
 * No requiere validaciones (no tiene parámetros)
 */
class GetPopularMoviesUseCase(
    private val repository: MovieRepository
) {
    /**
     * Ejecuta la obtención de películas populares
     */
    operator fun invoke(): Flow<Resource<List<Movie>>> {
        return repository.getPopularMovies()
    }
}