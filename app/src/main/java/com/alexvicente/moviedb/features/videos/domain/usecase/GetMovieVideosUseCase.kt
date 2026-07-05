package com.alexvicente.moviedb.features.videos.domain.usecase

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.util.Constants
import com.alexvicente.moviedb.features.videos.domain.model.Video
import com.alexvicente.moviedb.features.videos.domain.repository.VideosRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class GetMovieVideosUseCase @Inject constructor(
    private val repository: VideosRepository
) {
    operator fun invoke(
        movieId: Int,
        filterOfficial: Boolean = false
    ): Flow<Resource<List<Video>>> {
        if (movieId <= 0) {
            return flow { emit(Resource.Error(Constants.ERROR_INVALID_ID)) }
        }

        return repository.getMovieVideos(movieId).map { resource ->
            when (resource) {
                is Resource.Success -> {
                    var videos = resource.data
                    if (filterOfficial) videos = videos.filter { it.official }
                    videos = videos.sortedWith(
                        compareByDescending<Video> { it.official }
                            .thenByDescending { it.isTrailer() }
                            .thenBy { it.name }
                    )
                    Resource.Success(videos)
                }
                is Resource.Error   -> resource
                is Resource.Loading -> resource
            }
        }
    }
}