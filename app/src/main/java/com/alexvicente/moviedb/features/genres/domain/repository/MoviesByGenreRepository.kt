package com.alexvicente.moviedb.features.genres.domain.repository

import androidx.paging.PagingData
import com.alexvicente.moviedb.core.domain.model.Movie
import kotlinx.coroutines.flow.Flow

interface MoviesByGenreRepository {
    fun getMoviesByGenre(
        genreId: Int,
        language: String = "es-MX"
    ): Flow<PagingData<Movie>>
}