package com.alexvicente.moviedb.features.movie_details.domain.usecase

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.util.Constants
import com.alexvicente.moviedb.features.movie_details.domain.model.MovieDetails
import com.alexvicente.moviedb.features.movie_details.domain.repository.MovieDetailsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetMovieDetailsUseCase @Inject constructor(
    private val repository: MovieDetailsRepository
) {
    operator fun invoke(movieId: Int): Flow<Resource<MovieDetails>> {
        if (movieId <= 0) {
            return flow { emit(Resource.Error(Constants.ERROR_INVALID_ID)) }
        }
        return repository.getMovieDetails(movieId)
    }
}