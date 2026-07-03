package com.alexvicente.moviedb.features.genres.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.alexvicente.moviedb.core.data.mapper.MovieMapper.toDomain
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.features.genres.data.remote.api.MoviesByGenreApi
import java.io.IOException
import retrofit2.HttpException

class MoviesByGenrePagingSource(
    private val api: MoviesByGenreApi,
    private val genreId: Int,
    private val language: String
): PagingSource<Int, Movie>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        val page = params.key ?: STARTING_PAGE

        return try {
            val response = api.getMoviesByGenre(
                genreId = genreId,
                page = page,
                language = language
            )

            val movies = response.results.map { it.toDomain() }

            LoadResult.Page(
                data = movies,
                prevKey = if (page == STARTING_PAGE) null else page - 1,
                nextKey = if (page >= response.totalPages) null else page + 1
            )
        } catch (e: HttpException) {
            LoadResult.Error(e)
        } catch (e: IOException) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    companion object {
        const val STARTING_PAGE = 1
        const val PAGE_SIZE = 20
    }
}