package com.alexvicente.moviedb.features.tv_series.domain.usecase

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesDetails
import com.alexvicente.moviedb.features.tv_series.domain.repository.TvSeriesDetailsRepository
import javax.inject.Inject

class GetTvSeriesDetailsUseCase @Inject constructor(
    private val repository: TvSeriesDetailsRepository
){
    suspend operator fun invoke(
        seriesId: Int,
        language: String = "es-MX"
    ): Resource<TvSeriesDetails> = repository.getTvSeriesDetails(seriesId, language)
}