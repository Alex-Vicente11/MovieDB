package com.alexvicente.moviedb.features.tv_series.data.remote.dto

import com.alexvicente.moviedb.features.tv_series.data.mapper.TvSeriesMapper.toDomain
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesStatus
import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import org.junit.Test

class TvSeriesDetailsContractTest {

    private val gson = GsonBuilder().create()

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource(name)) { "Falta el fixture $name" }
            .readText()

    @Test
    fun returningSeries_greysAnatomy_parsesAndMapsToDomain() {
        val dto = gson.fromJson(fixture("tv_series_details_1416.json"), TvSeriesDetailsDto::class.java)

        val details = dto.toDomain()

        assertThat(details.id).isEqualTo(1416)
        assertThat(details.name).isEqualTo("Anatomía según Grey")
        assertThat(details.tagline).isEqualTo("La vida que salves puede ser la tuya.")
        assertThat(details.status).isEqualTo(TvSeriesStatus.RETURNING_SERIES)
        assertThat(details.numberOfSeasons).isEqualTo(23)
        assertThat(details.numberOfEpisodes).isEqualTo(466)
        assertThat(details.genres.map { it.name }).containsExactly("Drama")
        assertThat(details.seasons).hasSize(24)
        // «Especiales» llega primero, con season_number 0
        assertThat(details.seasons.first().seasonNumber).isEqualTo(0)
        assertThat(details.seasons.first().episodeCount).isEqualTo(68)
        assertThat(details.seasons.last().seasonNumber).isEqualTo(23)
        assertThat(details.seasons.last().overview).isEmpty()
    }

    @Test
    fun endedMiniseries_chernobyl_parsesAndMapsToDomain() {
        val dto = gson.fromJson(fixture("tv_series_details_87108.json"), TvSeriesDetailsDto::class.java)

        val details = dto.toDomain()

        assertThat(details.name).isEqualTo("Chernobyl")
        assertThat(details.status).isEqualTo(TvSeriesStatus.ENDED)
        assertThat(details.numberOfSeasons).isEqualTo(1)
        assertThat(details.numberOfEpisodes).isEqualTo(5)
        assertThat(details.seasons.single().name).isEqualTo("Miniseries")
    }

    @Test
    fun sparseResponse_mapsNullsAndMissingFieldsToSafeDefaults() {
        val json = """
            {"id": 1, "name": "  Serie  ", "overview": null, "poster_path": null,
             "backdrop_path": null, "vote_average": 0, "vote_count": 0,
             "first_air_date": "", "status": "Something New",
             "number_of_seasons": 1, "number_of_episodes": 0,
             "seasons": [{"id": 5, "name": "Season 1", "overview": null, "poster_path": null,
                          "air_date": null, "episode_count": 0, "season_number": 1,
                          "vote_average": 0}]}
        """.trimIndent()

        val details = gson.fromJson(json, TvSeriesDetailsDto::class.java).toDomain()

        assertThat(details.name).isEqualTo("Serie")
        assertThat(details.tagline).isEmpty()       // campo ausente
        assertThat(details.overview).isEmpty()      // null
        assertThat(details.posterPath).isNull()
        assertThat(details.firstAirDate).isNull()   // "" se vuelve null
        assertThat(details.status).isEqualTo(TvSeriesStatus.UNKNOWN)
        assertThat(details.genres).isEmpty()        // campo ausente
        assertThat(details.seasons.single().airDate).isNull()
    }
}