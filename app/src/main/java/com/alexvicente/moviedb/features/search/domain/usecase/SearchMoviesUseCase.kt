package com.alexvicente.moviedb.features.search.domain.usecase

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.core.util.Constants
import com.alexvicente.moviedb.features.search.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SearchMoviesUseCase @Inject constructor(
    private val repository: SearchRepository
) {
    operator fun invoke(query: String): Flow<Resource<List<Movie>>> {
        if (query.isBlank() || query.length < Constants.MIN_SEARCH_LENGTH) {
            return flow { emit(Resource.Error(Constants.ERROR_EMPTY_QUERY)) }
        }
        return repository.searchMovies(query.trim())
    }
}