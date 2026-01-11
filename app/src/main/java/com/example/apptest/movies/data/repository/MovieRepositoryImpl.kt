package com.example.apptest.movies.data.repository

import android.util.Log
import com.example.apptest.movies.data.remote.api.TMDBApiService
import com.example.apptest.movies.data.remote.mapper.MovieMapper.toDomain
import com.example.apptest.core.domain.model.Movie
import com.example.apptest.movies.domain.model.MovieDetails
import com.example.apptest.movies.domain.repository.MovieRepository
import com.example.apptest.core.data.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.HttpException
import java.io.IOException

/**
 * IMPLEMENTACIÓN DEL REPOSITORIO
 *
 * Implementa la interface MovieRepository del dominio
 * Responsabilidades:
 * - Hacer llamadas a la API
 * - Convertir DTOs a modelos de dominio
 * - Manejar errores de red
 * - Emitir estados (Loading, Success, Error)
 */
class MovieRepositoryImpl(
    private val api: TMDBApiService
) : MovieRepository {

    companion object {
        private const val TAG = "MovieRepositoryImpl"
    }

    /**
     * Buscar películas por query
     */
    override fun searchMovies(query: String): Flow<Resource<List<Movie>>> = flow {
        try {
            // 1. Emitir estado de carga
            emit(Resource.Loading())
            Log.d(TAG, "Searching movies: $query")

            // 2. Llamada a la API (retorna DTO)
            val response = api.searchMovies(query = query)

            // 3. Mapear DTOs → Domain Models
            val movies = response.results.toDomain()

            // 4. Emitir éxito
            Log.d(TAG, "Found ${movies.size} movies")
            emit(Resource.Success(movies))

        } catch (e: HttpException) {
            // Error HTTP (4xx, 5xx)
            val message = when (e.code()) {
                401 -> "Error de autenticación. Verifica el token"
                404 -> "No se encontraron películas"
                else -> "Error del servidor: ${e.code()}"
            }
            Log.e(TAG, "HttpException: ${e.code()}", e)
            emit(Resource.Error(message))

        } catch (e: IOException) {
            // Error de red/conexión
            Log.e(TAG, "IOException", e)
            emit(Resource.Error("Error de conexión. Verifica tu internet."))

        } catch (e: Exception) {
            // Otros errores
            Log.e(TAG, "Exception", e)
            emit(Resource.Error(e.localizedMessage ?: "Error desconocido"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Obtener películas populares
     */
    override fun getPopularMovies(): Flow<Resource<List<Movie>>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Getting popular movies")

            val response = api.getPopularMovies()
            val movies = response.results.toDomain()

            Log.d(TAG, "Found ${movies.size} popular movies")
            emit(Resource.Success(movies))

        } catch (e: HttpException) {
            Log.e(TAG, "HttpException: ${e.code()}", e)
            emit(Resource.Error("Error del servidor: ${e.code()}"))

        } catch (e: IOException) {
            Log.e(TAG, "IOException", e)
            emit(Resource.Error("Error de conexión. Verifica tu internet."))

        } catch (e: Exception) {
            Log.e(TAG, "Exception", e)
            emit(Resource.Error(e.localizedMessage ?: "Error desconocido"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Obtener detalles de una película
     */
    override fun getMovieDetails(movieId: Int): Flow<Resource<MovieDetails>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Getting movie details for ID: $movieId")

            // Llamada a la API (retorna MovieDetailsDto)
            val response = api.getMovieDetails(movieId = movieId)

            // Mapear DTO → MovieDetails (dominio)
            val movieDetails = response.toDomain()

            Log.d(TAG, "Movie found: ${movieDetails.title}")
            emit(Resource.Success(movieDetails))

        } catch (e: HttpException) {
            val message = when (e.code()) {
                404 -> "Película no encontrada (ID: $movieId)"
                401 -> "Error de autenticación"
                else -> "Error del servidor: ${e.code()}"
            }
            Log.e(TAG, "HttpException: ${e.code()}", e)
            emit(Resource.Error(message))

        } catch (e: IOException) {
            Log.e(TAG, "IOException", e)
            emit(Resource.Error("Error de conexión. Verifica tu internet."))

        } catch (e: Exception) {
            Log.e(TAG, "Exception", e)
            emit(Resource.Error(e.localizedMessage ?: "Error desconocido"))
        }
    }.flowOn(Dispatchers.IO)
}