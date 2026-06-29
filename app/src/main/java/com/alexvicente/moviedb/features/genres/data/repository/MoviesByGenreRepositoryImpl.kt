package com.alexvicente.moviedb.features.genres.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.features.genres.data.paging.MoviesByGenrePagingSource
import com.alexvicente.moviedb.features.genres.data.remote.api.MoviesByGenreApi
import com.alexvicente.moviedb.features.genres.domain.repository.MoviesByGenreRepository
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
                // lambda que crea una nueva instancia de PagingSource cada vez que se necesita refrescar
                // No llama una sola vez, la recrea cuando hay refresh o cuando cambia el género.
                MoviesByGenrePagingSource(api, genreId, language)
            }
        ).flow
    }
}