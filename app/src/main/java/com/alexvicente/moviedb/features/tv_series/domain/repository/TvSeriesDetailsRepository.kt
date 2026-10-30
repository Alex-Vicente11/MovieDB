package com.alexvicente.moviedb.features.tv_series.domain.repository

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesDetails

interface TvSeriesDetailsRepository {
    suspend fun getTvSeriesDetails(seriesId: Int, language: String): Resource<TvSeriesDetails>
}