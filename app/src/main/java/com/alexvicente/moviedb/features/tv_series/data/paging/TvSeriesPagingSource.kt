// data/paging/TvSeriesPagingSource.kt
package com.alexvicente.moviedb.features.tv_series.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.alexvicente.moviedb.features.tv_series.data.mapper.TvSeriesMapper.toDomain
import com.alexvicente.moviedb.features.tv_series.data.remote.api.TvSeriesApi
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeries
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesCategory
import retrofit2.HttpException
import java.io.IOException

class TvSeriesPagingSource(
    private val api: TvSeriesApi,
    private val category: TvSeriesCategory,
    private val language: String
) : PagingSource<Int, TvSeries>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TvSeries> {
        val page = params.key ?: STARTING_PAGE

        return try {
            val response = when (category) {
                TvSeriesCategory.POPULAR -> api.getPopularTvSeries(page, language)
                TvSeriesCategory.TOP_RATED -> api.getTopRatedTvSeries(page, language)
            }

            LoadResult.Page(
                data = response.results.map { it.toDomain() },
                prevKey = if (page == STARTING_PAGE) null else page - 1,
                nextKey = if (page >= response.totalPages) null else page + 1
            )
        } catch (e: HttpException) {
            LoadResult.Error(e)
        } catch (e: IOException) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, TvSeries>): Int? {
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