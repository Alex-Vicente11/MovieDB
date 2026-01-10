package com.example.apptest.movies.domain.usecase

import com.example.apptest.movies.domain.model.MovieDetails
import com.example.apptest.movies.domain.repository.MovieRepository
import com.example.apptest.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * USE CASE: Obtener detalles de una película
 *
 * Encapsula la lógica de negocio para obtener detalles completos
 * Valida el ID antes de llamar al repositorio
 */
class GetMovieDetailsUseCase(
    private val repository: MovieRepository
) {
    /**
     * Ejecuta la obtención de detalles
     *
     * @param movieId ID de la película
     */
    operator fun invoke(movieId: Int): Flow<Resource<MovieDetails>> {
        // Validación: ID debe ser positivo
        if (movieId <= 0) {
            return flow {
                emit(Resource.Error("ID de película inválido"))
            }
        }

        return repository.getMovieDetails(movieId)
    }
}