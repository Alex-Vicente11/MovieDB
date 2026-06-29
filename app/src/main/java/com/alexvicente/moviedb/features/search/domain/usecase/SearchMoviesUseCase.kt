package com.alexvicente.moviedb.features.search.domain.usecase

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.features.search.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * USE CASE: Buscar películas
 *
 * Encapsula la lógica de negocio para buscar películas
 * - Valida el query
 * - Llama al repositorio
 * - Puede agregar lógica adicional (filtros, transformaciones)
 */
class SearchMoviesUseCase @Inject constructor(
    private val repository: SearchRepository
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