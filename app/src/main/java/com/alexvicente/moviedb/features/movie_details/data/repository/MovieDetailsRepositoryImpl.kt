package com.alexvicente.moviedb.features.movie_details.data.repository

import android.util.Log
import com.alexvicente.moviedb.core.data.local.dao.MovieDetailsDao
import com.alexvicente.moviedb.core.data.local.mapper.toDomain
import com.alexvicente.moviedb.core.data.local.mapper.toEntity
import com.alexvicente.moviedb.core.data.mapper.MovieMapper.toDomain
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.movie_details.data.remote.api.MovieDetailsApi
import com.alexvicente.moviedb.features.movie_details.domain.model.MovieDetails
import com.alexvicente.moviedb.features.movie_details.domain.repository.MovieDetailsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * IMPLEMENTACIÓN del repositorio de detalles de películas
 *
 * CAPA: Data (implementación concreta)
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
 * Beneficios:
 *  Solo hace UNA cosa (detalles de películas)
 *  Usa API especializada (MovieDetailsApi)
 *  Menor superficie de error
 *  Más fácil de testear
 *  Código más limpio y legible
 */

// ÚNICO CAMBIO vs versión anterior:
//   ANTES → class MovieDetailsRepositoryImpl(private val api: MovieDetailsApi)
//   AHORA → class MovieDetailsRepositoryImpl @Inject constructor(...)
//
// @Inject constructor le dice a Hilt: "puedes crear esta clase con este constructor".
// Hilt verá que necesita MovieDetailsApi, la buscará en MovieDetailsModule
// y la inyectará automáticamente.
// Sin @Inject constructor el @Binds en MovieDetailsModule fallaría en compilación.

/**
 * Cache - First
 *
 * Estrategia: Cache -First sin expiración
 *      Si el detalle ya está en Room -> devolver Room, NO llamar a la API.
 *      Solo llama a la API si no hay datos en Room para este movieId.
 *
 * ¿Por qué sin expiración?
 *      Los detalles de una película (título, sinopsis, genéros, presupuesto)
 *      son prácticamente inmutables. No tiene sentido recargar datos que
 *      no van a cambiar. El usuario verá los detalles al instante.
 *
 * CAMBIOS vs versión anterior:
 *  AGREGADO -> MovieDetailsDao inyectado por Hilt
 *  CAMBIANDO -> cache-first antes de llamar a la API
 */
class MovieDetailsRepositoryImpl @Inject constructor(
    private val api: MovieDetailsApi,
    private val movieDetailsDao: MovieDetailsDao
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

        emit(Resource.Loading())
        Log.d(TAG, "Getting movie details for ID: $movieId")

        // 1. PASO 1 - Buscar en Room primero (cache-first))
        val cached = movieDetailsDao.getMovieDetails(movieId)

        if (cached != null) {
            // Tenemos datos en caché -> emitir y NO llamar a la API
            Log.d(TAG, "Cache hit for movie $movieId: ${cached.title}")
            emit(Resource.Success(cached.toDomain()))
            return@flow  // <- salimos del flow, no hay llamada a la API
        }

        // PASO 2 - No hay caché: llamar a la API
        Log.d(TAG, "Cache miss for movie $movieId, fetching from API")

        try {
            //Llamada a la API (retorna MovieDetailsDto)
            val response = api.getMovieDetails(movieId = movieId)
            // Mapear DTO → Domain Model usando MovieMapper de core
            val movieDetails = response.toDomain()

            // PASO 3 - Guardar en Room para futuras visitas
            movieDetailsDao.insertMovieDetails(movieDetails.toEntity())
            Log.d(TAG, "Saved details for ${movieDetails.title} to Room")

            // Emitir éxito
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
            emit(Resource.Error("Sin conexión. Verifica tu internet."))

        } catch (e: Exception) {
            // Otros errores inesperados
            Log.e(TAG, "Exception", e)
            emit(Resource.Error(e.localizedMessage ?: "Error desconocido"))
        }
    }.flowOn(Dispatchers.IO)
}