package com.alexvicente.moviedb.features.popular_movies.domain.repository

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.domain.model.Movie
import kotlinx.coroutines.flow.Flow

interface PopularMoviesRepository {

    fun getPopularMovies(): Flow<Resource<List<Movie>>>
}