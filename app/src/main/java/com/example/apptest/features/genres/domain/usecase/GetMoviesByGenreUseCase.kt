package com.example.apptest.features.genres.domain.usecase

import androidx.paging.PagingData
import com.example.apptest.core.domain.model.Movie
import com.example.apptest.features.genres.domain.repository.MoviesByGenreRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMoviesByGenreUseCase @Inject constructor(
    private val repository: MoviesByGenreRepository
) {
    operator fun invoke(
        genreId: Int,
        language: String = "es-MX"
    ): Flow<PagingData<Movie>> {
        return repository.getMoviesByGenre(genreId, language)
    }
}