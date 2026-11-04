package com.alexvicente.moviedb.features.tv_series.domain.repository

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.tv_series.domain.model.CastMember

interface TvSeriesCastRepository {
    suspend fun getTvSeriesCast(seriesId: Int, language: String): Resource<List<CastMember>>
}