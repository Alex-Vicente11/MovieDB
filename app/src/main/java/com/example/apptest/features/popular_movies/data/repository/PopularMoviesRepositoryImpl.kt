package com.example.apptest.features.popular_movies.data.repository

import android.util.Log
import com.example.apptest.core.data.local.dao.MovieDao
import com.example.apptest.core.data.local.entity.MovieEntity
import com.example.apptest.core.data.local.mapper.toDomain
import com.example.apptest.core.data.local.mapper.toEntity
import com.example.apptest.core.data.mapper.MovieMapper.toDomain
import com.example.apptest.core.data.util.Resource
import com.example.apptest.core.domain.model.Movie
import com.example.apptest.features.popular_movies.data.remote.api.PopularMoviesApi
import com.example.apptest.features.popular_movies.domain.repository.PopularMoviesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

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

/**
 *  Offline-First con Room
 *
 *  CAMBIOS vs versión anterior (solo Retrofit):
 *      AGREGADO -> MovieDao (inyectado por Hilt)
 *      CAMBIADO -> getPopularMovies() implementa offline-first
 *      IGUAL    -> firma del metodo, manejo de errores
 *
 *  ESTRATEGIA OFFLINE-FIRST:
 *      1. Emitir Loading
 *      2. Leer de Room y emitir inmediatamente si hay datos
 *      3. Verificar si el caché expiró (TTL = 30 minutos)
 *      4. Si expiró -> llamar API, llamar API, guardar en Room, emitir datos frescos
 *      5. Si no expiró -> los datos de Room ya fueron emitidos en paso 2
 *
 *  TTL (Time To Live) = 30 minutos:
 *      Las películas populares cambian poco. Con 30 minutos en caché,
 *      el usuario ve datos inmediatemante sin esperar la red,
 *      y los datos se actualizan con frecuencia razonable.
 */

private const val CACHE_TIMEOUT_MS = 30 * 60 * 1000L // 30 minutos en ms

class PopularMoviesRepositoryImpl @Inject constructor(
    private val api: PopularMoviesApi,
    private val movieDao: MovieDao      // inyectado por Hilt vía DatabaseModule
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

        // 1. Emitir estado de carga
        emit(Resource.Loading())
        Log.d(TAG, "Getting popular movies")

        // 2. Emitir caché de Room inmediatamente si existe
        // first() obtiene la primera emisión del Flow de Room (snapshot actual)
        // La UI muestra datos al instante sin esperar la red
        val cachedMovies = movieDao.getPopularMovies()
            .let { flow ->
                var result = emptyList<MovieEntity>()
                // Recolectamos solo el primer valor (snapshot)
                flow.collect { result = it; return@collect }
                result
            }

        if (cachedMovies.isNotEmpty()) {
            Log.d(TAG, "Emiting ${cachedMovies.size} movies from cache")
            emit(Resource.Success(cachedMovies.toDomain()))
        }

        // PASO 3 - Verificar si el caché expiró
        val lastCacheTime = movieDao.getLastCacheTime()
        val isCacheValid = lastCacheTime != null &&
                (System.currentTimeMillis() - lastCacheTime < CACHE_TIMEOUT_MS)

        if (isCacheValid && cachedMovies.isNotEmpty()) {
            Log.d(TAG, "Cache is valid, skipping API call")
            return@flow // datos frescos, no necesitamos la API
        }

        // PASO 4 - Caché expirado o vació: llamar a la API
        try {
            Log.d(TAG, "Cache expired or empty, fetching from API")
            val response = api.getPopularMovies()
            val movies = response.results

            // PASO 5 - Guardar en Room (reemplaza el caché anterior)
            movieDao.deletePopularMovies()
            movieDao.insertMovies(movies.map { dto ->
                // Reutilizamos el mapper de DTO -> Domain -> Entity
                // dto.toDomain() viene del mapper de red existente
                dto.toDomain().toEntity(isPopular = true)
            })

            Log.d(TAG, "Saved ${movies.size} movies to Room")

            // PASO 6 - Emitir datos frescos de Room
            // (Room también emitirá automáticamente vía Flow en el Dao,
            // pero aquí emitimos explicitamente para el flujo de Resource)
            emit(Resource.Success(movies.map { it.toDomain() }))

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
            // Si hay error de red pero tenemos caché, no sobreescribimos el Success
            // Solo emitimos error si no tenemos datos en caché
            if (cachedMovies.isEmpty()) emit(Resource.Error(message))

        } catch (e: IOException) {
            // Error de red/conexión
            Log.e(TAG, "IOException - no network", e)
            if (cachedMovies.isEmpty()) {
                emit(Resource.Error("Sin conexión. Verifica tu internet."))
            }
            // Si hay caché, el usuario ya lo vio - no mostramos error

        } catch (e: Exception) {
            // Otros errores inesperados
            Log.e(TAG, "Exception", e)
            if (cachedMovies.isEmpty()) emit(Resource.Error(e.localizedMessage ?: "Error desconocido"))
        }
    }
}