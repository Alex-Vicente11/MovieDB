package com.alexvicente.moviedb.testutil.fakes

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesDetails
import com.alexvicente.moviedb.features.tv_series.domain.repository.TvSeriesDetailsRepository
import com.alexvicente.moviedb.testutil.factories.TvSeriesFactory

class FakeTvSeriesDetailsRepository : TvSeriesDetailsRepository {

    var result: Resource<TvSeriesDetails> =
        Resource.Success(TvSeriesFactory.createTvSeriesDetails())
    val requestedIds = mutableListOf<Int>()

    override suspend fun getTvSeriesDetails(
        seriesId: Int,
        language: String
    ): Resource<TvSeriesDetails> {
        requestedIds += seriesId
        return result
    }
}