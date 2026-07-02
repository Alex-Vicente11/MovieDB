package com.alexvicente.moviedb.features.search.domain.repository

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.domain.model.Movie
import kotlinx.coroutines.flow.Flow

interface SearchRepository {

    fun searchMovies(query: String): Flow<Resource<List<Movie>>>
}