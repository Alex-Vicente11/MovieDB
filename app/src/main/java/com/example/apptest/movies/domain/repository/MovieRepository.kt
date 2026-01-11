package com.example.apptest.movies.domain.repository


import com.example.apptest.core.domain.model.Movie
import com.example.apptest.movies.domain.model.MovieDetails
import com.example.apptest.core.data.util.Resource
import kotlinx.coroutines.flow.Flow

/**
 * CONTRATO DEL REPOSITORIO
 *
 * Interface que define QUÉ puede hacer el repositorio
 * La implementación estará en data/repository/MovieRepositoryImpl.kt
 *
 * Ventajas:
 * - Desacopla el dominio de la implementación
 * - Permite cambiar la implementación sin afectar UseCases
 * - Facilita testing con mocks
 */
interface MovieRepository {

    /**
     * Buscar películas por query
     * @param query Término de búsqueda
     * @return Flow que emite Resource (Loading, Success, Error)
     */
    fun searchMovies(query: String): Flow<Resource<List<Movie>>>

    /**
     * Obtener películas populares
     * @return Flow que emite Resource (Loading, Success, Error)
     */
    fun getPopularMovies(): Flow<Resource<List<Movie>>>

    /**
     * Obtener detalles completos de una película
     * @param movieId ID de la película
     * @return Flow que emite Resource (Loading, Success, Error)
     */
    fun getMovieDetails(movieId: Int): Flow<Resource<MovieDetails>>
}