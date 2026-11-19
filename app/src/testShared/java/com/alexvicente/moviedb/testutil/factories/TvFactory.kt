package com.alexvicente.moviedb.testutil.factories

import com.alexvicente.moviedb.core.domain.model.Genre
import com.alexvicente.moviedb.features.tv_series.domain.model.CastMember
import com.alexvicente.moviedb.features.tv_series.domain.model.TvEpisode
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeasonDetails
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeasonSummary
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeries
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesDetails
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesStatus

object TvFactory {

    fun createSeasonSummary(
        id: Int = 1,
        name: String = "Temporada 1",
        overview: String = "",
        airDate: String? = "2005-03-27",
        episodeCount: Int = 9,
        seasonNumber: Int = 1,
        voteAverage: Double = 7.9
    ) = TvSeasonSummary(id, name, overview, null, airDate, episodeCount, seasonNumber, voteAverage)

    // La temporada 0 existe en TMDB: es la que confundía el centinela de «argumento ausente»
    fun createSpecialsSeason() = createSeasonSummary(
        id = 100, name = "Especiales", episodeCount = 68, seasonNumber = 0, voteAverage = 0.0
    )

    fun createSeriesDetails(
        id: Int = 1,
        name: String = "Anatomía según Grey",
        tagline: String = "",
        overview: String = "Un grupo de médicos residentes.",
        status: TvSeriesStatus = TvSeriesStatus.RETURNING_SERIES,
        seasons: List<TvSeasonSummary> = listOf(createSpecialsSeason(), createSeasonSummary())
    ) = TvSeriesDetails(
        id = id, name = name, tagline = tagline, overview = overview,
        posterPath = null, backdropPath = null, voteAverage = 8.2, voteCount = 1000,
        firstAirDate = "2005-03-27", status = status,
        numberOfSeasons = seasons.size, numberOfEpisodes = seasons.sumOf { it.episodeCount },
        genres = listOf(Genre(id = 18, name = "Drama")),
        seasons = seasons
    )

    fun createCastMember(
        id: Int = 1,
        creditId: String = "c$id",
        name: String = "Ellen Pompeo",
        character: String = "Meredith Grey"
    ) = CastMember(id, creditId, name, character, null)

    fun createSeries(id: Int = 1, name: String = "Serie $id") = TvSeries(
        id = id, name = name, overview = "", posterPath = null, backdropPath = null,
        voteAverage = 8.0, voteCount = 100, firstAirDate = "2021-03-14", popularity = 10.0
    )

    // ids únicos desde 1: la lista usa key = { it.id } y repetirlos lanzaría "Key was already used"
    fun createSeriesList(count: Int) = List(count) { createSeries(id = it + 1) }

    fun createEpisode(
        id: Int = 1,
        episodeNumber: Int = 1,
        name: String = "Episodio $episodeNumber",
        overview: String = "",
        airDate: String? = "2005-03-27",
        runtimeMinutes: Int? = 44,
        voteAverage: Double = 7.8,
        voteCount: Int = 120
    ) = TvEpisode(
        id, episodeNumber, name, overview, airDate, runtimeMinutes, null, voteAverage, voteCount
    )

    fun createSeasonDetails(
        id: Int = 1,
        seasonNumber: Int = 1,
        name: String = "Temporada 1",
        overview: String = "",
        episodes: List<TvEpisode> = listOf(
            createEpisode(id = 1, episodeNumber = 1),
            createEpisode(id = 2, episodeNumber = 2)
        )
    ) = TvSeasonDetails(id, seasonNumber, name, overview, null, "2005-03-27", 7.9, episodes)
}