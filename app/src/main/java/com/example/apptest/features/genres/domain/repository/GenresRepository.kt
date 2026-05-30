package com.example.apptest.features.genres.domain.repository

import com.example.apptest.core.data.util.Resource
import com.example.apptest.core.domain.model.Genre

interface GenresRepository {
    suspend fun getGenres(): Resource<List<Genre>>
}