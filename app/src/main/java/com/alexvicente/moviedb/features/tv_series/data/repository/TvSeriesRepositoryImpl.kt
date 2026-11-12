package com.alexvicente.moviedb.features.tv_series.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.filter
import com.alexvicente.moviedb.features.tv_series.data.paging.TvSeriesPagingSource
import com.alexvicente.moviedb.features.tv_series.data.remote.api.TvSeriesApi
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeries
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesCategory
import com.alexvicente.moviedb.features.tv_series.domain.repository.TvSeriesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TvSeriesRepositoryImpl @Inject constructor(
    private val api: TvSeriesApi
) : TvSeriesRepository {

    override fun getTvSeries(
        category: TvSeriesCategory,
        language: String
    ): Flow<PagingData<TvSeries>> = Pager(
        config = PagingConfig(
            pageSize = TvSeriesPagingSource.PAGE_SIZE,
            enablePlaceholders = false,
            prefetchDistance = 5
        ),
        pagingSourceFactory = { TvSeriesPagingSource(api, category, language) }
    ).flow.map { pagingData ->
        // Un Set nuevo por cada generación de PagingData (refresh o invalidacion):
        // así un refresh no descarta series ya vistas en la generación anterior
        val seenIds = HashSet<Int>()
        pagingData.filter { series -> seenIds.add(series.id)}
    }
}