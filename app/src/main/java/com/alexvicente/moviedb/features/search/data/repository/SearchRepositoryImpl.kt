package com.alexvicente.moviedb.features.search.data.repository

import android.util.Log
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.features.search.data.remote.api.SearchApi
import com.alexvicente.moviedb.features.search.domain.repository.SearchRepository
import com.alexvicente.moviedb.core.data.mapper.MovieMapper.toDomain
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.HttpException
import java.io.IOException

/**
 * IMPLEMENTACIÓN del repositorio de búsqueda
 */
class SearchRepositoryImpl @Inject constructor(
    private val api: SearchApi
) : SearchRepository {

    companion object {
        private const val TAG = "SearchRepository"
    }

    override fun searchMovies(query: String): Flow<Resource<List<Movie>>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Searching movies: $query")

            // 1. Llamada API
            val response = api.searchMovies(query = query)

            // 2. Mapear DTO → Domain
            val movies = response.results.toDomain()

            Log.d(TAG, "Found ${movies.size} movies")
            emit(Resource.Success(movies))

        } catch (e: HttpException) {
            val message = when (e.code()) {
                401 -> "Error de autenticación"
                404 -> "No se encontraron películas"
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