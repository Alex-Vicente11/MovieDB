package com.alexvicente.moviedb.features.tv_series.data.repository

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.data.util.safeApiCall
import com.alexvicente.moviedb.features.tv_series.data.mapper.TvSeriesMapper.toCast
import com.alexvicente.moviedb.features.tv_series.data.remote.api.TvSeriesApi
import com.alexvicente.moviedb.features.tv_series.domain.model.CastMember
import com.alexvicente.moviedb.features.tv_series.domain.repository.TvSeriesCastRepository
import javax.inject.Inject

class TvSeriesCastRepositoryImpl @Inject constructor(
    private val api: TvSeriesApi
) : TvSeriesCastRepository {

    override suspend fun getTvSeriesCast(
        seriesId: Int,
        language: String
    ): Resource<List<CastMember>> = safeApiCall {
        api.getTvSeriesCredits(seriesId, language).toCast()
    }
}