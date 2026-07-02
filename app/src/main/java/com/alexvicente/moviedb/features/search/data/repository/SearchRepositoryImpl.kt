package com.alexvicente.moviedb.features.search.data.repository

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.features.search.data.remote.api.SearchApi
import com.alexvicente.moviedb.features.search.domain.repository.SearchRepository
import com.alexvicente.moviedb.core.data.mapper.MovieMapper.toDomain
import com.alexvicente.moviedb.core.data.util.ErrorMapper
import com.alexvicente.moviedb.core.data.util.toUserMessage
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class SearchRepositoryImpl @Inject constructor(
    private val api: SearchApi
) : SearchRepository {

    override fun searchMovies(query: String): Flow<Resource<List<Movie>>> = flow {
        try {
            emit(Resource.Loading())
            val movies = api.searchMovies(query = query).results.toDomain()
            emit(Resource.Success(movies))
        } catch (e: Exception) {
            val error = ErrorMapper.map(e)
            emit(Resource.Error(error.toUserMessage(), error = error))
        }
    }.flowOn(Dispatchers.IO)
}