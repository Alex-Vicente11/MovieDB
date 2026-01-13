package com.example.apptest.features.movie_details.data.repository

import android.util.Log
import com.example.apptest.core.data.mapper.MovieMapper.toDomain
import com.example.apptest.core.data.util.Resource
import com.example.apptest.features.movie_details.data.remote.api.MovieDetailsApi
import com.example.apptest.features.movie_details.domain.model.MovieDetails
import com.example.apptest.features.movie_details.domain.repository.MovieDetailsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.HttpException
import java.io.IOException

/**
 * IMPLEMENTACIÓN del repositorio de detalles de películas
 *
 * CAPA: Data (implementación concreta)
 * UBICACIÓN: features/movie_details/data/repository/
 *
 * Responsabilidades:
 * - Hacer llamadas a la API (MovieDetailsApi)
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
 * ❌ ANTES: MovieRepositoryImpl (~120 líneas, 3 responsabilidades)
 * ✅ AHORA: MovieDetailsRepositoryImpl (~50 líneas, 1 responsabilidad)
 *
 * Beneficios:
 * ✅ Solo hace UNA cosa (detalles de películas)
 * ✅ Usa API especializada (MovieDetailsApi)
 * ✅ Menor superficie de error
 * ✅ Más fácil de testear
 * ✅ Código más limpio y legible
 */
class MovieDetailsRepositoryImpl(
    private val api: MovieDetailsApi
) : MovieDetailsRepository {

    companion object {
        private const val TAG = "MovieDetailsRepo"
    }

    /**
     * Obtener detalles completos de una película
     *
     * Flow de ejecución:
     * 1. Emitir Loading
     * 2. Llamar a la API con el movieId
     * 3. Mapear DTO → Domain Model
     * 4. Emitir Success con datos
     * 5. Si hay error, emitir Error con mensaje apropiado
     *
     * Manejo de errores:
     * - HttpException: Errores del servidor (4xx, 5xx)
     * - IOException: Errores de red/conexión
     * - Exception: Otros errores inesperados
     */
    override fun getMovieDetails(movieId: Int): Flow<Resource<MovieDetails>> = flow {
        try {
            // 1. Emitir estado de carga
            emit(Resource.Loading())
            Log.d(TAG, "Getting movie details for ID: $movieId")

            // 2. Llamada a la API (retorna MovieDetailsDto)
            val response = api.getMovieDetails(movieId = movieId)

            // 3. Mapear DTO → Domain Model usando MovieMapper de core
            val movieDetails = response.toDomain()

            // 4. Emitir éxito
            Log.d(TAG, "Movie found: ${movieDetails.title}")
            emit(Resource.Success(movieDetails))

        } catch (e: HttpException) {
            // Error HTTP (4xx, 5xx)
            val message = when (e.code()) {
                404 -> "Película no encontrada (ID: $movieId)"
                401 -> "Error de autenticación. Verifica el token"
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