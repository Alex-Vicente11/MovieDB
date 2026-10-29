package com.alexvicente.moviedb.testutil.fakes

import androidx.paging.PagingData
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeries
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesCategory
import com.alexvicente.moviedb.features.tv_series.domain.repository.TvSeriesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeTvSeriesRepository : TvSeriesRepository {

    var seriesByCategory: Map<TvSeriesCategory, List<TvSeries>> = emptyMap()
    val requestedCategories = mutableListOf<TvSeriesCategory>()

    override fun getTvSeries(
        category: TvSeriesCategory,
        language: String
    ): Flow<PagingData<TvSeries>> {
        requestedCategories += category
        return flowOf(PagingData.from(seriesByCategory[category].orEmpty()))
    }
}