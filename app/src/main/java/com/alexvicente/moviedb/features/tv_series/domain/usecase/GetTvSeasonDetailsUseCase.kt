package com.alexvicente.moviedb.features.tv_series.domain.usecase

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeasonDetails
import com.alexvicente.moviedb.features.tv_series.domain.repository.TvSeasonRepository
import javax.inject.Inject

class GetTvSeasonDetailsUseCase @Inject constructor(
    private val repository: TvSeasonRepository
) {
    suspend operator fun invoke(
        seriesId: Int,
        seasonNumber: Int,
        language: String = "es-MX"
    ): Resource<TvSeasonDetails> = repository.getTvSeasonDetails(seriesId, seasonNumber, language)
}