package com.alexvicente.moviedb.features.tv_series.domain.repository

import androidx.paging.PagingData
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeries
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesCategory
import kotlinx.coroutines.flow.Flow

interface TvSeriesRepository {
    fun getTvSeries(category: TvSeriesCategory, language: String): Flow<PagingData<TvSeries>>
}