package com.alexvicente.moviedb.features.tv_series.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.alexvicente.moviedb.features.tv_series.data.paging.TvSeriesPagingSource
import com.alexvicente.moviedb.features.tv_series.data.remote.api.TvSeriesApi
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeries
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesCategory
import com.alexvicente.moviedb.features.tv_series.domain.repository.TvSeriesRepository
import kotlinx.coroutines.flow.Flow
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
    ).flow
}