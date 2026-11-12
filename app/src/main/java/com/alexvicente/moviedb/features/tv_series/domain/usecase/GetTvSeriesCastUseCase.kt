package com.alexvicente.moviedb.features.tv_series.domain.usecase

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.tv_series.domain.model.CastMember
import com.alexvicente.moviedb.features.tv_series.domain.repository.TvSeriesCastRepository
import javax.inject.Inject

class GetTvSeriesCastUseCase @Inject constructor(
    private val repository: TvSeriesCastRepository
) {

    suspend operator fun invoke(
        seriesId: Int,
        language: String = "es-MX"
    ): Resource<List<CastMember>> = repository.getTvSeriesCast(seriesId, language)
}