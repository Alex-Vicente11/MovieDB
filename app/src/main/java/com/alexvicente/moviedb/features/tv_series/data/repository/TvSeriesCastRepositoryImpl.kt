package com.alexvicente.moviedb.features.tv_series.data.repository

import com.alexvicente.moviedb.core.data.util.ErrorMapper
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.data.util.toUserMessage
import com.alexvicente.moviedb.features.tv_series.data.mapper.TvSeriesMapper.toCast
import com.alexvicente.moviedb.features.tv_series.data.remote.api.TvSeriesApi
import com.alexvicente.moviedb.features.tv_series.domain.model.CastMember
import com.alexvicente.moviedb.features.tv_series.domain.repository.TvSeriesCastRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class TvSeriesCastRepositoryImpl @Inject constructor(
    private val api: TvSeriesApi
) : TvSeriesCastRepository {

    override suspend fun getTvSeriesCast(
        seriesId: Int,
        language: String
    ): Resource<List<CastMember>> = try {
        Resource.Success(api.getTvSeriesCredits(seriesId, language).toCast())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val error = ErrorMapper.map(e)
        Resource.Error(error.toUserMessage(), error = error)
    }
}