package com.alexvicente.moviedb.features.genres.data.repository

import com.alexvicente.moviedb.core.data.mapper.GenreMapper.toDomain
import com.alexvicente.moviedb.core.data.util.ErrorMapper
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.data.util.toUserMessage
import com.alexvicente.moviedb.core.domain.model.Genre
import com.alexvicente.moviedb.features.genres.data.remote.api.GenresApi
import com.alexvicente.moviedb.features.genres.domain.repository.GenresRepository
import javax.inject.Inject

class GenresRepositoryImpl @Inject constructor(
    private val api: GenresApi
) : GenresRepository {

    override suspend fun getGenres(): Resource<List<Genre>> = try {
        Resource.Success(api.getGenres().toDomain())
    } catch (e: Exception) {
        val error = ErrorMapper.map(e)
        Resource.Error(error.toUserMessage(), error = error)
    }
}