package com.alexvicente.moviedb.features.tv_series.data.repository

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.data.util.safeApiCall
import com.alexvicente.moviedb.features.tv_series.data.mapper.TvSeriesMapper.toDomain
import com.alexvicente.moviedb.features.tv_series.data.remote.api.TvSeriesApi
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeasonDetails
import com.alexvicente.moviedb.features.tv_series.domain.repository.TvSeasonRepository
import javax.inject.Inject

class TvSeasonRepositoryImpl @Inject constructor(
    private val api: TvSeriesApi
) : TvSeasonRepository {

    override suspend fun getTvSeasonDetails(
        seriesId: Int,
        seasonNumber: Int,
        language: String
    ): Resource<TvSeasonDetails> = safeApiCall {
        api.getTvSeasonDetails(seriesId, seasonNumber, language).toDomain()
    }
}