package com.alexvicente.moviedb.features.genres.domain.usecase

import androidx.paging.PagingData
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.features.genres.domain.repository.MoviesByGenreRepository
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