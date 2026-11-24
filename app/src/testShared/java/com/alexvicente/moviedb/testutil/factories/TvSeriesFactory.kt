package com.alexvicente.moviedb.testutil.factories

import com.alexvicente.moviedb.core.domain.model.Genre
import com.alexvicente.moviedb.features.tv_series.data.remote.dto.TvSeriesDto
import com.alexvicente.moviedb.features.tv_series.domain.model.CastMember
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeasonSummary
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeries
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesDetails
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesStatus

object TvSeriesFactory {

    fun createTvSeriesDto(
        id: Int = 1,
        name: String = "Series 1",
        overview: String? = "Overview",
        posterPath: String? = "/poster.jpg",
        backdropPath: String? = "/backdrop.jpg",
        voteAverage: Double = 8.5,
        voteCount: Int = 100,
        firstAirDate: String? = "2020-01-15",
        popularity: Double = 50.0
    ) = TvSeriesDto(
        id = id, name = name, overview = overview, posterPath = posterPath,
        backdropPath = backdropPath, voteAverage = voteAverage, voteCount = voteCount,
        firstAirDate = firstAirDate, popularity = popularity
    )

    fun createTvSeriesDtoList(size: Int): List<TvSeriesDto> =
        (1..size).map { createTvSeriesDto(id = it, name = "Series $it") }

    fun createTvSeries(
        id: Int = 1,
        name: String = "Series 1",
        posterPath: String? = "/poster.jpg",
        backdropPath: String? = "/backdrop.jpg"
    ) = TvSeries(
        id = id,
        name = name,
        overview = "Overview",
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = 8.5,
        voteCount = 100,
        firstAirDate = "2020-01-15",
        popularity = 50.0
    )

    fun createTvSeriesList(
        size: Int,
        namePrefix: String = "Series",
        posterPath: String? = "/poster.jpg",
        backdropPath: String? = "/backdrop.jpg"
    ): List<TvSeries> =
        (1..size).map {
            createTvSeries(
                id = it,
                name = "$namePrefix $it",
                posterPath = posterPath,
                backdropPath = backdropPath
            )
        }

    fun createTvSeriesDetails(
        id: Int = 1,
        name: String = "Series 1",
        tagline: String = "",
        overview: String = "Overview",
        status: TvSeriesStatus = TvSeriesStatus.ENDED,
        seasons: List<TvSeasonSummary> = emptyList(),
        genres: List<Genre> = emptyList(),
        voteAverage: Double = 8.5,
        voteCount: Int = 100,
        firstAirDate: String = "2020-01-15",
        numberOfSeasons: Int = 1,
        numberOfEpisodes: Int = 10
    ) = TvSeriesDetails(
        id = id, name = name, tagline = tagline, overview = overview,
        posterPath = null, backdropPath = null,
        voteAverage = voteAverage, voteCount = voteCount,
        firstAirDate = firstAirDate, status = status,
        numberOfSeasons = numberOfSeasons, numberOfEpisodes = numberOfEpisodes,
        genres = genres, seasons = seasons
    )

    fun createTvSeasonSummary(
        id: Int = 1,
        seasonNumber: Int = 1,
        name: String = "Season $seasonNumber",
        overview: String = "",
        airDate: String? = "2020-01-15",
        episodeCount: Int = 10,
        voteAverage: Double = 8.0
    ) = TvSeasonSummary(
        id = id, name = name, overview = overview, posterPath = null,
        airDate = airDate, episodeCount = episodeCount,
        seasonNumber = seasonNumber, voteAverage = voteAverage
    )

    // Temporada 0 de TMDB: es la que confundía el centinela de «argumento ausente»
    fun createSpecialsSeasonSummary() = createTvSeasonSummary(
        id = 100, seasonNumber = 0, name = "Especiales",
        airDate = "2005-03-27", episodeCount = 68, voteAverage = 0.0
    )

    fun createGreysAnatomyDetails(
        id: Int = 1,
        name: String = "Anatomía según Grey",
        overview: String = "Un grupo de médicos residentes.",
        tagline: String = "",
        seasons: List<TvSeasonSummary> = listOf(
            createSpecialsSeasonSummary(),
            createTvSeasonSummary(
                id = 1, seasonNumber = 1, name = "Temporada 1",
                airDate = "2005-03-27", episodeCount = 9, voteAverage = 7.9
            )
        )
    ) = createTvSeriesDetails(
        id = id, name = name, tagline = tagline, overview = overview,
        status = TvSeriesStatus.RETURNING_SERIES, seasons = seasons,
        genres = listOf(Genre(id = 18, name = "Drama")),
        voteAverage = 8.2, voteCount = 1000, firstAirDate = "2005-03-27",
        numberOfSeasons = seasons.size,
        numberOfEpisodes = seasons.sumOf { it.episodeCount }
    )


    // ── Reparto ──────────────────────────────────────────────────────────────

    fun createCastMember(
        id: Int = 1,
        name: String = "Actor $id",
        creditId: String = "credit-$id",
        character: String = "Personaje $id",
        profilePath: String? = "/profile$id.jpg"
    ) = CastMember(
        id = id, creditId = creditId, name = name,
        character = character, profilePath = profilePath
    )

    fun createCastMemberList(size: Int): List<CastMember> =
        (1..size).map { createCastMember(id = it) }
}