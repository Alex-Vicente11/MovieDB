package com.alexvicente.moviedb.features.tv_series.domain.repository

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeasonDetails

interface TvSeasonRepository {
    suspend fun getTvSeasonDetails(
        seriesId: Int,
        seasonNumber: Int,
        language: String
    ): Resource<TvSeasonDetails>
}