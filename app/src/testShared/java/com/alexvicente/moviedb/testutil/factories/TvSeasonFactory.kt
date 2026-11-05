package com.alexvicente.moviedb.testutil.factories

import com.alexvicente.moviedb.features.tv_series.domain.model.TvEpisode
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeasonDetails

object TvSeasonFactory {

    fun createTvEpisode(id: Int = 1, episodeNumber: Int = id) = TvEpisode(
        id = id,
        episodeNumber = episodeNumber,
        name = "Episodio $episodeNumber",
        overview = "",
        airDate = "2020-01-15",
        runtimeMinutes = 44,
        stillPath = null,
        voteAverage = 0.0,
        voteCount = 0
    )

    fun createTvSeasonDetails(
        seasonNumber: Int = 1,
        name: String = "Temporada $seasonNumber",
        episodeCount: Int = 3
    ) = TvSeasonDetails(
        id = seasonNumber + 100,
        seasonNumber = seasonNumber,
        name = name,
        overview = "",
        posterPath = null,
        airDate = "2020-01-15",
        voteAverage = 0.0,
        episodes = (1..episodeCount).map { createTvEpisode(id = it) }
    )
}