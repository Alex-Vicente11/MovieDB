package com.alexvicente.moviedb.features.popular_movies.domain.usecase

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.features.popular_movies.domain.repository.PopularMoviesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPopularMoviesUseCase @Inject constructor(
    private val repository: PopularMoviesRepository
) {

    operator fun invoke(): Flow<Resource<List<Movie>>> {
        return repository.getPopularMovies()
    }
}