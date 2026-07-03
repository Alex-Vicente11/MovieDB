package com.alexvicente.moviedb.features.videos.domain.repository

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.videos.domain.model.Video
import kotlinx.coroutines.flow.Flow

interface VideosRepository {

    fun getMovieVideos(movieId: Int): Flow<Resource<List<Video>>>
}