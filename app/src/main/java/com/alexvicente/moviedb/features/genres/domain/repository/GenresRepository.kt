package com.alexvicente.moviedb.features.genres.domain.repository

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.domain.model.Genre

interface GenresRepository {
    suspend fun getGenres(): Resource<List<Genre>>
}