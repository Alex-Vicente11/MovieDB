package com.alexvicente.moviedb.features.tv_series.data.remote.dto

import com.alexvicente.moviedb.features.tv_series.data.mapper.TvSeriesMapper.toDomain
import com.alexvicente.moviedb.testutil.Fixtures
import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import org.junit.Test

/**
 * Contrato de tv/{id}/season/{n} con respuestas reales de TMDB (src/test/resources),
 * recortadas: crew y guest_stars se dejaron en un elemento por episodio.
 * Cada fixture cubre un caso distinto de nulos y vacíos.
 */
class TvSeasonContractTest {

    private val gson = GsonBuilder().create()

    private fun parse(fixtureName: String): TvSeasonDetailsDto =
        gson.fromJson(Fixtures.read(fixtureName), TvSeasonDetailsDto::class.java)

    @Test
    fun finishedSeason_greysAnatomySeason1_parsesAndMapsToDomain() {
        val season = parse("tv_series/tv_season_details_1416_1.json").toDomain()

        assertThat(season.name).isEqualTo("Temporada 1")
        assertThat(season.seasonNumber).isEqualTo(1)
        assertThat(season.airDate).isEqualTo("2005-03-27")
        assertThat(season.overview).isNotEmpty()
        assertThat(season.episodes).hasSize(9)
        assertThat(season.episodes.first().episodeNumber).isEqualTo(1)
        assertThat(season.episodes.first().name).isEqualTo("Una noche de un duro día")
        assertThat(season.episodes.last().episodeNumber).isEqualTo(9)
        // el id es único por episodio: sirve como key de la lista
        assertThat(season.episodes.map { it.id }).containsNoDuplicates()
    }

    @Test
    fun specials_greysAnatomySeason0_hasEmptyOverviewsAndMissingRuntimes() {
        val season = parse("tv_series/tv_season_details_1416_0.json").toDomain()

        assertThat(season.name).isEqualTo("Especiales")
        assertThat(season.seasonNumber).isEqualTo(0)
        assertThat(season.episodes).hasSize(68)
        assertThat(season.episodes.all { it.overview.isEmpty() }).isTrue()
        assertThat(season.episodes.count { it.runtimeMinutes == null }).isEqualTo(5)
        // sin votos, vote_average llega como 0.0: hay que mirar vote_count para saber si hay calificación
        assertThat(season.episodes.count { it.voteCount == 0 }).isEqualTo(63)
        assertThat(season.episodes.map { it.id }).containsNoDuplicates()
    }

    @Test
    fun airingSeason_greysAnatomySeason23_hasUpcomingEpisodesWithoutRuntimeOrStill() {
        val season = parse("tv_series/tv_season_details_1416_23.json").toDomain()

        assertThat(season.episodes).hasSize(6)
        assertThat(season.episodes.count { it.runtimeMinutes == null }).isEqualTo(3)
        assertThat(season.episodes.count { it.stillPath == null }).isEqualTo(2)
        assertThat(season.episodes[3].airDate).isEqualTo("2026-11-05")
        assertThat(season.episodes.last().stillPath).isNull()
        assertThat(season.episodes.last().voteCount).isEqualTo(0)
    }

    @Test
    fun miniseries_chernobyl_parsesAndMapsToDomain() {
        val season = parse("tv_series/tv_season_details_87108_1.json").toDomain()

        assertThat(season.name).isEqualTo("Miniseries")
        assertThat(season.episodes).hasSize(5)
        assertThat(season.episodes.first().name).isEqualTo("1:23:45")
        assertThat(season.episodes.all { it.runtimeMinutes != null }).isTrue()
    }

    @Test
    fun sparseResponse_mapsNullsAndBlanksToSafeDefaults() {
        val json = """
            {"id": 9, "name": "  Temporada X  ", "overview": null, "poster_path": null,
             "air_date": "", "season_number": 2, "vote_average": 0,
             "episodes": [{"id": 1, "episode_number": 1, "name": " Piloto ", "overview": null,
                           "air_date": "", "runtime": 0, "still_path": "",
                           "vote_average": 0, "vote_count": 0}]}
        """.trimIndent()

        val season = gson.fromJson(json, TvSeasonDetailsDto::class.java).toDomain()

        assertThat(season.name).isEqualTo("Temporada X")
        assertThat(season.overview).isEmpty()
        assertThat(season.posterPath).isNull()
        assertThat(season.airDate).isNull()
        with(season.episodes.single()) {
            assertThat(name).isEqualTo("Piloto")
            assertThat(overview).isEmpty()
            assertThat(airDate).isNull()
            assertThat(runtimeMinutes).isNull()   // 0 minutos no es una duración
            assertThat(stillPath).isNull()        // "" se vuelve null
        }
    }

    @Test
    fun missingEpisodesField_mapsToEmptyList() {
        val json = """{"id": 1, "name": "S", "season_number": 1, "vote_average": 0}"""

        val season = gson.fromJson(json, TvSeasonDetailsDto::class.java).toDomain()

        assertThat(season.episodes).isEmpty()
    }
}
