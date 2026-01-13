package com.example.apptest.features.popular_movies.data.repository

import android.util.Log
import com.example.apptest.core.data.mapper.MovieMapper.toDomain
import com.example.apptest.core.data.util.Resource
import com.example.apptest.core.domain.model.Movie
import com.example.apptest.features.popular_movies.data.remote.api.PopularMoviesApi
import com.example.apptest.features.popular_movies.domain.repository.PopularMoviesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.HttpException
import java.io.IOException

/**
 * IMPLEMENTACIÓN del repositorio de películas populares
 *
 * CAPA: Data (implementación concreta)
 * UBICACIÓN: features/popular_movies/data/repository/
 *
 * Responsabilidades:
 * - Hacer llamadas a la API (PopularMoviesApi)
 * - Convertir DTOs a modelos de dominio (usando MovieMapper)
 * - Manejar errores de red/HTTP
 * - Emitir estados (Loading, Success, Error) mediante Flow
 *
 * Patrón aplicado:
 * - Repository Pattern: Abstrae la fuente de datos
 * - Mapper Pattern: Separa DTOs de modelos de dominio
 * - Flow Pattern: Emisión reactiva de estados
 *
 * Comparación con MovieRepositoryImpl (legacy):
 * ✅ Solo hace UNA cosa (popular movies)
 * ✅ Usa API especializada (PopularMoviesApi)
 * ✅ Menor superficie de error
 * ✅ Más fácil de testear
 */
class PopularMoviesRepositoryImpl(
    private val api: PopularMoviesApi
) : PopularMoviesRepository {

    companion object {
        private const val TAG = "PopularMoviesRepo"
    }

    /**
     * Obtener películas populares
     *
     * Flow de ejecución:
     * 1. Emitir Loading
     * 2. Llamar a la API
     * 3. Mapear DTO → Domain Model
     * 4. Emitir Success con datos
     * 5. Si hay error, emitir Error
     */
    override fun getPopularMovies(): Flow<Resource<List<Movie>>> = flow {
        try {
            // 1. Emitir estado de carga
            emit(Resource.Loading())
            Log.d(TAG, "Getting popular movies")

            // 2. Llamada a la API (retorna MovieResponseDto)
            val response = api.getPopularMovies()

            // 3. Mapear DTOs → Domain Models usando MovieMapper de core
            val movies = response.results.toDomain()

            // 4. Emitir éxito
            Log.d(TAG, "Found ${movies.size} popular movies")
            emit(Resource.Success(movies))

        } catch (e: HttpException) {
            // Error HTTP (4xx, 5xx)
            val message = when (e.code()) {
                401 -> "Error de autenticación. Verifica el token"
                404 -> "Películas populares no disponibles"
                429 -> "Demasiadas peticiones. Intenta más tarde"
                500, 502, 503 -> "Error del servidor. Intenta más tarde"
                else -> "Error del servidor: ${e.code()}"
            }
            Log.e(TAG, "HttpException: ${e.code()}", e)
            emit(Resource.Error(message))

        } catch (e: IOException) {
            // Error de red/conexión
            Log.e(TAG, "IOException", e)
            emit(Resource.Error("Error de conexión. Verifica tu internet."))

        } catch (e: Exception) {
            // Otros errores inesperados
            Log.e(TAG, "Exception", e)
            emit(Resource.Error(e.localizedMessage ?: "Error desconocido"))
        }
    }.flowOn(Dispatchers.IO)  // Ejecutar en IO thread
}