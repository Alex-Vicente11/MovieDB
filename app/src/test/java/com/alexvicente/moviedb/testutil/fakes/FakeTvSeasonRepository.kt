package com.alexvicente.moviedb.testutil.fakes

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeasonDetails
import com.alexvicente.moviedb.features.tv_series.domain.repository.TvSeasonRepository
import com.alexvicente.moviedb.testutil.factories.TvSeasonFactory

class FakeTvSeasonRepository : TvSeasonRepository {

    var result: Resource<TvSeasonDetails> =
        Resource.Success(TvSeasonFactory.createTvSeasonDetails())

    /** Registra cada consulta como (seriesId, seasonNumber), en orden. */
    val requested = mutableListOf<Pair<Int, Int>>()

    override suspend fun getTvSeasonDetails(
        seriesId: Int,
        seasonNumber: Int,
        language: String
    ): Resource<TvSeasonDetails> {
        requested += seriesId to seasonNumber
        return result
    }
}