package com.example.apptest.features.popular_movies.domain.usecase

import com.example.apptest.core.data.util.Resource
import com.example.apptest.core.domain.model.Movie
import com.example.apptest.features.popular_movies.domain.repository.PopularMoviesRepository
import com.example.apptest.movies.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow

/**
 * USE CASE: Obtener películas populares
 *
 * CAPA: Domain (lógica de negocio)
 * UBICACIÓN: features/popular_movies/domain/usecase/
 *
 * Responsabilidad:
 * - Encapsular la lógica de negocio para obtener películas populares
 * - Delegar al repositorio (no implementa la lógica de red)
 * - Validar precondiciones si las hubiera
 *
 * Cambios vs versión legacy:
 *  Package actualizado: movies.domain.usecase → popular_movies.domain.usecase
 *  Dependencia actualizada: MovieRepository → PopularMoviesRepository
 *  Ahora depende de una abstracción específica del feature
 *
 * Dependency Inversion Principle:
 * - Depende de PopularMoviesRepository (interface)
 * - NO depende de PopularMoviesRepositoryImpl (implementación)
 * - Esto permite testing fácil con mocks
 *
 * Single Responsibility Principle:
 * - Solo hace UNA cosa: coordinar la obtención de películas populares
 * - No maneja red, no mapea datos, no maneja UI
 */
class GetPopularMoviesUseCase(
    private val repository: PopularMoviesRepository
) {
    /**
     * Ejecuta la obtención de películas populares
     *
     * operator fun invoke() permite llamar al UseCase como función:
     * getPopularMoviesUseCase() en lugar de getPopularMoviesUseCase.execute()
     *
     * @return Flow que emite Resource con estados Loading/Success/Error
     *
     * Nota: Este UseCase no tiene validaciones porque no recibe parámetros
     * Si en el futuro agregamos paginación, aquí validaríamos el número de página
     */
    operator fun invoke(): Flow<Resource<List<Movie>>> {
        return repository.getPopularMovies()
    }

    // Ejemplo de validación futura si agregamos paginación:
    // operator fun invoke(page: Int): Flow<Resource<List<Movie>>> {
    //     if (page < 1) {
    //         return flow { emit(Resource.Error("Página inválida")) }
    //     }
    //     return repository.getPopularMovies(page)
    // }
}