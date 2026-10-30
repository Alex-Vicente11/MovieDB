package com.alexvicente.moviedb.features.tv_series.data.repository

import com.alexvicente.moviedb.core.data.util.ErrorMapper
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.data.util.toUserMessage
import com.alexvicente.moviedb.features.tv_series.data.mapper.TvSeriesMapper.toDomain
import com.alexvicente.moviedb.features.tv_series.data.remote.api.TvSeriesApi
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesDetails
import com.alexvicente.moviedb.features.tv_series.domain.repository.TvSeriesDetailsRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class TvSeriesDetailsRepositoryImpl @Inject constructor(
    private val api: TvSeriesApi
) : TvSeriesDetailsRepository {

    override suspend fun getTvSeriesDetails(
        seriesId: Int,
        language: String
    ): Resource<TvSeriesDetails> = try {
        Resource.Success(api.getTvSeriesDetails(seriesId, language).toDomain())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val error = ErrorMapper.map(e)
        Resource.Error(error.toUserMessage(), error = error)
    }
}