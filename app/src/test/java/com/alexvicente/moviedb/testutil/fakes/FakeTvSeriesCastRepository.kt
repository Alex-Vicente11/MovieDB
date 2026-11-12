package com.alexvicente.moviedb.testutil.fakes

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.tv_series.domain.model.CastMember
import com.alexvicente.moviedb.features.tv_series.domain.repository.TvSeriesCastRepository
import com.alexvicente.moviedb.testutil.factories.TvSeriesFactory

class FakeTvSeriesCastRepository : TvSeriesCastRepository {

    var result: Resource<List<CastMember>> =
        Resource.Success(TvSeriesFactory.createCastMemberList(3))

    /** Registra los ids consultados, en orden. */
    val requestedIds = mutableListOf<Int>()

    override suspend fun getTvSeriesCast(
        seriesId: Int,
        language: String
    ): Resource<List<CastMember>> {
        requestedIds += seriesId
        return result
    }
}

