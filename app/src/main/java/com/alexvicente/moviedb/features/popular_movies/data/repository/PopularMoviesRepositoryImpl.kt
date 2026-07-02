package com.alexvicente.moviedb.features.popular_movies.data.repository

import com.alexvicente.moviedb.core.data.local.dao.MovieDao
import com.alexvicente.moviedb.core.data.local.mapper.toDomain
import com.alexvicente.moviedb.core.data.local.mapper.toEntity
import com.alexvicente.moviedb.core.data.mapper.MovieMapper
import com.alexvicente.moviedb.core.data.util.ErrorMapper
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.data.util.toUserMessage
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.features.popular_movies.data.remote.api.PopularMoviesApi
import com.alexvicente.moviedb.features.popular_movies.domain.repository.PopularMoviesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

private const val CACHE_TIMEOUT_MS = 30 * 60 * 1000L

class PopularMoviesRepositoryImpl @Inject constructor(
    private val api: PopularMoviesApi,
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
            val response = api.getPopularMovies()
            val movies = response.results

            movieDao.deletePopularMovies()

            val domainMovies = with(MovieMapper) { movies.map { it.toDomain() } }
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