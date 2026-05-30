package com.example.apptest.features.genres.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.apptest.core.domain.model.Movie
import com.example.apptest.features.genres.data.paging.MoviesByGenrePagingSource
import com.example.apptest.features.genres.data.remote.api.MoviesByGenreApi
import com.example.apptest.features.genres.domain.repository.MoviesByGenreRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MoviesByGenreRepositoryImpl @Inject constructor(
    private val api: MoviesByGenreApi
): MoviesByGenreRepository {

    override fun getMoviesByGenre(genreId: Int, language: String): Flow<PagingData<Movie>> {
        return Pager(
            config = PagingConfig(
                pageSize = MoviesByGenrePagingSource.PAGE_SIZE,
                enablePlaceholders = false,
                // Cuántos items cargar antes de llegar al final
                prefetchDistance = 5
            ),
            pagingSourceFactory = {
                MoviesByGenrePagingSource(api, genreId, language)
            }
        ).flow
    }
}