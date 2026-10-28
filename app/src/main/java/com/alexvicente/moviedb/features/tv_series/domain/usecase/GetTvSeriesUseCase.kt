package com.alexvicente.moviedb.features.tv_series.domain.usecase

import androidx.paging.PagingData
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeries
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesCategory
import com.alexvicente.moviedb.features.tv_series.domain.repository.TvSeriesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTvSeriesUseCase @Inject constructor(
    private val repository: TvSeriesRepository
) {
    operator fun invoke(
        category: TvSeriesCategory,
        language: String = "es-MX"
    ): Flow<PagingData<TvSeries>> = repository.getTvSeries(category, language)
}