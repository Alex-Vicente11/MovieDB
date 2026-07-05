package com.alexvicente.moviedb.features.movie_details.data.repository

import com.alexvicente.moviedb.core.data.local.dao.MovieDetailsDao
import com.alexvicente.moviedb.core.data.local.mapper.toDomain
import com.alexvicente.moviedb.core.data.local.mapper.toEntity
import com.alexvicente.moviedb.core.data.mapper.MovieMapper.toDomain
import com.alexvicente.moviedb.core.data.util.ErrorMapper
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.data.util.toUserMessage
import com.alexvicente.moviedb.features.movie_details.data.remote.api.MovieDetailsApi
import com.alexvicente.moviedb.features.movie_details.domain.model.MovieDetails
import com.alexvicente.moviedb.features.movie_details.domain.repository.MovieDetailsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class MovieDetailsRepositoryImpl @Inject constructor(
    private val api: MovieDetailsApi,
    private val movieDetailsDao: MovieDetailsDao
) : MovieDetailsRepository {

    override fun getMovieDetails(movieId: Int): Flow<Resource<MovieDetails>> = flow {

        emit(Resource.Loading())

        val cached = movieDetailsDao.getMovieDetails(movieId)
        if (cached != null) {
            emit(Resource.Success(cached.toDomain()))
            return@flow
        }

        try {
            val movieDetails = api.getMovieDetails(movieId = movieId).toDomain()
            movieDetailsDao.insertMovieDetails(movieDetails.toEntity())
            emit(Resource.Success(movieDetails))
        } catch (e: Exception) {
            val error = ErrorMapper.map(e)
            emit(Resource.Error(error.toUserMessage(), error = error))
        }
    }.flowOn(Dispatchers.IO)
}