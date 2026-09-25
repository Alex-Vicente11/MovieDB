package com.alexvicente.moviedb.features.popular_movies.data.repository

import com.alexvicente.moviedb.core.data.local.dao.MovieDao
import com.alexvicente.moviedb.core.data.local.mapper.toDomain
import com.alexvicente.moviedb.core.data.local.mapper.toEntity
import com.alexvicente.moviedb.core.data.util.ErrorMapper
import com.alexvicente.moviedb.core.data.util.GraphQLDataException
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.data.util.toUserMessage
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.features.popular_movies.domain.repository.PopularMoviesRepository
import com.alexvicente.moviedb.graphql.PopularMoviesQuery
import com.apollographql.apollo.ApolloClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import android.util.Log
import com.alexvicente.moviedb.core.data.mapper.ApolloMovieMapper
import javax.inject.Inject

private const val CACHE_TIMEOUT_MS = 30 * 60 * 1000L
private const val TAG = "PopularMoviesRepo"

class PopularMoviesRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient,
    private val movieDao: MovieDao
) : PopularMoviesRepository {

    override fun getPopularMovies(): Flow<Resource<List<Movie>>> = flow {

        emit(Resource.Loading())

        val cachedMovies = movieDao.getPopularMovies().first()
        if (cachedMovies.isNotEmpty()) {
            emit(Resource.Success(cachedMovies.map { it.toDomain() }))
        }

        val lastCacheTime = movieDao.getLastCacheTime()
        val isCacheValid = lastCacheTime != null &&
                (System.currentTimeMillis() - lastCacheTime < CACHE_TIMEOUT_MS)

        if (isCacheValid && cachedMovies.isNotEmpty()) return@flow

        try {
            val response = apolloClient.query(PopularMoviesQuery(page = 1)).execute()

            // Nivel 1 (red/transporte) ya viene como excepción real si execute() falla;
            // Apollo también expone response.exception para algunos casos no fatales.
            response.exception?.let { throw it }

            val hasData = response.data?.popularMovies != null

            when {
                // Nivel 2: errores GraphQL sin datos utilizables
                response.hasErrors() && !hasData -> {
                    val message = response.errors?.firstOrNull()?.message
                        ?: "Error GraphQL desconocido"
                    throw GraphQLDataException(message)
                }
                // Nivel 3: éxito parcial — algunos campos fallaron, pero hay datos usables
                response.hasErrors() && hasData -> {
                    Log.w(TAG, "Respuesta parcial de GraphQL: ${response.errors}")
                }
            }

            val movies = response.data?.popularMovies.orEmpty()

            movieDao.deletePopularMovies()

            val domainMovies = with(ApolloMovieMapper) { movies.map { it.toDomain() } }
            movieDao.insertMovies(domainMovies.map { it.toEntity(isPopular = true) })
            emit(Resource.Success(domainMovies))

        } catch (e: Exception) {
            val error = ErrorMapper.map(e)
            if (cachedMovies.isEmpty()) {
                emit(Resource.Error(error.toUserMessage(), error = error))
            }
        }
    }.flowOn(Dispatchers.IO)
}