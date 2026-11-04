package com.alexvicente.moviedb.features.tv_series.data.mapper

import android.R.attr.name
import com.alexvicente.moviedb.core.data.mapper.GenreMapper.toDomain
import com.alexvicente.moviedb.features.tv_series.data.remote.dto.CastMemberDto
import com.alexvicente.moviedb.features.tv_series.data.remote.dto.TvCreditsDto
import com.alexvicente.moviedb.features.tv_series.data.remote.dto.TvSeasonSummaryDto
import com.alexvicente.moviedb.features.tv_series.data.remote.dto.TvSeriesDetailsDto
import com.alexvicente.moviedb.features.tv_series.data.remote.dto.TvSeriesDto
import com.alexvicente.moviedb.features.tv_series.domain.model.CastMember
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeasonSummary
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeries
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesDetails
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesStatus

object TvSeriesMapper {

    fun TvSeriesDto.toDomain(): TvSeries = TvSeries(
        id = id,
        name = name.trim(),
        overview = overview?.trim().orEmpty(),
        posterPath = posterPath?.trim(),
        backdropPath = backdropPath?.trim(),
        voteAverage = voteAverage,
        voteCount = voteCount,
        firstAirDate = firstAirDate?.trim()?.takeIf { it.isNotEmpty() },
        popularity = popularity
    )

    fun TvSeriesDetailsDto.toDomain(): TvSeriesDetails = TvSeriesDetails(
        id = id,
        name = name.trim(),
        tagline = tagline?.trim().orEmpty(),
        overview = overview?.trim().orEmpty(),
        posterPath = posterPath?.trim(),
        backdropPath = backdropPath?.trim(),
        voteAverage = voteAverage,
        voteCount = voteCount,
        firstAirDate = firstAirDate?.trim()?.takeIf { it.isNotEmpty() },
        status = status.toTvSeriesStatus(),
        numberOfSeasons = numberOfSeasons,
        numberOfEpisodes = numberOfEpisodes,
        genres = genres.orEmpty().map { it.toDomain() },
        seasons = seasons.orEmpty().map { it.toDomain() }
    )

    fun TvSeasonSummaryDto.toDomain(): TvSeasonSummary = TvSeasonSummary(
        id = id,
        name = name.trim(),
        overview = overview?.trim().orEmpty(),
        posterPath = posterPath?.trim(),
        airDate = airDate?.trim()?.takeIf { it.isNotEmpty() },
        episodeCount = episodeCount,
        seasonNumber = seasonNumber,
        voteAverage = voteAverage
    )

    private fun String?.toTvSeriesStatus(): TvSeriesStatus = when (this) {
        "Returning Series" -> TvSeriesStatus.RETURNING_SERIES
        "In Production" -> TvSeriesStatus.IN_PRODUCTION
        "Planned" -> TvSeriesStatus.PLANNED
        "Pilot" -> TvSeriesStatus.PILOT
        "Ended" -> TvSeriesStatus.ENDED
        "Canceled" -> TvSeriesStatus.CANCELED
        else -> TvSeriesStatus.UNKNOWN
    }

    fun CastMemberDto.toDomain(): CastMember = CastMember(
        id = id,
        creditId = creditId,
        name = name.trim(),
        character = character?.trim().orEmpty(),
        profilePath = profilePath?.trim()?.takeIf { it.isNotEmpty() }
    )

    fun TvCreditsDto.toCast(): List<CastMember> = cast.orEmpty().map { it.toDomain() }
}