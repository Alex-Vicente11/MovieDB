package com.example.apptest.features.videos.data.repository

import android.util.Log
import com.example.apptest.core.data.local.dao.VideosDao
import com.example.apptest.core.data.local.mapper.toDomain
import com.example.apptest.core.data.local.mapper.toEntity
import com.example.apptest.core.data.mapper.MovieMapper.toDomain
import com.example.apptest.core.data.mapper.MovieMapper.toDomainVideos
import com.example.apptest.core.data.util.Resource
import com.example.apptest.features.videos.data.remote.api.VideosApi
import com.example.apptest.features.videos.domain.model.Video
import com.example.apptest.features.videos.domain.repository.VideosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okio.IOException
import retrofit2.HttpException
import javax.inject.Inject

/**
 * Responsabilidades:
 * - Hacer llamadas a la Api (VideosApi)
 * - Convertir DTOs a modelos de dominio (usando MovieMapper)
 * - Manejar errores de red/HTTP
 * - Emitir estados (Loading, Success, Error) mediante Flow
 *
 * Patrones:
 * - Repository Pattern: Abstrae la fuente de datos
 * - Mapper Pattern: Separa DTOs de modelos de domino
 * - Flow Pattern: Emisión reactiva de estados
 *
 * Cache - first
 * Misma estrategia sin expiración
 * Los trailers de una película no cambian después del estreno.
 */

class VideosRepositoryImpl @Inject constructor(
    private val api: VideosApi,
    private val videosDao: VideosDao
) : VideosRepository {
    companion object {
        private const val TAG = "VideosRepo"
    }

    /**
     * Obtener videos de una pelicula
     *
     * Flow de ejecución:
     * 1. Emitir Loading
     * 2. Llamar a la API con el movieId
     * 3. Mapear DTOs -> Domain Models
     * 4. Emitir Success con datos
     * 5. Si hay error, emitir Error con mensaje apropiado
     *
     * Manejo de errores:
     * - HttpExceptions: Errores del servidor
     * - IOException: Errores de red/conexion
     * - Exception: Otros erroes inesperados
     */
    override fun getMovieVideos(movieId: Int): Flow<Resource<List<Video>>> = flow {

        emit(Resource.Loading())
        Log.d(TAG, "Getting videos for movie ID: $movieId")

        // Paso 1 - Buscar en Room
        val cached = videosDao.getVideosByMovieId(movieId)

        if (cached.isNotEmpty()) {
            Log.d(TAG, "Cache hit: ${cached.size} videos for movie $movieId")
            emit(Resource.Success(cached.map { it.toDomain() }))
            return@flow
        }

        // PASO 2 - Cache miss: llamar a la API
        Log.d(TAG, "Cache miss for videos of movie $movieId")

        try {
            // LLamada a la API (retorna VideoResponseDto)
            val response = api.getMovieVideos(movieId = movieId)

            // Mapear DTOs -> Domain Models usando MovieMapper de core
            val videos = response.results.toDomainVideos()

            // PASO 3 - Guardar en Room
            videosDao.insertVideos(videos.toEntity(movieId))
            Log.d(TAG, "Saved ${videos.size} videos for movie $movieId to Room")

            // Emitir exito
            Log.d(TAG, "Found ${videos.size} videos")
            emit(Resource.Success(videos))
        } catch (e: HttpException) {
            // Error HTTP
            val message = when (e.code()) {
                404 -> "No se encontraron videos para esta pelicula"
                401 -> "Error de autenticación. Verifica el token"
                429 -> "Demasiadas peticiones. Intenta más tarde"
                500, 502, 503 -> "Error del servidor. Intenta más tarde"
                else -> "Error del servidor: ${e.code()}"
            }
            Log.e(TAG, "HttpException: ${e.code()}", e)
            emit(Resource.Error(message))
        } catch (e: IOException) {
            // Error de red/conexion
            Log.e(TAG, "IOException", e)
            emit(Resource.Error("Sin conexión. Verifica tu internet."))
        } catch (e: Exception) {
            // Otros errores inesperados
            Log.e(TAG, "Exception", e)
            emit(Resource.Error(e.localizedMessage ?: "Error desconocido"))
        }
    }.flowOn(Dispatchers.IO)
}