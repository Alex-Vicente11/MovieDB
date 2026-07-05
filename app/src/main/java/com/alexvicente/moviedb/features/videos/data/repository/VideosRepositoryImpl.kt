package com.alexvicente.moviedb.features.videos.data.repository

import com.alexvicente.moviedb.core.data.local.dao.VideosDao
import com.alexvicente.moviedb.core.data.local.mapper.toDomain
import com.alexvicente.moviedb.core.data.local.mapper.toEntity
import com.alexvicente.moviedb.core.data.mapper.MovieMapper.toDomainVideos
import com.alexvicente.moviedb.core.data.util.ErrorMapper
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.data.util.toUserMessage
import com.alexvicente.moviedb.features.videos.data.remote.api.VideosApi
import com.alexvicente.moviedb.features.videos.domain.model.Video
import com.alexvicente.moviedb.features.videos.domain.repository.VideosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class VideosRepositoryImpl @Inject constructor(
    private val api: VideosApi,
    private val videosDao: VideosDao
) : VideosRepository {

    override fun getMovieVideos(movieId: Int): Flow<Resource<List<Video>>> = flow {

        emit(Resource.Loading())

        val cached = videosDao.getVideosByMovieId(movieId)
        if (cached.isNotEmpty()) {
            emit(Resource.Success(cached.map { it.toDomain() }))
            return@flow
        }

        try {
            val videos = api.getMovieVideos(movieId = movieId).results.toDomainVideos()
            videosDao.insertVideos(videos.toEntity(movieId))
            emit(Resource.Success(videos))
        } catch (e: Exception) {
            val error = ErrorMapper.map(e)
            emit(Resource.Error(error.toUserMessage(), error = error))
        }
    }.flowOn(Dispatchers.IO)
}