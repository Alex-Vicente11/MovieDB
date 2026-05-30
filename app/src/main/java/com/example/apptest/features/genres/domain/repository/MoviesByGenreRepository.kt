package com.example.apptest.features.genres.domain.repository

import androidx.paging.PagingData
import com.example.apptest.core.domain.model.Movie
import kotlinx.coroutines.flow.Flow

interface MoviesByGenreRepository {
    fun getMoviesByGenre(
        genreId: Int,
        language: String = "es-MX"
    ): Flow<PagingData<Movie>>
}