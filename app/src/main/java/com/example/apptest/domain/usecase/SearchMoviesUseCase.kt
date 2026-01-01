package com.example.apptest.domain.usecase

import com.example.apptest.domain.model.Movie
import com.example.apptest.domain.repository.MovieRepository
import com.example.apptest.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * USE CASE: Buscar películas
 *
 * Encapsula la lógica de negocio para buscar películas
 * - Valida el query
 * - Llama al repositorio
 * - Puede agregar lógica adicional (filtros, transformaciones)
 */
class SearchMoviesUseCase(
    private val repository: MovieRepository
) {
    /**
     * Ejecuta la búsqueda
     *
     * operator fun invoke() permite llamar al UseCase como función:
     * searchMoviesUseCase(query) en lugar de searchMoviesUseCase.execute(query)
     */
    operator fun invoke(query: String): Flow<Resource<List<Movie>>> {
        // Validación: Query no puede estar vacío
        if (query.isBlank()) {
            return flow {
                emit(Resource.Error("El término de búsqueda no puede estar vacío"))
            }
        }

        // Validación: Query debe tener al menos 2 caracteres
        if (query.length < 2) {
            return flow {
                emit(Resource.Error("Ingresa al menos 2 caracteres"))
            }
        }

        // Delegar al repositorio
        return repository.searchMovies(query.trim())
    }
}