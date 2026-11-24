package com.alexvicente.moviedb.testutil.factories

import com.alexvicente.moviedb.features.tv_series.domain.model.TvEpisode
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeasonDetails

object TvSeasonFactory {

    fun createTvEpisode(
        id: Int = 1,
        episodeNumber: Int = id,
        name: String = "Episodio $episodeNumber",
        overview: String = "",
        airDate: String? = "2020-01-15",
        runtimeMinutes: Int? = 44,
        voteAverage: Double = 0.0,
        voteCount: Int = 0
    ) = TvEpisode(
        id = id,
        episodeNumber = episodeNumber,
        name = name,
        overview = overview,
        airDate = airDate,
        runtimeMinutes = runtimeMinutes,
        stillPath = null,
        voteAverage = voteAverage,
        voteCount = voteCount
    )

    fun createTvSeasonDetails(
        seasonNumber: Int = 1,
        name: String = "Temporada $seasonNumber",
        episodeCount: Int = 3,
        id: Int = seasonNumber + 100,
        overview: String = "",
        episodes: List<TvEpisode> = (1..episodeCount).map { createTvEpisode(id = it) }
    ) = TvSeasonDetails(
        id = id,
        seasonNumber = seasonNumber,
        name = name,
        overview = overview,
        posterPath = null,
        airDate = "2020-01-15",
        voteAverage = 0.0,
        episodes = episodes
    )
}